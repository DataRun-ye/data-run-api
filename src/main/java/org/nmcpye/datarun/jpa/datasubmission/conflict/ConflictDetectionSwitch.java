package org.nmcpye.datarun.jpa.datasubmission.conflict;

import org.nmcpye.datarun.config.activation.ActivationSwitch;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conflict detection's activation switch, {@code conflict-detection}, off by default
 * ({@code datarun.activation.conflict-detection}, or {@code DATARUN_ACTIVATION_CONFLICT_DETECTION}). Off, the upload
 * routes behave exactly as before it existed. Every change switch of submission review will require it, so no change
 * can be turned on before detection is.
 */
@Configuration
public class ConflictDetectionSwitch {

    public static final ActivationSwitch CONFLICT_DETECTION = ActivationSwitch.of("conflict-detection",
        "keeps a changed phone copy aside as a conflict after a server-side change, and answers an unchanged re-send "
            + "without writing it");

    @Bean
    ActivationSwitch conflictDetectionActivationSwitch() {
        return CONFLICT_DETECTION;
    }
}
