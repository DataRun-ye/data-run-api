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
import java.util.Map;

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

    /**
     * Rebuilds the flattened execution context for a single user.
     * This method translates the scattered runtime arrays into a persistent CQRS
     * view.
     */
    @Async
    @Transactional
    public void rebuildForUser(String userLogin) {
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

        // 6. Delete old context and insert new context
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
