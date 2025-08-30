package org.nmcpye.datarun.jpa.option.service;

import jakarta.validation.constraints.NotNull;
import org.nmcpye.datarun.jpa.option.Option;
import org.nmcpye.datarun.jpa.option.OptionGroup;
import org.nmcpye.datarun.jpa.option.OptionGroupSet;
import org.nmcpye.datarun.jpa.option.OptionSet;
import org.nmcpye.datarun.jpa.option.exception.InvalidOptionCodesException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    /**
     * Validate a set of option codes and return a mapping code -> optionId.
     * <p>
     * If any code is missing, this method throws InvalidOptionCodesException containing the missing codes.
     *
     * @param optionCodes set of option codes (client-submitted). Null/empty -> empty map.
     * @param optionSetUid optional option set id to restrict lookup; may be null to search globally.
     * @return map code -> optionUid for all codes provided (guaranteed to contain every code in optionCodes)
     * @throws InvalidOptionCodesException if any code is not found
     */
    Map<String, String> validateAndMapOptionCodes(Collection<String> optionCodes,
                                                  @NotNull String optionSetUid)
        throws InvalidOptionCodesException;

    Optional<Option> getOptionByCode(String code);

    List<Option> getOptionsByCode(List<String> code);

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
