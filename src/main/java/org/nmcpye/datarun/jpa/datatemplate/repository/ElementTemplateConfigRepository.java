package org.nmcpye.datarun.jpa.datatemplate.repository;

import jakarta.validation.constraints.NotNull;
import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.datatemplate.ElementTemplateConfig;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/// Spring Data MongoDB repository for the DataFormTemplate entity.
@Repository
@JaversSpringDataAuditable
public interface ElementTemplateConfigRepository
        extends BaseJpaIdentifiableRepository<ElementTemplateConfig, Long> {
    void deleteAllByTemplateIdAndTemplateVersionId(String templateId, String templateVersionId);

    @Query("SELECT e.id FROM ElementTemplateConfig e " +
            "WHERE e.templateId=:templateId AND e.templateVersionId=:templateVersionId")
    List<Long> findIdsByTemplateIdAndTemplateVersionId(@Param("templateId") String templateId,
                                                       @Param("templateVersionId") String templateVersionId);

    List<ElementTemplateConfig> findAllByTemplateIdAndTemplateVersionId(String templateId, String templateVersionId);

    List<ElementTemplateConfig> findAllByTemplateIdAndVersionNo(String templateId, Integer versionNo);

    Optional<ElementTemplateConfig> findTopByTemplateIdOrderByVersionNoDesc(@NotNull String templateId);
}
