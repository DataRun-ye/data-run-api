package org.nmcpye.datarun.party.service;

import com.google.common.collect.Streams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.nmcpye.datarun.jpa.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;

import java.util.Map;
import java.util.stream.Stream;

//@Component
@Profile("run-party-migration") // Safety switch
@RequiredArgsConstructor
@Slf4j
public class InitialPartyMigrationRunner implements CommandLineRunner {

    private final OrgUnitRepository orgUnitRepo; // Your existing repos
    private final TeamRepository teamRepo;
    private final UserRepository userRepository;
    private final PartySyncService syncService;

    @Override
    public void run(String... args) {
        log.info("Starting initial Party Migration...");

//         1. Migrate OrgUnits
        var orgUnits = orgUnitRepo.findAll(); // In real prod, verify size first!
        for (var ou : orgUnits) {
            syncService.syncParty(
                ou.getId(),
                ou.getUid(),
                ou.getCode(),
                "ORG_UNIT",
                ou.getName(),
                ou.getLabel(),
                ou.getParent().getId(),
                Stream.concat(ou.getOrgUnitGroups().stream()
                        .map(oug -> "oug:" + oug.getCode()),
                    Stream.of("lvl:" + ou.getHierarchyLevel())).toList(), // tags
                Map.of("level", ou.getLevel(), "path", ou.getPath())
            );
        }
        log.info("Migrated {} OrgUnits", orgUnits.size());

        // 2. Migrate Teams
        var teams = teamRepo.findAll();
        for (var t : teams) {
            syncService.syncParty(
                t.getId(),
                t.getUid(),
                t.getCode(),
                "TEAM",
                t.getName(),
                t.getLabel(), null,
                Stream.concat(t.getManagedByTeams().stream()
                        .map(oug -> "managedByTeam:" + oug.getUid()),
                    t.getUsers().stream()
                        .map(u -> "user:" + u.getUid())).toList(),
                Map.of("code", t.getCode())
            );
        }
        log.info("Migrated {} Teams", teams.size());

        var users = userRepository.findAll();
        for (var u : users) {
            syncService.syncParty(
                u.getId(),
                u.getUid(),
                u.getLogin(),
                "USER",
                u.getFirstName(),
                Map.of(), null,
                Streams.concat(Stream.concat(u.getManagedByTeams().stream()
                            .map(oug -> "managedByTeam:" + oug.getUid()),
                        u.getUserGroups().stream()
                            .map(oug -> "userGroup:" + oug.getUid())),
                    u.getManagedByTeams().stream()
                        .map(oug -> "managedByGroup:" + oug.getUid())).toList(),
                Map.of("code", u.getCode())
            );
        }
        log.info("Migrated {} Users", teams.size());

        // ... Migrate Users ...
    }
}
