package org.nmcpye.datarun.jpa.accessfilter;

import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.jpa.option.OptionSet;
import org.nmcpye.datarun.mongo.datatemplateversion.DataTemplateVersion;
import org.nmcpye.datarun.mongo.datatemplateversion.repository.DataTemplateVersionRepository;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada
 * @since 21/03/2025
 */
@Component
public class OptionSetFilter extends DefaultJpaFilter<OptionSet> {
    private final DataTemplateVersionRepository templateRepository;

    public OptionSetFilter(DataTemplateVersionRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Override
    public Specification<OptionSet> getAccessSpecification(CurrentUserDetails user,
                                                           QueryRequest queryRequest) {
        Set<String> userOptionSets = getUserOptionSets();

        return (root, query, cb) -> {
            if (user.isSuper()) {
                return cb.conjunction();
            } else {
                return root.get("id").in(userOptionSets);
            }
        };
    }

    private Set<String> getUserOptionSets() {
        return getUserFormsWithWritePermission()
            .stream().flatMap(f -> f.getFields().stream())
            .filter(f -> f.getType().isOptionsType())
            .map(FormDataElementConf::getOptionSet)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    public Collection<DataTemplateVersion> getUserFormsWithWritePermission() {
        final var currentUser = SecurityUtils.getCurrentUserDetailsOrThrow();
        List<DataTemplateVersion> versions = templateRepository
            .findDistinctByTemplateUidInOrderByVersionNumberDesc(currentUser.getUserFormsUIDs());
        return versions;
    }
}
