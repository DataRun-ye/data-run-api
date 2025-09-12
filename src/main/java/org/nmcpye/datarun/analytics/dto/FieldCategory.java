package org.nmcpye.datarun.analytics.dto;

/**
 * For UI grouping (e.g., in separate panels in the pivot builder)
 *
 * @author Hamza Assada
 * @since 26/08/2025
 */
public enum FieldCategory {
    /// e.g., Org Unit, Team, Activity
    CORE_DIMENSION,
    /// dynamic measure from form  e.g., MUAC Score, Household Size (from data_element)
    DYNAMIC_MEASURE,
    ///    e.g., Repeat Path, Category Name
    HIERARCHICAL_CONTEXT
}
