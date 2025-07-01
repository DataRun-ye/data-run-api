package org.nmcpye.datarun.common;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * @author Hamza Assada 06/05/2025 (7amza.it@gmail.com)
 */
@Value
@Builder
public class FindExistingSubmissionsDto {
    List<String> existing;
    List<String> missing;
}
