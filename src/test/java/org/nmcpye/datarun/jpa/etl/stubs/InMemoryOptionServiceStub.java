package org.nmcpye.datarun.jpa.etl.stubs;

/**
 * @author Hamza Assada 14/08/2025 (7amza.it@gmail.com)
 */

import org.nmcpye.datarun.jpa.option.Option;
import org.nmcpye.datarun.jpa.option.OptionGroup;
import org.nmcpye.datarun.jpa.option.OptionGroupSet;
import org.nmcpye.datarun.jpa.option.OptionSet;
import org.nmcpye.datarun.jpa.option.exception.InvalidOptionCodesException;
import org.nmcpye.datarun.jpa.option.service.OptionService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Simple in-memory OptionService implementation for tests.
 * - registerOption(code, id) to seed available options
 * - validateAndMapOptionCodes returns code -> id map or throws InvalidOptionCodesException
 */
public class InMemoryOptionServiceStub implements OptionService {

    // code -> Option
    private final Map<String, Option> optionsByCode = new ConcurrentHashMap<>();

    public InMemoryOptionServiceStub() {
    }

    /**
     * helper: add an option available for tests
     */
    public void addOption(String code, String id) {
        Option o = new Option();
        // set minimal fields expected by codepaths (id, code)
        o.setId(id);
        o.setCode(code);
        optionsByCode.put(code, o);
    }

    /**
     * clear all seeded options
     */
    public void clear() {
        optionsByCode.clear();
    }

    @Override
    public Map<String, String> validateAndMapOptionCodes(Collection<String> optionCodes, String optionSetUid) throws InvalidOptionCodesException {
        if (optionCodes == null || optionCodes.isEmpty()) return Collections.emptyMap();
        // normalize codes (trim)
        List<String> normalized = optionCodes.stream().filter(Objects::nonNull).map(String::trim).collect(Collectors.toList());
        Set<String> unique = new LinkedHashSet<>(normalized);

        List<String> missing = unique.stream().filter(c -> !optionsByCode.containsKey(c)).collect(Collectors.toList());
        if (!missing.isEmpty()) {
            throw new InvalidOptionCodesException(missing);
        }

        Map<String, String> map = new LinkedHashMap<>();
        for (String c : normalized) {
            Option o = optionsByCode.get(c);
            map.put(c, o.getId());
        }
        return map;
    }

    // --- The rest of the OptionService methods are test/no-op implementations.
    // implement them to satisfy the interface; tests won't use them.

    @Override
    public OptionSet saveOptionSet(OptionSet optionSet) {
        throw new UnsupportedOperationException("test stub");
    }

    @Override
    public void updateOptionSet(OptionSet optionSet) {
        throw new UnsupportedOperationException("test stub");
    }

    @Override
    public Optional<OptionSet> getOptionSet(String id) {
        return Optional.empty();
    }

    @Override
    public Optional<OptionSet> getOptionSetByName(String name) {
        return Optional.empty();
    }

    @Override
    public Optional<OptionSet> getOptionSetByCode(String code) {
        return Optional.empty();
    }

    @Override
    public void deleteOptionSet(OptionSet optionSet) { /* no-op */ }

    @Override
    public List<OptionSet> getAllOptionSets() {
        return List.of();
    }

    @Override
    public List<Option> getOptions(String optionSetId, String name, Integer max) {
        return List.copyOf(optionsByCode.values());
    }

    @Override
    public void updateOption(Option option) { /* no-op */ }

    @Override
    public Optional<Option> getOption(String id) {
        // search by id
        return optionsByCode.values().stream().filter(o -> id != null && id.equals(o.getId())).findFirst();
    }

    @Override
    public Optional<Option> getOptionByCode(String code) {
        return Optional.ofNullable(optionsByCode.get(code));
    }

    @Override
    public List<Option> getOptionsByCode(List<String> code) {
        return code.stream().map(optionsByCode::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void deleteOption(Option option) {
        optionsByCode.remove(option.getCode());
    }

    @Override
    public OptionGroup saveOptionGroup(OptionGroup group) {
        throw new UnsupportedOperationException("test stub");
    }

    @Override
    public void updateOptionGroup(OptionGroup group) {
        throw new UnsupportedOperationException("test stub");
    }

    @Override
    public Optional<OptionGroup> getOptionGroup(String uid) {
        return Optional.empty();
    }

    @Override
    public void deleteOptionGroup(OptionGroup group) { /* no-op */ }

    @Override
    public List<OptionGroup> getAllOptionGroups() {
        return List.of();
    }

    @Override
    public OptionGroupSet saveOptionGroupSet(OptionGroupSet group) {
        throw new UnsupportedOperationException("test stub");
    }

    @Override
    public void updateOptionGroupSet(OptionGroupSet group) {
        throw new UnsupportedOperationException("test stub");
    }

    @Override
    public Optional<OptionGroupSet> getOptionGroupSet(String uid) {
        return Optional.empty();
    }

    @Override
    public void deleteOptionGroupSet(OptionGroupSet group) { /* no-op */ }

    @Override
    public List<OptionGroupSet> getAllOptionGroupSets() {
        return List.of();
    }
}
