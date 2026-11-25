package org.nmcpye.datarun.web.v2.idempotency;

import org.nmcpye.datarun.web.v2.caller.ApiCaller;
import org.nmcpye.datarun.web.v2.concurrency.EntityTag;
import org.nmcpye.datarun.web.v2.concurrency.IfMatch;
import org.nmcpye.datarun.web.v2.config.V2Properties;
import org.nmcpye.datarun.web.v2.problem.ApiProblemException;
import org.nmcpye.datarun.web.v2.problem.ProblemCode;
import org.nmcpye.datarun.web.v2.problem.ProblemError;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.security.MessageDigest;
import java.util.function.Function;

/**
 * Runs v2 commands so that a retry after a lost response is answered with the first outcome, never applied twice.
 * <p>
 * One transaction per command, READ COMMITTED, in this order:
 * <ol>
 *   <li>bound every lock wait ({@code datarun.v2.commands.lock-timeout});</li>
 *   <li>claim the caller's {@code Idempotency-Key} ({@link IdempotencyStore#claim}). A duplicate waits here, on
 *       the key, never on the resource. A key a committed command already holds is answered from its outcome when
 *       the fingerprint is the same (a replay, marked {@code Idempotent-Replayed: true}, even if its
 *       {@code If-Match} is stale by now) and 422 {@code idempotency-key-reused} when it is not;</li>
 *   <li>run the {@link Command}: it locks the resource's row, checks {@code If-Match} through its context, applies
 *       the change and returns the outcome;</li>
 *   <li>record the outcome on the key, then commit.</li>
 * </ol>
 * A refusal or failure anywhere rolls back the command and its claim together, so only successful outcomes are
 * remembered and a retry of a refused request simply runs again.
 * <p>
 * The contract is enforced, not only documented: a change whose command never called
 * {@link CommandContext#checkVersion} fails (500, rolled back), and work that would leave the transaction
 * (REQUIRES_NEW, NOT_SUPPORTED) fails before it starts.
 * <p>
 * The response comes from the {@link OutcomeRenderer} for both the first request and every replay, with the
 * outcome's status and the resource's {@code ETag}.
 */
@Component
public class IdempotentCommands {

    public static final String REPLAYED_HEADER = "Idempotent-Replayed";

    private final IdempotencyStore store;
    private final TransactionTemplate transactions;
    private final V2Properties properties;

    public IdempotentCommands(IdempotencyStore store, PlatformTransactionManager transactionManager,
                              V2Properties properties) {
        this.store = store;
        this.properties = properties;
        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        this.transactions.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    /** Runs a change of a versioned resource: the command checks {@code If-Match} through its context. */
    public <B> ResponseEntity<B> execute(CommandRequest request, Command command, OutcomeRenderer<B> renderer) {
        return run(request, request.ifMatch(), command, renderer, null);
    }

    /**
     * Runs a create: the same claim and replay, with no {@code If-Match} (its context refuses
     * {@code checkVersion}). The answer, first or replayed, also carries {@code Location}.
     *
     * @param location the created resource's URL, from the outcome
     */
    public <B> ResponseEntity<B> create(CreateRequest request, Command command, OutcomeRenderer<B> renderer,
                                        Function<CommandOutcome, URI> location) {
        return run(request, null, command, renderer, location);
    }

    private <B> ResponseEntity<B> run(IdempotentRequest request, IfMatch ifMatch, Command command,
                                      OutcomeRenderer<B> renderer, Function<CommandOutcome, URI> location) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("A v2 command runs in its own transaction; call it outside one");
        }
        ApiCaller caller = request.caller();
        byte[] fingerprint = request.fingerprint();
        Result result = transactions.execute(status -> {
            store.limitLockWaits(properties.getCommands().getLockTimeout());
            IdempotencyStore.Claim claim = store.claim(caller.userId(), request.idempotencyKey(), fingerprint,
                properties.getIdempotency().getRetention());
            if (claim instanceof IdempotencyStore.Completed completed) {
                if (!MessageDigest.isEqual(completed.requestHash(), fingerprint)) {
                    throw ApiProblemException.of(ProblemCode.IDEMPOTENCY_KEY_REUSED)
                        .detail("This Idempotency-Key was used for a different request; choose a new key for a "
                            + "new change.")
                        .error(ProblemError.atHeader(IdempotencyKey.HEADER, "reused", null));
                }
                if (completed.outcome() == null) {
                    throw new IllegalStateException("A committed idempotency key has no outcome");
                }
                return new Result(completed.outcome(), true);
            }
            Context context = new Context(caller, ifMatch);
            TransactionSynchronizationManager.registerSynchronization(STAY_IN_THIS_TRANSACTION);
            CommandOutcome outcome = command.run(context);
            if (outcome == null) {
                throw new IllegalStateException("A command returns its outcome");
            }
            if (ifMatch != null && !context.checked) {
                throw new IllegalStateException("A command on a versioned resource checks If-Match through its "
                    + "context (checkVersion) after locking its row, before changing it");
            }
            store.recordOutcome(caller.userId(), request.idempotencyKey(), outcome);
            return new Result(outcome, false);
        });
        ResponseEntity.BodyBuilder response = ResponseEntity.status(result.outcome().responseStatus())
            .eTag(EntityTag.ofVersion(result.outcome().resourceVersion()).toString());
        if (location != null) {
            response.location(location.apply(result.outcome()));
        }
        if (result.replayed()) {
            response.header(REPLAYED_HEADER, "true");
        }
        return response.body(renderer.render(result.outcome()));
    }

    private record Result(CommandOutcome outcome, boolean replayed) {
    }

    /**
     * A command's work stays in the transaction that claimed its key: work in a suspended transaction
     * (REQUIRES_NEW, NOT_SUPPORTED) would commit even when the command rolls back, outside the idempotency record.
     * Suspending fails before such work starts.
     */
    private static final TransactionSynchronization STAY_IN_THIS_TRANSACTION = new TransactionSynchronization() {
        @Override
        public void suspend() {
            throw new IllegalStateException("A v2 command's work stays in its transaction; REQUIRES_NEW or "
                + "NOT_SUPPORTED would escape its idempotency claim");
        }
    };

    private static final class Context implements CommandContext {

        private final ApiCaller caller;
        private final IfMatch ifMatch;
        private boolean checked;

        Context(ApiCaller caller, IfMatch ifMatch) {
            this.caller = caller;
            this.ifMatch = ifMatch;
        }

        @Override
        public ApiCaller caller() {
            return caller;
        }

        @Override
        public void checkVersion(long currentVersion, String versionHistory) {
            if (ifMatch == null) {
                throw new IllegalStateException("A create names no version to check");
            }
            ifMatch.check(currentVersion, versionHistory);
            checked = true;
        }
    }
}
