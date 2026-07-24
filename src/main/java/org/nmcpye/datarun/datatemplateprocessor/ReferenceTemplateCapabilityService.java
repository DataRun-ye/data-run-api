package org.nmcpye.datarun.datatemplateprocessor;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.datatemplateelement.FieldTemplateElementDto;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReferenceTemplateCapabilityService {

    private final TemplateVersionRepository templateVersionRepository;

    public Set<String> findReferenceTemplateUids(Collection<String> templateUids) {
        if (templateUids == null || templateUids.isEmpty()) {
            return Collections.emptySet();
        }

        return templateVersionRepository.findLatestByTemplateUidIn(templateUids).stream()
                .filter(this::containsReference)
                .map(TemplateVersion::getTemplateUid)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean containsReference(Collection<String> templateUids) {
        return !findReferenceTemplateUids(templateUids).isEmpty();
    }

    public Set<String> findReferenceTemplateUidsAcrossVersions(Collection<String> templateUids) {
        if (templateUids == null || templateUids.isEmpty()) {
            return Collections.emptySet();
        }

        return templateVersionRepository.findByTemplateUidIn(templateUids).stream()
                .filter(this::containsReference)
                .map(TemplateVersion::getTemplateUid)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    private boolean containsReference(TemplateVersion version) {
        if (version.getFields() == null) {
            return false;
        }

        return version.getFields().stream()
                .filter(Objects::nonNull)
                .map(FieldTemplateElementDto::getType)
                .anyMatch(ValueType.Reference::equals);
    }
}
