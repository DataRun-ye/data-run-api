//package org.nmcpye.datarun.drun.postgres.service.impl;
//
//import org.nmcpye.datarun.drun.postgres.common.CurrentUserGroupInfo;
//import org.nmcpye.datarun.repository.UserRepository;
//import org.springframework.context.annotation.Lazy;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import static com.google.common.base.Preconditions.checkNotNull;
//
///**
// * This interface defined methods for getting access to the currently logged in
// * user and clearing the logged in state. If no user is logged in or the auto
// * access admin is active, all user access methods will return null.
// *
// * @author Torgeir Lorange Ostby
// */
//@Service
//public class CurrentUserService {
//
//    private final UserRepository userRepository;
//
//    private final Cache<CurrentUserGroupInfo> currentUserGroupInfoCache;
//
//    public CurrentUserService(@Lazy UserRepositoryExt userRepository, CacheProvider cacheProvider) {
//        checkNotNull(userRepository);
//
//        this.userRepository = userRepository;
//        this.currentUserGroupInfoCache = cacheProvider.createCurrentUserGroupInfoCache();
//    }
//
//    /**
//     * @return the username of the currently logged in user. If no user is
//     * logged in or the auto access admin is active, null is returned.
//     */
//    public String getCurrentUsername() {
//        return CurrentUserUtil.getCurrentUsername();
//    }
//
//    public User getCurrentUser() {
//        String username = CurrentUserUtil.getCurrentUsername();
//
//        return userRepository.getUserByUsername(username, true);
//    }
//
//    @Transactional(readOnly = true)
//    public boolean currentUserIsSuper() {
//        User user = getCurrentUser();
//
//        return user != null && user.isSuper();
//    }
//
//    @Transactional(readOnly = true)
//    public Set<OrganisationUnit> getCurrentUserOrganisationUnits() {
//        User user = getCurrentUser();
//
//        return user != null ? new HashSet<>(user.getOrganisationUnits()) : new HashSet<>();
//    }
//
//    @Transactional(readOnly = true)
//    public Set<OrganisationUnit> getCurrentUserTeamsOrganisationUnits() {
//        User user = getCurrentUser();
//        Set<OrganisationUnit> organisationUnits = new HashSet<>();
//        if (user != null) {
//            organisationUnits =
//                user
////                    .getUserData()
//                    .getTeams()
//                    .stream()
//                    .filter(team -> !team.getInactive())
//                    .filter(team -> !team.getActivity().getInactive())
//                    .map(Team::getAssignments)
//                    .flatMap(Collection::stream)
//                    .map(Assignment::getOrganisationUnit)
//                    .collect(Collectors.toSet());
//        }
//        return organisationUnits;
//    }
//
//    @Transactional(readOnly = true)
//    public Set<OrganisationUnit> getCurrentUserManagedTeamsOrganisationUnits() {
//        User user = getCurrentUser();
//        Set<OrganisationUnit> organisationUnits = new HashSet<>();
//        if (user != null) {
//            organisationUnits =
//                user
////                    .getUserData()
//                    .getTeams()
//                    .stream()
//                    .filter(team -> !team.getInactive())
//                    .filter(team -> !team.getActivity().getInactive())
//                    .map(Team::getManagedTeams)
//                    .flatMap(Collection::stream)
//                    .map(Team::getAssignments)
//                    .flatMap(Collection::stream)
//                    .map(Assignment::getOrganisationUnit)
//                    .collect(Collectors.toSet());
//        }
//        return organisationUnits;
//    }
//
//    @Transactional(readOnly = true)
//    public Set<OrganisationUnit> getCurrentUserTeamGroupsOrganisationUnits() {
//        User user = getCurrentUser();
//        Set<OrganisationUnit> organisationUnits = new HashSet<>();
//        if (user != null) {
//            organisationUnits =
//                user
//                    .getTeams()
//                    .stream()
//                    .filter(team -> !team.getInactive())
//                    .filter(team -> !team.getActivity().getInactive())
//                    .map(Team::getGroups)
//                    .flatMap(Collection::stream)
//                    .filter(teamGroup -> !teamGroup.getInactive())
//                    .map(TeamGroup::getMembers)
//                    .flatMap(Collection::stream)
//                    .map(Team::getAssignments)
//                    .flatMap(Collection::stream)
//                    .map(Assignment::getOrganisationUnit)
//                    .collect(Collectors.toSet());
//        }
//        return organisationUnits;
//    }
//
//    @Transactional(readOnly = true)
//    public Set<OrganisationUnit> getCurrentUserGroupsOrganisationUnits() {
//        User user = getCurrentUser();
//        Set<OrganisationUnit> organisationUnits = new HashSet<>();
//        if (user != null) {
//            organisationUnits =
//                user
//                    .getGroups()
//                    .stream()
//                    .filter(userGroup -> !userGroup.getInactive())
//                    .map(UserGroup::getMembers)
//                    .flatMap(Collection::stream)
//                    .map(User::getOrganisationUnits)
//                    .flatMap(Collection::stream)
//                    .collect(Collectors.toSet());
//        }
//        return organisationUnits;
//    }
//
//    @Transactional(readOnly = true)
//    public Set<OrganisationUnit> getCurrentUserManagedGroupsOrganisationUnits() {
//        User user = getCurrentUser();
//        Set<OrganisationUnit> organisationUnits = new HashSet<>();
//        if (user != null) {
//            organisationUnits =
//                user
//                    .getGroups()
//                    .stream()
//                    .filter(userGroup -> !userGroup.getInactive())
//                    .map(UserGroup::getManagedGroups)
//                    .flatMap(Collection::stream)
//                    .map(UserGroup::getMembers)
//                    .flatMap(Collection::stream)
//                    .map(User::getOrganisationUnits)
//                    .flatMap(Collection::stream)
//                    .collect(Collectors.toSet());
//        }
//        return organisationUnits;
//    }
//
//    @Transactional(readOnly = true)
//    public Set<OrganisationUnit> getAllCurrentUserAccessibleOrganisationUnits() {
//        Set<OrganisationUnit> organisationUnits = getCurrentUserTeamsOrganisationUnits();
//        organisationUnits.addAll(getCurrentUserManagedTeamsOrganisationUnits());
//        organisationUnits.addAll(getCurrentUserTeamGroupsOrganisationUnits());
////        organisationUnits.addAll(getCurrentUserGroupsOrganisationUnits());
//        organisationUnits.addAll(getCurrentUserManagedGroupsOrganisationUnits());
//        return organisationUnits;
//    }
//
//    @Transactional(readOnly = true)
//    public boolean currentUserIsAuthorized(String auth) {
//        User user = getCurrentUser();
//
//        return user != null && user.isAuthorized(auth);
//    }
//
////    @Transactional(readOnly = true)
////    public CurrentUserGroupInfo getCurrentUserGroupsInfo() {
////        User user = CurrentUserUtil.getCurrentUserDetails();
////
////        return user == null ? null : getCurrentUserGroupsInfo(user.getUid());
////    }
//
//    @Transactional(readOnly = true)
//    public CurrentUserGroupInfo getCurrentUserGroupsInfo(String userUID) {
//        return currentUserGroupInfoCache.get(userUID, userRepository::getCurrentUserGroupInfo);
//    }
//
//    @Transactional(readOnly = true)
//    public void invalidateUserGroupCache(String userUID) {
//        try {
//            currentUserGroupInfoCache.invalidate(userUID);
//        } catch (NullPointerException exception) {
//            // Ignore if key doesn't exist
//        }
//    }
//}
