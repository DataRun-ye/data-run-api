package org.nmcpye.datarun.partyaccess.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/// @author Hamza Assada 28/12/2025
@Data @Builder
public class PartyDto {
    private String uid;
    private String type;
    private String label;
    private Map<String, Object> meta;
}
