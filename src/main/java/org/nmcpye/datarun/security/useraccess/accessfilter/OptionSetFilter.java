package org.nmcpye.datarun.security.useraccess.accessfilter;

import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.postgres.domain.OptionSet;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.service.impl.UserAccessibleElementsService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada, 21/03/2025
 */
@Component
public class OptionSetFilter extends DefaultJpaFilter<OptionSet> {
    private final UserAccessibleElementsService userAccessibleElementsService;

    public OptionSetFilter(UserAccessibleElementsService userAccessibleElementsService) {
        this.userAccessibleElementsService = userAccessibleElementsService;
    }

    public static Specification<Activity> isEnabled() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("disabled"));
    }

    @Override
    public Specification<OptionSet> getAccessSpecification(CurrentUserDetails user,
                                                           QueryRequest queryRequest) {
        Set<String> userOptionSets = getUserOptionSets(user.getUsername());

        return (root, query, cb) -> {
            if (user.isSuper()) {
                return cb.conjunction();
            } else {
                return root.get("uid").in(userOptionSets);
            }
        };
    }

    private Set<String> getUserOptionSets(String userLogin) {
        return userAccessibleElementsService.getAllAccessibleUserForms(userLogin)
            .stream().flatMap(f -> f.getFieldsConf().stream())
            .filter(f -> f.getType().isOptionsType())
            .map(FormDataElementConf::getOptionSet)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }
}
