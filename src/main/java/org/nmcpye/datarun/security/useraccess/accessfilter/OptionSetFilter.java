package org.nmcpye.datarun.security.useraccess.accessfilter;

import org.nmcpye.datarun.activity.Activity;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateversion.DataTemplateTemplateVersion;
import org.nmcpye.datarun.datatemplateversion.repository.DataTemplateVersionRepository;
import org.nmcpye.datarun.optionset.OptionSet;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada, 21/03/2025
 */
@Component
public class OptionSetFilter extends DefaultJpaFilter<OptionSet> {
    private final DataTemplateVersionRepository templateRepository;

    public OptionSetFilter(DataTemplateVersionRepository templateRepository) {
        this.templateRepository = templateRepository;
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
        return getUserFormsWithWritePermission(userLogin)
            .stream().flatMap(f -> f.getFields().stream())
            .filter(f -> f.getType().isOptionsType())
            .map(FormDataElementConf::getOptionSet)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    public List<DataTemplateTemplateVersion> getUserFormsWithWritePermission(String userLogin) {
        final var currentUser = SecurityUtils.getCurrentUserDetailsOrThrow();
        return templateRepository.findTopByTemplateUidInOrderByVersionNumberDesc(currentUser.getUserFormsUIDs());
    }
}
