package org.nmcpye.datarun.drun.postgres.common;

import org.nmcpye.datarun.drun.postgres.repository.IdentifiableRelationalRepository;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalServiceImpl;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.data.jpa.domain.Specification;

public abstract class DefaultIdentifiableSpecifications<T extends Identifiable<Long>>
    extends IdentifiableRelationalServiceImpl<T>
    implements IdentifiableSpecifications<T> {

    public DefaultIdentifiableSpecifications(IdentifiableRelationalRepository<T> repository) {
        super(repository);
    }

    @Override
    public Specification<T> hasNameLike(String name) {
        return (root, query, criteriaBuilder) -> name == null ? null : criteriaBuilder.like(root.get("name"), "%" + name + "%");
    }

    @Override
    public Specification<T> hasId(Long id) {
        return (root, query, criteriaBuilder) -> id == null ? null : criteriaBuilder.equal(root.get("id"), id);
    }

    @Override
    public Specification<T> hasUid(String uid) {
        return (root, query, criteriaBuilder) -> uid == null ? null : criteriaBuilder.equal(root.get("uid"), uid);
    }

//    public Specification<T> hasCode(String code) {
//        return (root, query, criteriaBuilder) -> code == null ? null : criteriaBuilder.equal(root.get("code"), code);
//    }

    @Override
    public Specification<T> canRead(String logIn) {
        return (root, query, criteriaBuilder) -> logIn == null ? null : criteriaBuilder.equal(root.get("createdBy"), logIn);
    }

    @Override
    public Specification<T> hasAccess() {
        if (SecurityUtils.isAuthenticated()) {
            return canRead(SecurityUtils.getCurrentUserLogin().get());
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.disjunction();
        }
    }
}
