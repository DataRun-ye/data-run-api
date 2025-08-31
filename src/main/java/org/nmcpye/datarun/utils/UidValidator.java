package org.nmcpye.datarun.utils;

import lombok.NoArgsConstructor;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;

/**
 *
 */

/**
 * Small wrapper around UID validation logic.
 *
 * @author Hamza Assada
 * @since 31/08/2025
 */
@NoArgsConstructor
public final class UidValidator {

    public static void requireValid(String uid, String name) {
        if (!CodeGenerator.isValidUid(uid)) {
            throw new IllegalQueryException("Invalid uid for " + name + ": " + uid);
        }
    }

    public static boolean isValid(String uid) {
        return CodeGenerator.isValidUid(uid);
    }
}
