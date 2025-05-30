package org.nmcpye.datarun.schema;

import org.nmcpye.datarun.orgunit.OrgUnit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <01-05-2025>
 */
@Component
public class OrgUnitImportHandler implements ImportableEntityHandler<OrgUnit, Long> {

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
            // Add referential integrity checks, e.g., parent exists
        }
        return issues;
    }

    @Override
    public void persist(List<OrgUnit> items, ImportContext context) {
        // Save with JPA or batching repo
    }
}
