package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.common.IdentifiableRepository;
import org.nmcpye.datarun.drun.postgres.common.Identifiable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface IdentifiableRelationalRepository<T extends Identifiable<Long>>
    extends JpaRepository<T, Long>,
    JpaSpecificationExecutor<T>,
    IdentifiableRepository<T, Long> {
//    Optional<T> findByUid(String uid);
//    Set<T> findAllByUidIn(Collection<String> uids);

}
