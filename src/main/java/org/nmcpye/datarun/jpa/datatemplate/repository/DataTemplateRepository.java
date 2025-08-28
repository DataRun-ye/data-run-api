package org.nmcpye.datarun.jpa.datatemplate.repository;

import jakarta.persistence.LockModeType;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/// Spring Data jpa repository for the DataTemplate entity.
@Repository
//@JaversSpringDataAuditable
public interface DataTemplateRepository
    extends JpaIdentifiableRepository<DataTemplate> {
    String TEMPLATE_BY_UID_CACHE = "templateByUidCache";

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM DataTemplate f WHERE f.uid = :uid")
    Optional<DataTemplate> findByUidForWrite(@Param("uid") String uid);

    @Cacheable(cacheNames = TEMPLATE_BY_UID_CACHE)
    @Override
    Optional<DataTemplate> findByUid(String uid);
}
