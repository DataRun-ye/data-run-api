package org.nmcpye.datarun.jpa.accessfilter;

import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.dataelement.DataElement;
import org.nmcpye.datarun.mongo.datatemplateversion.DataTemplateVersion;
import org.nmcpye.datarun.mongo.datatemplateversion.repository.DataTemplateVersionRepository;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada
 * @since 21/03/2025
 */
@Component
public class DataElementFilter extends DefaultJpaFilter<DataElement> {
    private final DataTemplateVersionRepository templateRepository;

    public DataElementFilter(DataTemplateVersionRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public static Specification<Activity> isEnabled() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("disabled"));
    }

    @Override
    public Specification<DataElement> getAccessSpecification(CurrentUserDetails user,
                                                             QueryRequest queryRequest) {
        Collection<DataTemplateVersion> forms = getUserFormsWithWritePermission();

        Set<String> userDataElements = forms
            .stream()
            .flatMap((form) -> form.getFields().stream())
            .map(FormDataElementConf::getId).collect(Collectors.toSet());

        return (root, query, cb) -> {
            if (user.isSuper()) {
                return cb.conjunction();
            } else {
                return root.get("uid").in(userDataElements);
            }
        };
    }

    public Collection<DataTemplateVersion> getUserFormsWithWritePermission() {
        final var currentUser = SecurityUtils.getCurrentUserDetailsOrThrow();
        List<DataTemplateVersion> versions = templateRepository
            .findDistinctByTemplateUidInOrderByVersionNumberDesc(currentUser.getUserFormsUIDs());
        return versions;
    }
}
