package org.nmcpye.datarun.domain;

import jakarta.persistence.*;

import java.util.Collection;

/**
 * @author Hamza, 21/03/2025
 */
@Entity
@Table(name = "user_role")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToMany(mappedBy = "roles")
    private Collection<User> users;

    @ManyToMany
    @JoinTable(
        name = "roles_privileges",
        joinColumns = @JoinColumn(
            name = "role_id", referencedColumnName = "id"),
        inverseJoinColumns = @JoinColumn(
            name = "privilege_id", referencedColumnName = "id"))
    private Collection<Privilege> privileges;
}
