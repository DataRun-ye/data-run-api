package org.nmcpye.datarun.jpa.option.service;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.option.Option;
import org.nmcpye.datarun.jpa.option.OptionGroup;
import org.nmcpye.datarun.jpa.option.OptionGroupSet;
import org.nmcpye.datarun.jpa.option.OptionSet;
import org.nmcpye.datarun.jpa.option.exception.InvalidOptionCodesException;
import org.nmcpye.datarun.jpa.option.repository.OptionGroupRepository;
import org.nmcpye.datarun.jpa.option.repository.OptionGroupSetRepository;
import org.nmcpye.datarun.jpa.option.repository.OptionRepository;
import org.nmcpye.datarun.jpa.option.repository.OptionSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link Option}.
 *
 * @author Hamza Assada 30/06/2025 (7amza.it@gmail.com)
 */
@Service
@Slf4j
public class DefaultOptionService implements OptionService {

    private final OptionRepository optionRepository;

    private final OptionSetRepository optionSetRepository;

    private final OptionGroupRepository optionGroupRepository;

    private final OptionGroupSetRepository optionGroupSetRepository;

    public DefaultOptionService(
        OptionSetRepository optionSetRepository,
        OptionRepository optionRepository,
        OptionGroupRepository optionGroupRepository,
        OptionGroupSetRepository optionGroupSetRepository) {
        this.optionRepository = optionRepository;
        this.optionSetRepository = optionSetRepository;
        this.optionGroupRepository = optionGroupRepository;
        this.optionGroupSetRepository = optionGroupSetRepository;
    }

    @Override
    @Transactional
    public OptionSet saveOptionSet(OptionSet optionSet) {
//        validateOptionSet(optionSet);
        return optionSetRepository.save(optionSet);
    }

    @Override
    @Transactional
    public void updateOptionSet(OptionSet optionSet) {
//        validateOptionSet(optionSet);
        optionSetRepository.update(optionSet);
    }

//    @Override
//    public void validateOptionSet(OptionSet optionSet) throws IllegalQueryException {
//        if (optionSet.getValueType() != ValueType.MULTI_TEXT) {
//            return;
//        }
//        for (Option option : optionSet.getOptionSetOptions()) {
//            if (option.getId() != null && option.getId() != 0L && option.getCode() == null) {
//                option = optionRepository.get(option.getId());
//            }
//            ErrorMessage error = validateOption(optionSet, option);
//            if (error != null) {
//                throw new IllegalQueryException(error);
//            }
//        }
//    }
//
//    @Override
//    public ErrorMessage validateOption(OptionSet optionSet, Option option) {
//        if (optionSet != null &&
//            optionSet.getValueType() == ValueType.MULTI_TEXT &&
//            option.getCode().contains(ValueType.MULTI_TEXT_SEPARATOR)) {
//            return new ErrorMessage(ErrorCode.E1118, optionSet.getUid(), option.getCode());
//        }
//        return null;
//    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OptionSet> getOptionSet(String id) {
        return optionSetRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OptionSet> getOptionSetByName(String name) {
        return optionSetRepository.findFirstByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OptionSet> getOptionSetByCode(String code) {
        return optionSetRepository.findFirstByCode(code);
    }

    @Override
    @Transactional
    public void deleteOptionSet(OptionSet optionSet) {
        optionSetRepository.delete(optionSet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OptionSet> getAllOptionSets() {
        return optionSetRepository.findAll();
    }

    // -------------------------------------------------------------------------
    // Option
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<Option> getOptions(String optionSetId, String key, Integer max) {
        List<Option> options;

        if (key != null || max != null) {
            // Use query as option set size might be very high
            options = optionRepository.getOptions(optionSetId, key, max);
        } else {
            // Return all from object association to preserve custom order
            OptionSet optionSet = getOptionSet(optionSetId).orElseThrow();

            options = new ArrayList<>(optionSet.getOptions());
        }

        return options;
    }

    @Override
    @Transactional
    public void updateOption(Option option) {
        optionRepository.update(option);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Option> getOption(String id) {
        return optionRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> validateAndMapOptionCodes(Collection<String> optionCodes, String optionSetId) throws InvalidOptionCodesException {
        if (optionCodes == null || optionCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        // Normalize codes: trim.
        List<String> codesNormalized = optionCodes.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .toList();

        Set<String> uniqueCodes = new LinkedHashSet<>(codesNormalized);

        List<Option> found = optionRepository.findAllByOptionSetUid(optionSetId);


        Map<String, String> codeToId = found.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Option::getCode, Option::getId));

        List<String> missing = uniqueCodes.stream()
            .filter(code -> !codeToId.containsKey(code))
            .collect(Collectors.toList());

        if (!missing.isEmpty()) {
            throw new InvalidOptionCodesException(missing);
        }

        // Return mapping for requested codes (preserve original duplicates is caller responsibility)
        return codeToId;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Option> getOptionByCode(String code) {
        return optionRepository.findFirstByCode(code);
    }

    @Override
    public List<Option> getOptionsByCode(List<String> code) {
        return optionRepository.findAllByCodeIn(code);
    }

    @Override
    @Transactional
    public void deleteOption(Option option) {
        optionRepository.delete(option);
    }

    // -------------------------------------------------------------------------
    // OptionGroup
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public OptionGroup saveOptionGroup(OptionGroup group) {
        return optionGroupRepository.save(group);
    }

    @Override
    @Transactional
    public void updateOptionGroup(OptionGroup group) {
        optionGroupRepository.update(group);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OptionGroup> getOptionGroup(String id) {
        return optionGroupRepository.findById(id);
    }

    @Override
    @Transactional
    public void deleteOptionGroup(OptionGroup group) {
        optionGroupRepository.delete(group);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OptionGroup> getAllOptionGroups() {
        return optionGroupRepository.findAll();
    }

    // -------------------------------------------------------------------------
    // OptionGroupSet
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public OptionGroupSet saveOptionGroupSet(OptionGroupSet group) {
        return optionGroupSetRepository.save(group);
    }

    @Override
    @Transactional
    public void updateOptionGroupSet(OptionGroupSet group) {
        optionGroupSetRepository.update(group);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OptionGroupSet> getOptionGroupSet(String id) {
        return optionGroupSetRepository.findById(id);
    }


    @Override
    @Transactional
    public void deleteOptionGroupSet(OptionGroupSet group) {
        optionGroupSetRepository.delete(group);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OptionGroupSet> getAllOptionGroupSets() {
        return optionGroupSetRepository.findAll();
    }
}
