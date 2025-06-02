package org.nmcpye.datarun.userdetail;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

/**
 * @author Hamza Assada, 20/03/2025
 */
@Value
@Builder
public class CurrentUserActivityInfo {
    Long userId;
    String userUID;
    Set<String> activityUIDs;
}
