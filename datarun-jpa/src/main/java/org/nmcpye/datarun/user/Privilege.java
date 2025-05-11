package org.nmcpye.datarun.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.util.Collection;

/**
 * @author Hamza Assada, 21/03/2025
 */
@Entity
@Table(name = "role_privilege")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
public class Privilege {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    /**
     * Read, Write, ...
     */
    @NotNull
    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @ManyToMany(mappedBy = "privileges")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Collection<Role> roles;
}
