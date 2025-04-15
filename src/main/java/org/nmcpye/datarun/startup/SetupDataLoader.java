//package org.nmcpye.datarun.startup;
//
//import com.google.common.collect.Sets;
//import org.nmcpye.datarun.common.repository.UserRepository;
//import org.nmcpye.datarun.domain.Authority;
//import org.nmcpye.datarun.domain.Privilege;
//import org.nmcpye.datarun.domain.Role;
//import org.nmcpye.datarun.domain.User;
//import org.nmcpye.datarun.repository.AuthorityRepository;
//import org.nmcpye.datarun.repository.PrivilegeRepository;
//import org.nmcpye.datarun.repository.RoleRepository;
//import org.springframework.context.ApplicationListener;
//import org.springframework.context.event.ContextRefreshedEvent;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.Set;
//
///**
// * @author Hamza Assada, 24/03/2025
// */
//@Component
//public class SetupDataLoader implements
//    ApplicationListener<ContextRefreshedEvent> {
//
//    boolean alreadySetup = false;
//
//    private final UserRepository userRepository;
//
//    private final RoleRepository roleRepository;
//
//    private final AuthorityRepository authorityRepository;
//
//    private final PrivilegeRepository privilegeRepository;
//
//    private final PasswordEncoder passwordEncoder;
//
//    public SetupDataLoader(UserRepository userRepository, RoleRepository roleRepository, AuthorityRepository authorityRepository, PrivilegeRepository privilegeRepository, PasswordEncoder passwordEncoder) {
//        this.userRepository = userRepository;
//        this.roleRepository = roleRepository;
//        this.authorityRepository = authorityRepository;
//        this.privilegeRepository = privilegeRepository;
//        this.passwordEncoder = passwordEncoder;
//    }
//
//    @Override
//    @Transactional
//    public void onApplicationEvent(ContextRefreshedEvent event) {
//
//        if (alreadySetup)
//            return;
//        Authority adminAuthority
//            = createAuthorityIfNotFound("ROLE_ADMIN");
//        Authority userAuthority
//            = createAuthorityIfNotFound("ROLE_USER");
//
//        Privilege readPrivilege
//            = createPrivilegeIfNotFound("READ");
//        Privilege writePrivilege
//            = createPrivilegeIfNotFound("WRITE");
////        Privilege adminPrivilege
////            = createPrivilegeIfNotFound("ALL");
//
//        Set<Privilege> adminPrivileges = Sets.newHashSet(
//            readPrivilege, writePrivilege);
//        Set<Privilege> userPrivileges = Sets.newHashSet(
//            readPrivilege);
//
//        Set<Authority> adminAuthorities = Sets.newHashSet(
//            adminAuthority);
//        Set<Authority> userAuthorities = Sets.newHashSet(
//            userAuthority);
//
//        final var adminRules = Sets.newHashSet(createRoleIfNotFound("ADMIN", adminPrivileges));
//        final var userRules = Sets.newHashSet(createRoleIfNotFound("USER", Sets.newHashSet(readPrivilege)));
//
//        createUserIfNotFound("admin", adminAuthorities, adminRules);
//        createUserIfNotFound("user", userAuthorities, userRules);
//
//        alreadySetup = true;
//    }
//
//    @Transactional
//    Privilege createPrivilegeIfNotFound(String name) {
//
//        Privilege privilege = privilegeRepository.findByName(name).orElse(null);
//        if (privilege == null) {
//            privilege = new Privilege();
//            privilege.setName(name);
//            privilegeRepository.save(privilege);
//        }
//        return privilege;
//    }
//
//    @Transactional
//    Role createRoleIfNotFound(
//        String name, Set<Privilege> privileges) {
//
//        Role role = roleRepository.findByName(name).orElse(null);
//        if (role == null) {
//            role = new Role();
//            role.setName(name);
//            role.setPrivileges(privileges);
//            roleRepository.save(role);
//        }
//        return role;
//    }
//
//
//    @Transactional
//    Authority createAuthorityIfNotFound(String name) {
//
//        Authority authority = authorityRepository.findById(name).orElse(null);
//        if (authority == null) {
//            authority = new Authority().name(name);
//            authorityRepository.save(authority);
//        }
//
//        return authority;
//    }
//
//    @Transactional
//    User createUserIfNotFound(
//        String login, Collection<Authority> authorities, Collection<Role> roles) {
//
//        var user = userRepository.findOneByLogin(login).orElse(null);
//        if (user == null) {
//            user = new User();
//            user.setFirstName("Administrator");
//            user.setLastName("admin");
//            user.setLogin("admin");
//            user.setPassword(passwordEncoder.encode("adming"));
//            user.setEmail("test@test.com");
//            user.setAuthorities(new HashSet<>(authorities));
//            user.setRoles(new HashSet<>(roles));
//            user.setActivated(true);
//            userRepository.save(user);
//        }
//        return user;
//    }
//}
