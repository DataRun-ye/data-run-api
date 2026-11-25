package org.nmcpye.datarun.web.v2.idempotency;

import org.nmcpye.datarun.web.v2.caller.ApiCaller;
import org.nmcpye.datarun.web.v2.concurrency.EntityTag;
import org.nmcpye.datarun.web.v2.config.V2Properties;
import org.nmcpye.datarun.web.v2.problem.ApiProblemException;
import org.nmcpye.datarun.web.v2.problem.ProblemCode;
import org.nmcpye.datarun.web.v2.problem.ProblemError;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.MessageDigest;

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

    public <B> ResponseEntity<B> execute(CommandRequest request, Command command, OutcomeRenderer<B> renderer) {
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
            CommandOutcome outcome = command.run(new Context(request));
            if (outcome == null) {
                throw new IllegalStateException("A command returns its outcome");
            }
            store.recordOutcome(caller.userId(), request.idempotencyKey(), outcome);
            return new Result(outcome, false);
        });
        ResponseEntity.BodyBuilder response = ResponseEntity.status(result.outcome().responseStatus())
            .eTag(EntityTag.ofVersion(result.outcome().resourceVersion()).toString());
        if (result.replayed()) {
            response.header(REPLAYED_HEADER, "true");
        }
        return response.body(renderer.render(result.outcome()));
    }

    private record Result(CommandOutcome outcome, boolean replayed) {
    }

    private record Context(CommandRequest request) implements CommandContext {

        @Override
        public ApiCaller caller() {
            return request.caller();
        }

        @Override
        public void checkVersion(long currentVersion, String versionHistory) {
            request.ifMatch().check(currentVersion, versionHistory);
        }
    }
}
