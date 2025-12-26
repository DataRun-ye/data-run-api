package org.nmcpye.datarun.partyaccess.entities;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PartySetSpec {
    private List<String> members;       // For STATIC
    private String rootId;              // For ORG_TREE
    private Integer depth;              // For ORG_TREE
    private List<String> tags;          // For TAG_FILTER
    private List<String> types;         // For TAG_FILTER
    private String sqlKey;              // For QUERY
    private Map<String, Object> params; // For QUERY
}
