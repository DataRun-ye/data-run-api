package org.nmcpye.datarun.common;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <06-05-2025>
 */
@Value
@Builder
public class FindExistingSubmissionsDto {
    List<String> existing;
    List<String> missing;
}
