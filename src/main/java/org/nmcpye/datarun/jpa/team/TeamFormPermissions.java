package org.nmcpye.datarun.jpa.team;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.enumeration.FormPermission;

import java.util.Set;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class TeamFormPermissions {
    private String form;
    private Set<FormPermission> permissions;
}

