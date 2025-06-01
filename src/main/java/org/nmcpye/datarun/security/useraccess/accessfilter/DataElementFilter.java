package org.nmcpye.datarun.security.useraccess.accessfilter;

import org.nmcpye.datarun.activity.Activity;
import org.nmcpye.datarun.dataelement.DataElement;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateversion.DataTemplateTemplateVersion;
import org.nmcpye.datarun.datatemplateversion.repository.DataTemplateVersionRepository;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada, 21/03/2025
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
        Set<String> userDataElements = getUserFormsWithWritePermission(user.getUsername())
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

    public List<DataTemplateTemplateVersion> getUserFormsWithWritePermission(String userLogin) {
        final var currentUser = SecurityUtils.getCurrentUserDetailsOrThrow();

        return templateRepository.findTopByTemplateUidInOrderByVersionNumberDesc(currentUser.getUserFormsUIDs());
    }
}
