package org.nmcpye.datarun.drun.postgres.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.user.User;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * DTO for {@link org.nmcpye.datarun.user.User}
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty(value = "id")
    @Size(max = 11)
    String uid;

    @NotNull
    @Size(min = 1, max = 50)
    String login;

    @Size(max = 20)
    String mobile;

    @Size(max = 50)
    String firstName;

    String lastName;

    public UserDto(User user) {
        this.uid = user.getUid();
        this.uid = user.getUid();
        this.login = user.getLogin();
        this.mobile = user.getMobile();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserDto userDto)) return false;
        return Objects.equals(getUid(), userDto.getUid());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUid());
    }
}
