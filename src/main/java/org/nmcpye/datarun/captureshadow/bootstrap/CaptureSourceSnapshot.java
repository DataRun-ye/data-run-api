package org.nmcpye.datarun.captureshadow.bootstrap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Component
public class CaptureSourceSnapshot {

    private final JdbcTemplate jdbc;
    private final CaptureSourceReader sourceReader;
    private final CaptureCanonicalizer canonicalizer;

    CaptureSourceSnapshot(
        JdbcTemplate jdbc,
        CaptureSourceReader sourceReader,
        CaptureCanonicalizer canonicalizer
    ) {
        this.jdbc = jdbc;
        this.sourceReader = sourceReader;
        this.canonicalizer = canonicalizer;
    }

    @Transactional(
        propagation = Propagation.REQUIRES_NEW,
        isolation = Isolation.REPEATABLE_READ,
        readOnly = true
    )
    public CaptureSourceBoundary capture() {
        return captureCurrentTransaction();
    }

    CaptureSourceBoundary captureCurrentTransaction() {
        assertRepeatableRead();
        long expectedCount = sourceReader.count();
        Long maximumSerial = sourceReader.maximumSerial();
        MessageDigest digest = sha256();
        long scannedCount = 0;
        long lastSerial = CaptureShadowBootstrap.INITIAL_SERIAL_CURSOR;
        Instant maximumLastModified = null;

        while (true) {
            List<CaptureSourceRow> rows = sourceReader.readPage(
                lastSerial,
                maximumSerial,
                CaptureShadowBootstrap.BATCH_SIZE
            );
            if (rows.isEmpty()) {
                break;
            }
            for (CaptureSourceRow row : rows) {
                canonicalizer.updateFingerprint(digest, row);
                scannedCount++;
                if (maximumLastModified == null
                    || row.lastModifiedDate().isAfter(maximumLastModified)) {
                    maximumLastModified = row.lastModifiedDate();
                }
            }
            lastSerial = rows.get(rows.size() - 1).serialNumber();
        }

        if (scannedCount != expectedCount) {
            throw new CaptureShadowBootstrapConflictException(
                "Source boundary scan count " + scannedCount
                    + " differs from source count " + expectedCount
            );
        }
        return new CaptureSourceBoundary(
            scannedCount,
            maximumSerial,
            HexFormat.of().formatHex(digest.digest()),
            maximumLastModified
        );
    }

    private void assertRepeatableRead() {
        String isolation = jdbc.queryForObject("SHOW transaction_isolation", String.class);
        if (!"repeatable read".equals(isolation)) {
            throw new CaptureShadowBootstrapConflictException(
                "Source snapshot requires REPEATABLE_READ but transaction isolation is "
                    + isolation
            );
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
