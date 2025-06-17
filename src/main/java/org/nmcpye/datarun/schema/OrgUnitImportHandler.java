package org.nmcpye.datarun.schema;

import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.jpa.orgunitgroup.repository.OrgUnitGroupRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hamza Assada 01/05/2025 <7amza.it@gmail.com>
 */
@Component
public class OrgUnitImportHandler implements ImportableEntityHandler<OrgUnit, Long> {
    private final OrgUnitRepository orgUnitRepository;
    private final OrgUnitGroupRepository orgUnitGroupRepository;

    public OrgUnitImportHandler(OrgUnitRepository orgUnitRepository, OrgUnitGroupRepository orgUnitGroupRepository) {
        this.orgUnitRepository = orgUnitRepository;
        this.orgUnitGroupRepository = orgUnitGroupRepository;
    }

    @Override
    public Class<OrgUnit> getEntityType() {
        return OrgUnit.class;
    }

    @Override
    public Class<Long> getKeyType() {
        return Long.class;
    }

    @Override
    public List<ImportIssue> validate(List<OrgUnit> items, ImportContext context) {
        List<ImportIssue> issues = new ArrayList<>();
        for (OrgUnit unit : items) {
            if (unit.getCode() == null || unit.getCode().isBlank()) {
                issues.add(new ImportIssue("code", "Code is required", Severity.ERROR));
            }

            if (unit.getName() == null || unit.getName().isBlank()) {
                issues.add(new ImportIssue("name", "Name is required", Severity.ERROR));
            }

            if (unit.getParent() != null) {
                checkParent(unit.getParent(), issues);
            }
            // Add referential integrity checks, e.g., parent exists
        }
        return issues;
    }

    private void checkParent(OrgUnit parent, List<ImportIssue> issues) {
        if (parent.getUid() != null) {
            String uid = parent.getUid();
            if (!orgUnitRepository.existsByUid(uid)) {
                issues.add(new ImportIssue("parent uid",
                    uid + " not found", Severity.ERROR));
            }
        }

        if (parent.getCode() != null) {
            String code = parent.getCode();
            if (!orgUnitRepository.existsByCode(code)) {
                issues.add(new ImportIssue("parent code",
                    code + " not found", Severity.ERROR));
            }
        }

        if (parent.getId() != null) {
            String id = parent.getId();
            if (!orgUnitRepository.existsById(id)) {
                issues.add(new ImportIssue("parent id",
                    id + " not found", Severity.ERROR));
            }
        }
    }

    @Override
    public void persist(List<OrgUnit> items, ImportContext context) {
        // Save with JPA or batching repo
    }
}
