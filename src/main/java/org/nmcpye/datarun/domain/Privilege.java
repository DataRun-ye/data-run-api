package org.nmcpye.datarun.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

/**
 * @author Hamza, 21/03/2025
 */
@Entity
@Table(name = "user_role")
@Getter
@Setter
public class Privilege {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * Read, Write, ...
     */
    private String name;

    @ManyToMany(mappedBy = "privileges")
    private Collection<Role> roles;
}
