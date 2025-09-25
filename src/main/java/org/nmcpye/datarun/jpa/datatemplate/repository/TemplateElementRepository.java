package org.nmcpye.datarun.jpa.datatemplate.repository;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.datatemplate.TemplateElement;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/// Spring Data MongoDB repository for the DataFormTemplate entity.
@Repository
//@JaversSpringDataAuditable
public interface TemplateElementRepository
    extends BaseJpaIdentifiableRepository<TemplateElement, Long> {
    void deleteAllByTemplateUidAndTemplateVersionUid(String templateUid, String templateVersionUid);

    @Query("SELECT e.id FROM TemplateElement e " +
        "WHERE e.templateUid=:templateUid AND e.templateVersionUid=:templateVersionUid")
    List<Long> findIdsByTemplateUidAndTemplateVersionUid(@Param("templateUid") String templateUid,
                                                         @Param("templateVersionUid") String templateVersionUid);

    Optional<TemplateElement> findByUid(@Size(max = 11) String uid);

    List<TemplateElement> findAllByTemplateUidAndTemplateVersionUid(String templateUid, String templateVersionUid);

    List<TemplateElement> findAllByTemplateUidAndVersionNo(String templateUid, Integer templateVersionNo);

    Optional<TemplateElement> findTopByTemplateUidOrderByVersionNoDesc(@NotNull String templateId);

    List<TemplateElement> findByTemplateVersionUid(String templateVersionUid);

    Optional<TemplateElement> findByRepeatUid(String repeatUid);

    // load only repeat entries (cached in app)
    @Query("SELECT e.repeatUid, e.canonicalPath FROM TemplateElement e WHERE e.elementKind = 'REPEAT'")
    List<Object[]> findAllRepeats(); // returns [repeatUid, semanticPath] rows

    List<TemplateElement> findBySchemaFingerprint(String schemaFingerprint);
}
