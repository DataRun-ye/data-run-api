package org.nmcpye.datarun.jpa.datatemplate.repository;

import jakarta.validation.constraints.NotNull;
import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.datatemplate.ElementTemplateConfig;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/// Spring Data MongoDB repository for the DataFormTemplate entity.
@Repository
//@JaversSpringDataAuditable
public interface ElementTemplateConfigRepository
    extends BaseJpaIdentifiableRepository<ElementTemplateConfig, Long> {
    void deleteAllByTemplateUidAndTemplateVersionUid(String templateUid, String templateVersionUid);

    @Query("SELECT e.id FROM ElementTemplateConfig e " +
        "WHERE e.templateUid=:templateUid AND e.templateVersionUid=:templateVersionUid")
    List<Long> findIdsByTemplateUidAndTemplateVersionUid(@Param("templateUid") String templateUid,
                                                         @Param("templateVersionUid") String templateVersionUid);

    List<ElementTemplateConfig> findAllByTemplateUidAndTemplateVersionUid(String templateUid, String templateVersionUid);

    List<ElementTemplateConfig> findAllByTemplateUidAndVersionNo(String templateUid, Integer templateVersionNo);

    Optional<ElementTemplateConfig> findTopByTemplateUidOrderByVersionNoDesc(@NotNull String templateId);
}
