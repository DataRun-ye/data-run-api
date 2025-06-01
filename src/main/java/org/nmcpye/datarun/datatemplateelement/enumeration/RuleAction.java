package org.nmcpye.datarun.datatemplateelement.enumeration;

/**
 * The RuleAction enumeration.
 */
public enum RuleAction {
    // Expression must be logical (true, false)
    Visibility,
    Show, // deprecated, use Visibility
    Hide, // deprecated, use Visibility
    // Expression must be logical (true, false)
    Constraint,
    Error, // deprecated, use Constraint
    Warning, // deprecated, use Constraint
    Filter, // deprecated, use choice Filter
    // Expression must be logical (true, false)
    StopRepeat,
    // Expression must be logical (true, false)
    Mandatory,
    // Expression result must be numerical >= 0
    Count,
    // Expression result must be a compatible Value with the Field type
    // i.e for default Value
    Assign,
}
