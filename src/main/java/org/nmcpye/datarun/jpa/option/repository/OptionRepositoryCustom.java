package org.nmcpye.datarun.jpa.option.repository;

import org.nmcpye.datarun.jpa.option.Option;

import java.util.List;

/**
 * @author Hamza Assada
 * @since 30/06/2025
 */
public interface OptionRepositoryCustom {
    List<Option> getOptions(String optionSetId, String key, Integer max);
}
