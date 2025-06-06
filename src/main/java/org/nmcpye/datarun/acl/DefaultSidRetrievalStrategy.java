//package org.nmcpye.datarun.acl;
//
//import org.nmcpye.datarun.security.CurrentUserDetails;
//import org.springframework.security.access.hierarchicalroles.NullRoleHierarchy;
//import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
//import org.springframework.security.acls.domain.GrantedAuthoritySid;
//import org.springframework.security.acls.domain.PrincipalSid;
//import org.springframework.security.acls.model.Sid;
//import org.springframework.security.acls.model.SidRetrievalStrategy;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.util.Assert;
//
//import java.util.*;
//
///**
// * @author Hamza Assada 31/05/2025 <7amza.it@gmail.com>
// */
//public class DefaultSidRetrievalStrategy implements SidRetrievalStrategy {
//
//    private RoleHierarchy roleHierarchy = new NullRoleHierarchy();
//
//    public DefaultSidRetrievalStrategy() {
//    }
//
//    public DefaultSidRetrievalStrategy(RoleHierarchy roleHierarchy) {
//        Assert.notNull(roleHierarchy, "RoleHierarchy must not be null");
//        this.roleHierarchy = roleHierarchy;
//    }
//
//    @Override
//    public List<Sid> getSids(Authentication authentication) {
//        Collection<? extends GrantedAuthority> authorities = this.roleHierarchy
//            .getReachableGrantedAuthorities(authentication.getAuthorities());
//
//        List<Sid> sids = new ArrayList<>(authorities.size() + 1);
//        sids.add(new PrincipalSid(authentication));
//        for (GrantedAuthority authority : authorities) {
//            sids.add(new GrantedAuthoritySid(authority));
//        }
//
//        final var userGroups = getCurrentUserDetails(authentication)
//            .map(CurrentUserDetails::getUserGroupsUIDs)
//            .orElse(Collections.emptySet());
//
//        for (String group : userGroups) {
//            sids.add(new TeamSid(group));
//        }
//
//        final var userTeams = getCurrentUserDetails(authentication)
//            .map(CurrentUserDetails::getUserTeamsUIDs)
//            .orElse(Collections.emptySet());
//
//        for (String team : userTeams) {
//            sids.add(new TeamSid(team));
//        }
//
//        return sids;
//    }
//
//    private Optional<CurrentUserDetails> getCurrentUserDetails(Authentication authentication) {
//        return Optional.ofNullable(authentication)
//            .map(Authentication::getPrincipal)
//            .filter(p -> p instanceof CurrentUserDetails)
//            .map(CurrentUserDetails.class::cast);
//    }
//
//    private CurrentUserDetails getCurrentUserDetailsOrThrow(Authentication authentication) {
//        return Optional.ofNullable(authentication)
//            .map(Authentication::getPrincipal)
//            .filter(p -> p instanceof CurrentUserDetails)
//            .map(CurrentUserDetails.class::cast).orElseThrow(() -> new SecurityException("User is not authenticated"));
//    }
//}
