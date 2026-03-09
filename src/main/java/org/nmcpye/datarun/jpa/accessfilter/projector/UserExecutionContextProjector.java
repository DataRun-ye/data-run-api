package org.nmcpye.datarun.jpa.accessfilter.projector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.enumeration.AccessLevel;
import org.nmcpye.datarun.common.enumeration.FormPermission;
import org.nmcpye.datarun.jpa.accessfilter.entity.UserExecutionContext;
import org.nmcpye.datarun.jpa.accessfilter.repository.UserExecutionContextRepository;
import org.nmcpye.datarun.security.CurrentUserInfoService;
import org.nmcpye.datarun.userdetail.UserFormAccess;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.nmcpye.datarun.jpa.accessfilter.event.UserAccessRulesChangedEvent;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.AssignmentPartyBinding;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentMemberRepository;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentPartyBindingRepository;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.party.dto.PartyResolutionRequest;
import org.nmcpye.datarun.party.dto.ResolvedParty;
import org.nmcpye.datarun.party.resolution.engine.PartyResolutionEngine;
import org.springframework.context.event.EventListener;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Background projector service that handles Phase 1 of the CQRS execution
 * boundary.
 * It flattens the complex planning rules (Teams, OrgUnits, Assignments) into
 * the
 * fast UserExecutionContext table for the API and Mobile clients.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserExecutionContextProjector {

    private final UserExecutionContextRepository executionContextRepository;
    private final CurrentUserInfoService currentUserInfoService;
    private final AssignmentMemberRepository assignmentMemberRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentPartyBindingRepository assignmentPartyBindingRepository;
    private final PartyResolutionEngine partyResolutionEngine;
    private final ObjectMapper objectMapper;

    /**
     * Rebuilds the flattened execution context for a single user.
     * This method translates the scattered runtime arrays into a persistent CQRS
     * view.
     */
    @Async
    @EventListener
    @Transactional
    public void rebuildForUser(UserAccessRulesChangedEvent event) {
        String userLogin = event.getUserLogin();
        log.info("Rebuilding CQRS User Execution Context for user: {}", userLogin);

        // 1. Fetch current info
        var teamInfo = currentUserInfoService.getUserTeamInfo(userLogin);
        String userUid = teamInfo.getUserUID();

        var activityInfo = currentUserInfoService.getUserActivityInfo(userLogin);
        var groupInfo = currentUserInfoService.getUserGroupIds(userLogin);
        var formAccesses = currentUserInfoService.getUserFormAccess(userLogin, teamInfo.getTeamUIDs());

        // We use a map to deduplicate and resolve the Highest Permission per Entity
        // Key: "ENTITY_TYPE:ENTITY_UID", Value: UserExecutionContext
        Map<String, UserExecutionContext> contextMap = new HashMap<>();

        // 2. Flatten Teams
        teamInfo.getTeamUIDs().forEach(uid -> appendOrUpgrade(contextMap, userUid, "TEAM", uid, AccessLevel.READ));
        teamInfo.getManagedTeamUIDs()
                .forEach(uid -> appendOrUpgrade(contextMap, userUid, "TEAM", uid, AccessLevel.ALL));

        // 3. Flatten Activities
        activityInfo.getActivityUIDs()
                .forEach(uid -> appendOrUpgrade(contextMap, userUid, "ACTIVITY", uid, AccessLevel.READ));

        // 4. Flatten User Groups
        groupInfo.getUserGroupUIDs()
                .forEach(uid -> appendOrUpgrade(contextMap, userUid, "USER_GROUP", uid, AccessLevel.READ));

        // 5. Flatten Data Templates (Forms)
        for (UserFormAccess formAccess : formAccesses) {
            String formUid = formAccess.getForm();
            AccessLevel highestFormLevel = AccessLevel.READ;

            for (FormPermission perm : formAccess.getPermissions()) {
                AccessLevel mapped = mapFormPermission(perm);
                if (mapped.ordinal() > highestFormLevel.ordinal()) {
                    highestFormLevel = mapped;
                }
            }
            appendOrUpgrade(contextMap, userUid, "DATA_TEMPLATE", formUid, highestFormLevel);
        }

        // 6. Process Assignments (Legacy + New Bindings)
        Set<String> principalIds = new HashSet<>();
        principalIds.add(userUid);
        principalIds.addAll(teamInfo.getTeamUIDs());
        principalIds.addAll(groupInfo.getUserGroupUIDs());

        List<String> activeAssignmentIds = assignmentMemberRepository.findActiveAssignmentIdsForPrincipalsAndTeams(
                principalIds, teamInfo.getTeamUIDs());

        if (!activeAssignmentIds.isEmpty()) {
            List<Assignment> assignments = assignmentRepository.findAllById(activeAssignmentIds);

            List<AssignmentPartyBinding> bindings = assignmentPartyBindingRepository
                    .findByAssignmentIdIn(activeAssignmentIds);
            Map<String, List<AssignmentPartyBinding>> bindingsByAssignment = bindings.stream()
                    .collect(Collectors.groupingBy(b -> String.valueOf(b.getAssignment().getId())));

            for (Assignment assignment : assignments) {
                String assignmentIdStr = String.valueOf(assignment.getId());

                // Legacy Path (if still present)
                if (assignment.getTeam() != null) {
                    appendOrUpgrade(contextMap, userUid, "TEAM", assignment.getTeam().getUid(), AccessLevel.READ);
                }
                if (assignment.getOrgUnit() != null) {
                    appendOrUpgrade(contextMap, userUid, "ORG_UNIT", assignment.getOrgUnit().getUid(),
                            AccessLevel.READ);
                }

                // New Bindings Path
                List<AssignmentPartyBinding> assignmentBindings = bindingsByAssignment.getOrDefault(assignmentIdStr,
                        List.of());
                for (AssignmentPartyBinding binding : assignmentBindings) {
                    // Only process bindings that apply to our principal (or are global/null
                    // principal)
                    boolean appliesToUser = binding.getPrincipalId() == null
                            || principalIds.contains(binding.getPrincipalId());
                    if (!appliesToUser) {
                        continue;
                    }

                    PartyResolutionRequest resolutionRequest = PartyResolutionRequest.builder()
                            .assignmentId(assignmentIdStr)
                            .userId(userUid)
                            .limit(1000000)
                            .offset(0)
                            .build();

                    String specJson = null;
                    try {
                        specJson = objectMapper.writeValueAsString(binding.getPartySet().getSpec());
                    } catch (Exception e) {
                        log.error("Failed to serialize PartySetSpec", e);
                    }

                    // Bypass security (isMaterialized = false) because we are currently BUILDING
                    // the security context
                    List<ResolvedParty> resolvedParties = partyResolutionEngine.executeStrategy(
                            binding.getPartySet().getKind(),
                            binding.getPartySet().getId(),
                            specJson,
                            false,
                            resolutionRequest);
                    for (ResolvedParty rp : resolvedParties) {
                        appendOrUpgrade(contextMap, userUid, rp.getSource(), rp.getUid(), AccessLevel.READ);
                    }

                    if (binding.getVocabulary() != null) {
                        appendOrUpgrade(contextMap, userUid, "DATA_TEMPLATE", binding.getVocabulary().getUid(),
                                AccessLevel.READ);
                    }
                }
            }
        }

        // 7. Delete old context and insert new context
        executionContextRepository.deleteByUserUid(userUid);
        executionContextRepository.persistAllAndFlush(contextMap.values());
        log.debug("Successfully rebuilt {} context rows for user {}", contextMap.size(), userUid);
    }

    private void appendOrUpgrade(Map<String, UserExecutionContext> map, String userUid, String type, String entityUid,
            AccessLevel level) {
        String key = type + ":" + entityUid;
        UserExecutionContext existing = map.get(key);

        if (existing == null) {
            map.put(key, UserExecutionContext.builder()
                    .userUid(userUid)
                    .entityType(type)
                    .entityUid(entityUid)
                    .resolvedPermission(level)
                    .build());
        } else {
            // Upgrade permission if the new one is higher
            if (level.ordinal() > existing.getResolvedPermission().ordinal()) {
                existing.setResolvedPermission(level);
            }
        }
    }

    private AccessLevel mapFormPermission(FormPermission permission) {
        return switch (permission) {
            case DELETE_SUBMISSIONS, DELETE_SUBMISSIONS_FROM_USERS -> AccessLevel.DELETE;
            case ADD_SUBMISSIONS, EDIT_SUBMISSIONS, EDIT_SUBMISSIONS_FROM_USERS, APPROVE_SUBMISSIONS ->
                AccessLevel.UPDATE;
            default -> AccessLevel.READ; // VIEW_SUBMISSIONS, VIEW_SUBMISSIONS_FROM_USERS
        };
    }
}
