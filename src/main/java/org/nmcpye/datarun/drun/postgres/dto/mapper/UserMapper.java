//package org.nmcpye.datarun.drun.postgres.dto.mapper;
//
//import org.mapstruct.BeanMapping;
//import org.mapstruct.Mapping;
//import org.mapstruct.Named;
//import org.nmcpye.datarun.domain.Authority;
//import org.nmcpye.datarun.domain.User;
//import org.nmcpye.datarun.drun.postgres.dto.UserDto;
//import org.nmcpye.datarun.service.dto.AdminUserDTO;
//import org.springframework.stereotype.Service;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * Mapper for the entity {@link User} and its DTO called {@link UserDto}.
// * <p>
// * Normal mappers are generated using MapStruct, this one is hand-coded as MapStruct
// * support is still in beta, and requires a manual step with an IDE.
// */
//@Service
//public class UserMapper {
//
//    public List<UserDto> usersToUserDTOs(List<User> users) {
//        return users.stream().filter(Objects::nonNull).map(this::userToUserDTO).toList();
//    }
//
//    public UserDto userToUserDTO(User user) {
//        return new UserDto(user);
//    }
//
//    public AdminUserDTO userToAdminUserDTO(User user) {
//        return new AdminUserDTO(user);
//    }
//
//    private Set<Authority> authoritiesFromStrings(Set<String> authoritiesAsString) {
//        Set<Authority> authorities = new HashSet<>();
//
//        if (authoritiesAsString != null) {
//            authorities = authoritiesAsString
//                .stream()
//                .map(string -> {
//                    Authority auth = new Authority();
//                    auth.setName(string);
//                    return auth;
//                })
//                .collect(Collectors.toSet());
//        }
//
//        return authorities;
//    }
//
//    public User userFromUid(String uid) {
//        if (uid == null) {
//            return null;
//        }
//        User user = new User();
//        user.setUid(uid);
//        return user;
//    }
//
//    @Named("uid")
//    @BeanMapping(ignoreByDefault = true)
//    @Mapping(target = "uid", source = "uid")
//    public UserDto toDtoUid(User user) {
//        if (user == null) {
//            return null;
//        }
//        UserDto userDto = new UserDto();
//        userDto.setUid(user.getUid());
//        return userDto;
//    }
//
//    @Named("idSet")
//    @BeanMapping(ignoreByDefault = true)
//    @Mapping(target = "uid", source = "uid")
//    public Set<UserDto> toDtoUidSet(Set<User> users) {
//        if (users == null) {
//            return Collections.emptySet();
//        }
//
//        Set<UserDto> userSet = new HashSet<>();
//        for (User userEntity : users) {
//            userSet.add(this.toDtoUid(userEntity));
//        }
//
//        return userSet;
//    }
//
//    @Named("login")
//    @BeanMapping(ignoreByDefault = true)
//    @Mapping(target = "uid", source = "uid")
//    @Mapping(target = "uid", source = "uid")
//    @Mapping(target = "login", source = "login")
//    @Mapping(target = "mobile", source = "mobile")
//    @Mapping(target = "firstName", source = "firstName")
//    @Mapping(target = "lastName", source = "lastName")
//    public UserDto toDtoLogin(User user) {
//        if (user == null) {
//            return null;
//        }
//        UserDto userDto = new UserDto();
//        userDto.setUid(user.getUid());
//        userDto.setLogin(user.getLogin());
//        userDto.setMobile(user.getMobile());
//        userDto.setFirstName(user.getFirstName());
//        userDto.setLastName(user.getLastName());
//        return userDto;
//    }
//
//    @Named("loginSet")
//    @BeanMapping(ignoreByDefault = true)
//    @Mapping(target = "uid", source = "uid")
//    @Mapping(target = "login", source = "login")
//    @Mapping(target = "mobile", source = "mobile")
//    @Mapping(target = "firstName", source = "firstName")
//    @Mapping(target = "lastName", source = "lastName")
//    public Set<UserDto> toDtoLoginSet(Set<User> users) {
//        if (users == null) {
//            return Collections.emptySet();
//        }
//
//        Set<UserDto> userSet = new HashSet<>();
//        for (User userEntity : users) {
//            userSet.add(this.toDtoLogin(userEntity));
//        }
//
//        return userSet;
//    }
//}
