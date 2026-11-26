package org.nmcpye.datarun.jpa.datasubmission.command;

import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * The submission lock, which every transaction that writes a stored submission takes before anything else: each
 * upload item on either route (a held release's replay among them), and the admin delete.
 *
 * <p>Its first statement bounds every lock wait for the rest of the transaction ({@code SET LOCAL lock_timeout}) to
 * {@code datarun.submissions.lock-timeout}, 5 s unless set, as v2's commands bound theirs
 * ({@code datarun.v2.commands.lock-timeout}); Hibernate ignores a numeric lock timeout on PostgreSQL. Then it reads
 * the stored submission under the lock ({@link DataSubmissionRepository#findByUidForWrite}). A change that holds the
 * submission past the bound fails the waiting transaction with SQLState 55P03 ({@link SubmissionLog#isLockTimeout}):
 * a stalled holder (a database link dropped in mid-transaction, an {@code UPDATE} left open in psql) costs each
 * waiting writer a bounded wait, after which its connection returns to the pool. PostgreSQL bounds each lock wait on
 * its own, so a writer queued behind another waiter on the same submission can wait up to about twice the bound:
 * first in the queue, then for the holder.</p>
 */
@Component
public class SubmissionLock {

    private final DataSubmissionRepository submissions;
    private final NamedParameterJdbcTemplate jdbc;
    private final Duration timeout;

    public SubmissionLock(
        DataSubmissionRepository submissions,
        NamedParameterJdbcTemplate jdbc,
        @Value("${datarun.submissions.lock-timeout:5s}") Duration timeout) {
        this.submissions = submissions;
        this.jdbc = jdbc;
        this.timeout = timeout;
    }

    /**
     * Takes the submission lock as the current transaction's first statements: bounds its lock waits, then returns the
     * stored submission with this uid, locked until the transaction ends, or null when none is stored. A null uid
     * locks nothing; the bound still applies.
     */
    public DataSubmission take(String uid) {
        jdbc.queryForObject("SELECT set_config('lock_timeout', :timeout, true)",
            new MapSqlParameterSource("timeout", Math.max(1, timeout.toMillis()) + "ms"), String.class);
        return uid == null ? null : submissions.findByUidForWrite(uid).orElse(null);
    }

    /**
     * The locked submission's document as the database prints it ({@code form_data::text}), or null when it has none:
     * read in the same transaction, under the lock, so it is the state {@link #take} returned, with every number exact.
     * A document loaded through Hibernate holds decimals as binary doubles.
     *
     * @param submissionId the locked submission's id ({@code data_submission.id})
     */
    public String documentText(String submissionId) {
        return jdbc.queryForObject("SELECT form_data::text FROM data_submission WHERE id = :id",
            new MapSqlParameterSource("id", submissionId), String.class);
    }
}
