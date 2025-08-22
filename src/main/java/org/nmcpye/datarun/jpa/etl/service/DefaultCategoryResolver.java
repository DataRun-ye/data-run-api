package org.nmcpye.datarun.jpa.etl.service;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.activity.repository.ActivityRepository;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableObject;
import org.nmcpye.datarun.jpa.etl.exception.InvalidCategoryValueException;
import org.nmcpye.datarun.jpa.etl.model.CategoryResolutionResult;
import org.nmcpye.datarun.jpa.option.repository.OptionRepository;
import org.nmcpye.datarun.jpa.option.service.OptionService;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultCategoryResolver implements CategoryResolver {

    private final OptionService optionService;
    private final OptionRepository optionRepository;
    private final TeamRepository teamRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final ActivityRepository activityRepository;

    @Override
    public CategoryResolutionResult resolveCategory(Object rawValue, FormDataElementConf categoryElement) {
        if (rawValue == null) return null;

        ValueType vt = categoryElement.getType();
        String sval = rawValue.toString().trim();

        // If rawValue already looks like an id, prefer to find by id first.
        // Try per ValueType
        if (vt == ValueType.SelectOne) {
            // option set -> map code -> option id
            // Accept either an option id or a code: try id lookup by calling getOptionByCode?
            // We have getOptionByCode - may use search heuristics
            Optional<String> optId = tryResolveOptionByOptionSetAndCode(sval, categoryElement.getOptionSet());
            return CategoryResolutionResult
                .builder().id(optId
                    .orElseThrow(() -> new InvalidCategoryValueException("Unknown option '" + sval + "'")))
                .kind("option").build();
        } else if (vt == ValueType.Team) {
            // try id -> code
            return resolveDomainCandidate(sval, teamRepository::findByUid, teamRepository::findById, "team");
        } else if (vt == ValueType.OrganisationUnit) {
            return resolveDomainCandidate(sval, orgUnitRepository::findByUid, orgUnitRepository::findById, "org_unit");
        } else if (vt == ValueType.Activity) {
            return resolveDomainCandidate(sval, activityRepository::findByUid, activityRepository::findById, "activity");
        } else {
            throw new InvalidCategoryValueException("Element type " + vt + " cannot be used as repeat category");
        }
    }

    @Override
    public Map<String, String> mapOptionCodesToIds(List<String> codes, String optionSetId) {
        if (codes == null || codes.isEmpty()) return Collections.emptyMap();
        return optionService.validateAndMapOptionCodes(codes, optionSetId);
    }

    // try to resolve option either by assuming input is id (findById) or code (getOptionByCode)
    private Optional<String> tryResolveOptionByOptionSetAndCode(String input, String optionSetId) {
        try {
            return optionRepository.findByCodeAndOptionSetUid(input, optionSetId).map(JpaIdentifiableObject::getId);
        } catch (Exception e) {
            throw new InvalidCategoryValueException("Failed to resolve option '" + input + "': " + e.getMessage(), e);
        }
    }

    private CategoryResolutionResult resolveDomainCandidate(String candidate,
                                                            java.util.function.Function<String, Optional<?>> findByUid,
                                                            java.util.function.Function<String, Optional<?>> findById,
                                                            String kind) {

        // 1) try by (uid)
        Optional<?> byUid = findByUid.apply(candidate);
        if (byUid.isPresent()) {
            // assume domain entity has getId() method
            String id = extractId(byUid.get());
            return CategoryResolutionResult.builder().id(id).kind(kind).build();
        }

        // 2) try by id
        Optional<?> byId = findById.apply(candidate);
        if (byId.isPresent()) {
            String id = extractId(byId.get());
            return CategoryResolutionResult.builder().id(id).kind(kind).build();
        }

        throw new InvalidCategoryValueException("Could not resolve " + kind + " candidate '" + candidate + "'");
    }

    private String extractId(Object domainObj) {
        try {
            // try common getId() method
            return (String) domainObj.getClass().getMethod("getId").invoke(domainObj);
        } catch (Exception e) {
            throw new InvalidCategoryValueException("Domain entity missing getId(): " + domainObj.getClass().getName(), e);
        }
    }
}
