package org.nmcpye.datarun.jpa.option.service;

import org.nmcpye.datarun.jpa.option.Option;
import org.nmcpye.datarun.jpa.option.OptionGroup;
import org.nmcpye.datarun.jpa.option.OptionGroupSet;
import org.nmcpye.datarun.jpa.option.OptionSet;

import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing {@link Option}.
 */
public interface OptionService {
    // -------------------------------------------------------------------------
    // OptionSet
    // -------------------------------------------------------------------------

    OptionSet saveOptionSet(OptionSet optionSet);

    void updateOptionSet(OptionSet optionSet);

//    /**
//     * Validate an {@link OptionSet}.
//     *
//     * @param optionSet the set to validate
//     * @throws IllegalQueryException when the provided {@link OptionSet} has
//     *                               validation errors
//     */
//    void validateOptionSet(OptionSet optionSet) throws IllegalQueryException;
//
//    ErrorMessage validateOption(OptionSet optionSet, Option option);

    Optional<OptionSet> getOptionSet(String id);

    Optional<OptionSet> getOptionSetByName(String name);

    Optional<OptionSet> getOptionSetByCode(String code);

    void deleteOptionSet(OptionSet optionSet);

    List<OptionSet> getAllOptionSets();

    List<Option> getOptions(String optionSetId, String name, Integer max);

    // -------------------------------------------------------------------------
    // Option
    // -------------------------------------------------------------------------

    void updateOption(Option option);

    Optional<Option> getOption(String id);

    Optional<Option> getOptionByCode(String code);

    void deleteOption(Option option);

    // -------------------------------------------------------------------------
    // OptionGroup
    // -------------------------------------------------------------------------

    OptionGroup saveOptionGroup(OptionGroup group);

    void updateOptionGroup(OptionGroup group);

    Optional<OptionGroup> getOptionGroup(String uid);

    void deleteOptionGroup(OptionGroup group);

    List<OptionGroup> getAllOptionGroups();

    // -------------------------------------------------------------------------
    // OptionGroupSet
    // -------------------------------------------------------------------------

    OptionGroupSet saveOptionGroupSet(OptionGroupSet group);

    void updateOptionGroupSet(OptionGroupSet group);

    Optional<OptionGroupSet> getOptionGroupSet(String uid);

    void deleteOptionGroupSet(OptionGroupSet group);

    List<OptionGroupSet> getAllOptionGroupSets();
}
