package org.nmcpye.datarun.drun.postgres.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Utility repository to load bag relationships based on https://vladmihalcea.com/hibernate-multiplebagfetchexception/
 */
public class TeamRepositoryWithBagRelationshipsImpl
    implements TeamRepositoryWithBagRelationships {

    private static final String ID_PARAMETER = "id";
    private static final String TEAMS_PARAMETER = "teams";

    @PersistenceContext
    private EntityManager entityManager;

//    public List<Team> findAllWithManagedInfo() {
//        String currentUserLogin = SecurityUtils.getCurrentUserLogin()
//            .orElseThrow(() -> new IllegalStateException("Current user not found"));
//
//        String jpql = "SELECT DISTINCT t, " +
//            "CASE WHEN EXISTS (" +
//            "    SELECT 1 FROM Team mt " +
//            "    JOIN mt.users u " +
//            "    WHERE mt MEMBER OF t.managedByTeams AND u.login = :currentUserLogin" +
//            ") THEN true ELSE false END as isManaged " +
//            "FROM Team t " +
//            "LEFT JOIN FETCH t.activity " +
//            "LEFT JOIN FETCH t.users";
//
//        List<Object[]> results = entityManager.createQuery(jpql, Object[].class)
//            .setParameter("currentUserLogin", currentUserLogin)
//            .getResultList();
//
//        return results.stream().map(result -> {
//            Team team = (Team) result[0];
//            Boolean isManaged = (Boolean) result[1];
//            team.setIsManaged(isManaged);
//            return team;
//        }).collect(Collectors.toList());
//    }

    @Override
    public Optional<Team> fetchBagRelationships(Optional<Team> team) {
        return team.map(this::fetchTeams);
    }

    @Override
    public Page<Team> fetchBagRelationships(Page<Team> teams) {
        return new PageImpl<>(fetchBagRelationships(teams.getContent()), teams.getPageable(), teams.getTotalElements());
    }

    @Override
    public List<Team> fetchBagRelationships(List<Team> teams) {
        return Optional.of(teams).map(this::fetchTeams).orElse(Collections.emptyList());
    }

    Team fetchTeams(Team result) {
        return entityManager
            .createQuery(
                "select team from Team team " +
                    "left join fetch team.activity " +
                    "left join fetch team.users user " +
                    "where team.id = :id",
                Team.class
            )
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<Team> fetchTeams(List<Team> teams) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, teams.size()).forEach(index -> order.put(teams.get(index).getId(), index));
        List<Team> result = entityManager
            .createQuery(
                "select team from Team team " +
                    "left join fetch team.activity " +
                    "left join fetch team.users user " +
                    "where team in :teams",
                Team.class
            )
            .setParameter(TEAMS_PARAMETER, teams)
            .getResultList();
        result.sort((o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }
}
