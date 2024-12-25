//package org.nmcpye.datarun.mongo.repository;
//
//import org.nmcpye.datarun.mongo.domain.TeamFormPermission;
//import org.springframework.data.mongodb.repository.MongoRepository;
//import org.springframework.data.mongodb.repository.Query;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
///**
// * Spring Data MongoDB repository for the DataForm entity.
// */
//@Repository
//public interface TeamFormPermissionRepository
//    extends MongoRepository<TeamFormPermission, String> {
//
//    @Query("{'team': ?0, 'form': ?0}")
//    Optional<TeamFormPermission> findByTeamAndForm(String team, String form);
//
//    @Query("{'form': ?0}")
//    List<TeamFormPermission> findByForm(String form);
//
//    // Find form template IDs by single team and permission
//    @Query(value = "{ 'team': ?0, 'permissions': ?1 }", fields = "{ 'form': 1, '_id': 0 }")
//    List<String> findFormsByTeamAndPermission(String team, String permission);
//
//    // Find form template IDs by a list of team IDs and a specific permission
//    @Query(value = "{ 'team': { $in: ?0 }, 'permissions': ?1 }", fields = "{ 'form': 1, '_id': 0 }")
//    List<String> findFormsByTeamAndPermission(List<String> teams, String permission);
//
//    // Find all form template IDs for a given team (any permission)
//    @Query(value = "{ 'team': ?0 }", fields = "{ 'form': 1, '_id': 0 }")
//    List<String> findAllFormsByTeam(String team);
//
//    // Find all permissions for a specific team and form template
//    @Query(value = "{ 'team': ?0, 'form': ?1 }", fields = "{ 'permissions': 1, '_id': 0 }")
//    List<String> findPermissionsByTeamAndForm(String team, String form);
//
//    // Find form template IDs by a list of team IDs and a specific permission
//    @Query(value = "{ 'form': { $in: ?0 }, 'permissions': ?1 }", fields = "{ 'form': 1, '_id': 0 }")
//    List<String> findByFormsAndPermission(List<String> form, String permission);
//
//    // Check if a team has a specific permission for a form template
//    @Query(value = "{ 'team': ?0, 'form': ?1, 'permissions': ?2 }", fields = "{ '_id': 1 }")
//    boolean existsByTeamAndFormAndPermission(String team, String form, String permission);
//}
