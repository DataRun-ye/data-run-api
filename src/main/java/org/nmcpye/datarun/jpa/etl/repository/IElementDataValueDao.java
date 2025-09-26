package org.nmcpye.datarun.jpa.etl.repository;

import org.nmcpye.datarun.jpa.etl.dto.ElementDataValue;

import java.util.List;

/**
 * JDBC DAO for inserting/updating element_data_value rows using named parameters and batch operations.
 *
 * <p>Important assumptions:
 * <ul>
 *   <li>Partial unique indexes exist to distinguish single vs multi-select rows.</li>
 *   <li>UPSERT SQL handles "undelete" by clearing deleted_at on conflict.</li>
 * </ul>
 *
 * <p>Usage: batch upsert of ElementDataValue rows and marking submission rows as deleted.
 *
 * @author Hamza Assada
 * @since 13/08/2025
 */
public interface IElementDataValueDao {
    void upsertSubmissionValuesBatch(List<ElementDataValue> rows);

    void markAllAsDeletedForSubmission(String submissionId);
}
