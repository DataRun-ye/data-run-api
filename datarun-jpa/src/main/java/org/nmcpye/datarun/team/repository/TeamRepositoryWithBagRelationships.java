package org.nmcpye.datarun.team.repository;

import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface TeamRepositoryWithBagRelationships {
    Optional<Team> fetchBagRelationships(Team team);

    List<Team> fetchBagRelationships(List<Team> teams);

    Page<Team> fetchBagRelationships(Page<Team> teams);
//
//    void updatePaths();
//
//    void forceUpdatePaths();
}
