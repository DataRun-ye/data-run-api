package org.nmcpye.datarun.jpa.accessfilter;

import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * @author Hamza Assada
 * @since 21/03/2025
 */
@Component
public class FormTemplateFilter extends DefaultJpaFilter<DataTemplate> {

    public static Specification<DataTemplate> isDeleted() {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.isTrue(root.get("deleted"));
    }

    public static Specification<DataTemplate> isActive() {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.notEqual(root.get("deleted"), true);
    }

    @Override
    public Specification<DataTemplate> getAccessSpecification(CurrentUserDetails user,
                                                              QueryRequest queryRequest) {
        Specification<DataTemplate> spec = (root, query, cb) -> {
            if (user.isSuper()) {
                return cb.conjunction();
            }

            if (user.getUserFormsUIDs() == null || user.getUserFormsUIDs().isEmpty()) {
                return cb.disjunction(); // user has no access
            }

            return root.get("uid").in(user.getUserFormsUIDs());

        };

        if (queryRequest == null || !queryRequest.isIncludeDeleted()) {
            spec = spec.and(isActive());
        }
        return spec;
    }
}
