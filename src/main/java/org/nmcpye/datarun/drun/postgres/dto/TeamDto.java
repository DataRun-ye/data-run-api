//package org.nmcpye.datarun.drun.postgres.dto;
//
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Size;
//import lombok.Value;
//
//import java.io.Serializable;
//import java.util.Set;
//
///**
// * DTO for {@link org.nmcpye.datarun.drun.postgres.domain.Team}
// */
//@Value
//public class TeamDto implements Serializable {
//    Long id;
//    @Size(max = 11)
//    String uid;
//    @NotNull
//    String code;
//    String name;
//    String description;
//    Boolean disabled;
//    Boolean deleteClientData;
//    ActivityDto activity;
//    Set<UserDto> users;
//    Long parentId;
//}
