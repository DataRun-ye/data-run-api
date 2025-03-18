package org.nmcpye.datarun.drun.postgres.common;

import org.springframework.data.jpa.domain.Specification;

public interface IdentifiableSpecifications<T extends IdentifiableEntity<Long>> {
    Specification<T> hasNameLike(String name);

    Specification<T> hasId(Long id);

    Specification<T> hasUid(String uid);

//    public Specification<T> hasCode(String code) {
//        return (root, query, criteriaBuilder) -> code == null ? null : criteriaBuilder.equal(root.get("code"), code);
//    }

//    Specification<T> canRead(String logIn);

//    Specification<T> hasAccess();
}
