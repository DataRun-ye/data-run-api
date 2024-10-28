package org.nmcpye.datarun.drun.common;

import org.nmcpye.datarun.drun.postgres.common.IdentifiableObject;
import org.springframework.data.jpa.domain.Specification;

public class IdentifiableSpecifications<T extends IdentifiableObject<Long>> {
    public Specification<T> hasNameLike(String name) {
        return (root, query, criteriaBuilder) -> name == null ? null : criteriaBuilder.like(root.get("name"), "%" + name + "%");
    }

    public Specification<T> hasId(Long id) {
        return (root, query, criteriaBuilder) -> id == null ? null : criteriaBuilder.equal(root.get("id"), id);
    }

    public Specification<T> hasUid(String uid) {
        return (root, query, criteriaBuilder) -> uid == null ? null : criteriaBuilder.equal(root.get("uid"), uid);
    }

    public Specification<T> hasCode(String code) {
        return (root, query, criteriaBuilder) -> code == null ? null : criteriaBuilder.equal(root.get("code"), code);
    }

    public Specification<T> hasUser(String logIn) {
        return (root, query, criteriaBuilder) -> logIn == null ? null : criteriaBuilder.equal(root.get("createdBy"), logIn);
    }
}
