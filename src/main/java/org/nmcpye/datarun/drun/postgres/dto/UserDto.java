package org.nmcpye.datarun.drun.postgres.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link org.nmcpye.datarun.domain.User}
 */
@Value
@Builder
@AllArgsConstructor
public class UserDto implements Serializable {
    Long id;
    @Size(max = 11)
    String uid;
    @NotNull
    @Size(min = 1, max = 50)
    @Pattern(regexp = "^(?>[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)|(?>[_.@A-Za-z0-9-]+)$")
    String login;
    @Size(max = 20)
    String mobile;
    @Size(max = 50)
    String firstName;
    @Size(max = 50)
    String lastName;
    @Size(min = 5, max = 254)
    @Email
    String email;
    boolean activated = false;
    @Size(min = 2, max = 10)
    String langKey;
    @Size(max = 256)
    String imageUrl;
}
