package org.nmcpye.datarun.jpa.etl.util;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.activity.repository.ActivityRepository;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.jpa.etl.exception.InvalidReferenceValueException;
import org.nmcpye.datarun.jpa.etl.model.ReferenceResolutionResult;
import org.nmcpye.datarun.jpa.option.Option;
import org.nmcpye.datarun.jpa.option.repository.OptionRepository;
import org.nmcpye.datarun.jpa.option.service.OptionService;
import org.nmcpye.datarun.jpa.option.service.OptionSetService;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Hamza Assada
 * @since 26/08/2025
 */
@Service
@RequiredArgsConstructor
public class ReferenceResolver {
    private final OptionService optionService;
    private final OptionSetService optionSetService;
    private final OptionRepository optionRepository;
    private final TeamRepository teamRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final ActivityRepository activityRepository;

    //    @Cacheable(cacheNames = USER_FORM_ACCESS_CACHE, key = "#rawValue + '_' + #reference.id")
    public ReferenceResolutionResult resolveReference(Object rawValue, FormDataElementConf reference) {
        if (rawValue == null) return null;

        ValueType vt = reference.getType();
        String sval = rawValue.toString().trim();

        // If rawValue already looks like an id, prefer to find by id first.
        // Try per ValueType
        if (vt == ValueType.SelectOne) {
            // option set -> map code -> option id
            // Accept either an option id or a code: try id lookup by calling getOptionByCode?
            // We have getOptionByCode - may use search heuristics
            Optional<Option> optional = resolveOptionByOptionSetAndCode(sval, reference.getOptionSet());
            final var option = optional
                .orElseThrow(() ->
                    new InvalidReferenceValueException("Unknown option '" + sval + "'"));
            return ReferenceResolutionResult
                .builder().uid(option.getUid())
                .kind("option_value").name(option.getName())
                .label(option.getLabel()).build();
        } else if (vt == ValueType.Team) {
            // try id -> code
            return resolveDomainCandidate(sval, teamRepository::findByUid, teamRepository::findById, "team");
        } else if (vt == ValueType.OrganisationUnit) {
            return resolveDomainCandidate(sval, orgUnitRepository::findByUid, orgUnitRepository::findById, "org_unit");
        } else if (vt == ValueType.Activity) {
            return resolveDomainCandidate(sval, activityRepository::findByUid, activityRepository::findById, "activity");
        } else {
            throw new InvalidReferenceValueException("Element type " + vt + " cannot be used as repeat category");
        }
    }

    // try to resolve option either by assuming input is id (findById) or code (getOptionByCode)
    public Optional<Option> resolveOptionByOptionSetAndCode(String input, String optionSetId) {
        try {
            return optionRepository.findByCodeAndOptionSetUid(input, optionSetId);
        } catch (Exception e) {
            throw new InvalidReferenceValueException("Failed to resolve option '" + input + "': " + e.getMessage(), e);
        }
    }

    /**
     * Map option codes (multi-select) to option ids for the given optionSet.
     * Should throw InvalidCategoryValueException if any code is unknown.
     */
    public Map<String, String> mapOptionCodesToUids(List<String> codes, String optionSetUid) {
        if (codes == null || codes.isEmpty()) return Collections.emptyMap();
        return optionService.validateAndMapOptionCodes(codes, optionSetUid);
    }

    private static <T extends JpaBaseIdentifiableObject> ReferenceResolutionResult resolveDomainCandidate(String candidate,
                                                                                                          Function<String, Optional<T>> findByUid,
                                                                                                          Function<String, Optional<T>> findById,
                                                                                                          String kind) {
        // try by (id)
        Optional<T> byUid = findByUid.apply(candidate);
        if (byUid.isPresent()) {
            var referenceObject = byUid.get();
            return ReferenceResolutionResult
                .builder()
                .uid(referenceObject.getUid())
                .name(referenceObject.getName())
                .kind(referenceObject.getName())
                .label(referenceObject.getLabel()).build();
        }

        // try by id
        Optional<T> byId = findById.apply(candidate);
        if (byId.isPresent()) {
            var referenceObject = byId.get();
            return ReferenceResolutionResult.builder()
                .uid(referenceObject.getId())
                .kind(kind)
                .name(referenceObject.getName())
                .label(referenceObject.getLabel()).build();
        }

        throw new InvalidReferenceValueException("Could not resolve " + kind + " candidate '" + candidate + "'");
    }
}
