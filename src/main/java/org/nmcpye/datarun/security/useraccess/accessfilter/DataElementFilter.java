package org.nmcpye.datarun.security.useraccess.accessfilter;

import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.postgres.domain.DataElement;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.service.impl.UserAccessibleElementsService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Hamza Assada, 21/03/2025
 */
@Component
public class DataElementFilter extends DefaultJpaFilter<DataElement> {
    private final UserAccessibleElementsService userAccessibleElementsService;

    public DataElementFilter(UserAccessibleElementsService userAccessibleElementsService) {
        this.userAccessibleElementsService = userAccessibleElementsService;
    }

    public static Specification<Activity> isEnabled() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("disabled"));
    }

    @Override
    public Specification<DataElement> getAccessSpecification(CurrentUserDetails user,
                                                             QueryRequest queryRequest) {
        List<String> userDataElements = userAccessibleElementsService
            .getUserFormsWithWritePermission(user.getUsername())
            .stream()
            .flatMap((form) -> form.getFieldsConf().stream())
            .map(FormDataElementConf::getId).toList();

        return (root, query, cb) -> {
            if (user.isSuper()) {
                return cb.conjunction();
            } else {
                return root.get("uid").in(userDataElements);
            }
        };
    }
}
