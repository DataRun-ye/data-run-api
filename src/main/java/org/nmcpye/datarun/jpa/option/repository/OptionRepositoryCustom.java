package org.nmcpye.datarun.jpa.option.repository;

import org.nmcpye.datarun.jpa.option.Option;

import java.util.List;

/**
 * @author Hamza Assada 30/06/2025 (7amza.it@gmail.com)
 */
public interface OptionRepositoryCustom {
    List<Option> getOptions(String optionSetId, String key, Integer max);
}
