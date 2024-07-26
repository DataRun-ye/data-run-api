package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.domain.common.IdentifiableObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

@NoRepositoryBean
public interface IdentifiableRepository<T extends IdentifiableObject<Long>> extends JpaRepository<T, Long> {
    Optional<T> findByUid(String uid);

    Page<T> findAllByUser(Pageable pageable);

    //
    Page<T> findAllWithEagerRelationshipsByUser(Pageable pageable);

    //
    Optional<T> findOneWithEagerRelationshipsByUser(Long id);
}
