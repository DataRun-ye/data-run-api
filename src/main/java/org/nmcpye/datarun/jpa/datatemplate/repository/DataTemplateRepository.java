package org.nmcpye.datarun.jpa.datatemplate.repository;

import jakarta.persistence.LockModeType;
import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for the DataFormTemplate entity.
 */
@Repository
@JaversSpringDataAuditable
public interface DataTemplateRepository
    extends JpaIdentifiableRepository<DataTemplate> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM DataTemplate f WHERE f.uid = :uid")
    Optional<DataTemplate> findByUidForWrite(@Param("uid") String uid);
}
