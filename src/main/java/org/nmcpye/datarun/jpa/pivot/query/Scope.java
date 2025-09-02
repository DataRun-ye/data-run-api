package org.nmcpye.datarun.jpa.pivot.query;

/**
 * @author Hamza Assada 17/08/2025 (7amza.it@gmail.com)
 */
public enum Scope {
    /**
     * Field is one-per-submission (e.g., org_unit, submission_date)
     */
    SUBMISSION,

    /**
     * Field is one-per-repeat-instance (e.g., medicine quantity, medicine type category)
     */
    REPEAT_INSTANCE
}
