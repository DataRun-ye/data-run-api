package org.nmcpye.datarun.jpa.etl.service;

import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.jpa.etl.model.CategoryResolutionResult;

import java.util.List;
import java.util.Map;

/**
 * Resolves a category raw value (id or code) to a canonical id + kind, and provides helper to map option codes.
 *
 * @author Hamza Assada 19/08/2025 (7amza.it@gmail.com)
 */
public interface CategoryResolver {
    /**
     * Resolve a raw category candidate (String or domain object) into canonical id and kind.
     * Returns a CategoryResolutionResult or throws InvalidCategoryValueException if cannot resolve.
     */
    CategoryResolutionResult resolveCategory(Object rawValue, FormDataElementConf categoryElement);

    /**
     * Map option codes (multi-select) to option ids for the given optionSet.
     * Should throw InvalidCategoryValueException if any code is unknown.
     */
    Map<String, String> mapOptionCodesToIds(List<String> codes, String optionSetId);
}
