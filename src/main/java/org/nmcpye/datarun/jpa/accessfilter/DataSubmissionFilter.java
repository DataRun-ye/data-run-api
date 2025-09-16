package org.nmcpye.datarun.jpa.accessfilter;

import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.userdetail.UserFormAccess;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * @author Hamza Assada
 * @since 21/03/2025
 */
@Component
public class DataSubmissionFilter extends DefaultJpaFilter<DataSubmission> {

    public static Specification<DataSubmission> isActive() {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.notEqual(root.get("deleted"), true);
    }

    @Override
    public Specification<DataSubmission> getAccessSpecification(CurrentUserDetails user,
                                                                QueryRequest queryRequest) {
        Specification<DataSubmission> spec = (root, query, cb) -> {
            if (user.isSuper()) {
                return cb.conjunction();
            }

            final var viewForms = user.getFormAccess().stream()
                .filter(UserFormAccess::canViewSubmission)
                .map(UserFormAccess::getForm)
                .toList();

            if (viewForms.isEmpty()) {
                return cb.disjunction(); // user has no access
            }

            return root.get("templateUid").in(viewForms);

        };

        if (queryRequest == null || !queryRequest.isIncludeDeleted()) {
            spec = spec.and(isActive());
        }
        return spec;
    }
}
