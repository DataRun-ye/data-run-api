package org.nmcpye.datarun.jpa.datatemplate.repository;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.nmcpye.datarun.jpa.datatemplate.CanonicalElement;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Hamza Assada
 * @since 25/09/2025
 */
@Repository
public interface CanonicalElementRepository extends BaseJpaRepository<CanonicalElement, String> {
    List<CanonicalElement> findByTemplateUid(String templateUid);

//    @Query(value = "SELECT id FROM canonical_element " +
//        "WHERE template_uid = :templateUid ", nativeQuery = true)
//    List<String> findUidsByTemplateUid(@Param("templateUid") String templateUid);

}
