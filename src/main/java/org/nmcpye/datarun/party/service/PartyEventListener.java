//package org.nmcpye.datarun.party.service;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.event.TransactionPhase;
//import org.springframework.transaction.event.TransactionalEventListener;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Component
//@RequiredArgsConstructor
//public class PartyEventListener {
//
//    private final PartySyncService syncService;
//
//    // 1. Listen for OrgUnit changes
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    public void onOrgUnitSaved(OrgUnitSavedEvent event) {
//        var ou = event.getOrgUnit();
//
//        // Map OrgUnit specific fields to Meta
//        Map<String, Object> meta = new HashMap<>();
//        meta.put("code", ou.getCode());
//        meta.put("level", ou.getLevel());
//
//        syncService.syncParty(
//            ou.getId(),
//            ou.getUid(),
//            "ORG_UNIT",
//            ou.getName(), // or ou.getLabel()
//            ou.getParentId(), // Hierarchy is crucial here
//            meta
//        );
//    }
//
//    // 2. Listen for Team changes
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    public void onTeamSaved(TeamSavedEvent event) {
//        var team = event.getTeam();
//
//        syncService.syncParty(
//            team.getId(),
//            team.getUid(),
//            "TEAM",
//            team.getName(),
//            null, // Teams usually don't have a 'parent' in the Org Tree sense
//            Map.of("code", team.getCode())
//        );
//    }
//
//    // 3. Listen for User changes
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    public void onUserSaved(UserSavedEvent event) {
//        var user = event.getUser();
//
//        syncService.syncParty(
//            user.getId(),
//            user.getUid(),
//            "USER",
//            user.getFullName(), // Assuming you have a name field
//            null,
//            Map.of("username", user.getUsername())
//        );
//    }
//}
