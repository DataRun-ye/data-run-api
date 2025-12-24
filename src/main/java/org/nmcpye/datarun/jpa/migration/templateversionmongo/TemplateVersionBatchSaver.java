package org.nmcpye.datarun.jpa.migration.templateversionmongo;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateVersionRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Separate service to perform the transactional save. Keep transaction on this method so
 * that the migration loop can retry the whole batch if the save fails.
 */
//@Service
@RequiredArgsConstructor
class TemplateVersionBatchSaver {
    private final TemplateVersionRepository pgRepo;

    @Transactional
    public List<TemplateVersion> saveBatch(List<TemplateVersion> entities) {
        // You might want to chunk the save further for very large batches
        return pgRepo.persistAll(entities);
    }
}
