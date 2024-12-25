package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.drun.postgres.domain.TeamFormPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamFormPermissionRepository extends JpaRepository<TeamFormPermission, Long> {

//    Optional<TeamFormPermission> findByTeamIdAndForm(Long teamId, String form);

//    List<TeamFormPermission> findByForm(String form);

    //    @Query("SELECT tfp.form FROM TeamFormPermission tfp WHERE tfp.team.id = ?1 AND ?2 MEMBER OF tfp.permissions")
//    List<String> findFormsByTeamAndPermission(Long teamId, String permission);
//
//    @Query("SELECT tfp.form FROM TeamFormPermission tfp WHERE tfp.team.id IN ?1 AND ?2 MEMBER OF tfp.permissions")
//    List<String> findFormsByTeamsAndPermission(List<Long> teamIds, String permission);
//
//    @Query("SELECT tfp.form FROM TeamFormPermission tfp WHERE tfp.team.id = ?1")
//    List<String> findAllFormsByTeam(Long teamId);
//
//    @Query("SELECT tfp.permissions FROM TeamFormPermission tfp WHERE tfp.team.id = ?1 AND tfp.form = ?2")
//    List<String> findPermissionsByTeamAndForm(Long teamId, String form);
//
//    @Query("SELECT CASE WHEN COUNT(tfp) > 0 THEN true ELSE false END FROM TeamFormPermission tfp WHERE tfp.team.id = ?1 AND tfp.form = ?2 AND ?3 MEMBER OF tfp.permissions")
//    boolean existsByTeamAndFormAndPermission(Long teamId, String form, String permission);
    Optional<TeamFormPermission> findByTeamIdAndForm(Long teamId, String form);

//    @Query("SELECT tfp.form FROM TeamFormPermission tfp WHERE tfp.team.id IN :teamIds AND :permission = ANY(tfp.permissions)")
//    List<String> findFormsByTeamsAndPermission(@Param("teamIds") List<Long> teamIds, @Param("permission") FormPermission permission);

//    @Query("SELECT tfp FROM TeamFormPermission tfp WHERE tfp.team.id = :teamId AND tfp.form = :form AND :permission = ANY(tfp.permissions)")
//    Optional<TeamFormPermission> findByTeamIdAndFormAndPermission(@Param("teamId") Long teamId, @Param("form") String form, @Param("permission") FormPermission permission);
//
//    @Query("SELECT tfp FROM TeamFormPermission tfp WHERE tfp.team.id = :teamId AND :permission = ANY(tfp.permissions)")
//    List<TeamFormPermission> findByTeamIdAndPermission(@Param("teamId") Long teamId, @Param("permission") FormPermission permission);
}
