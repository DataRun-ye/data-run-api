package org.nmcpye.datarun.jpa.userrefreshtoken;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.jpa.user.User;

import java.time.Instant;
import java.util.Objects;

/**
 * @author Hamza Assada 15/04/2025 <7amza.it@gmail.com>
 */
@Entity
@Table(name = "refresh_token", indexes = {
    @Index(name = "idx_refresh_token_token_unq", columnList = "token", unique = true),
})
@Getter
@Setter
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    protected Long id;


    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonSerialize(contentAs = AuditableObject.class)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties(value = {"teams", "password", "authorities", "translations"}, allowSetters = true)
    private User user; // Your user entity

    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefreshToken that)) return false;
        return Objects.equals(getToken(), that.getToken());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getToken());
    }
}
