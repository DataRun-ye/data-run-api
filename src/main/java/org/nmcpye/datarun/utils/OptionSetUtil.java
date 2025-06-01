package org.nmcpye.datarun.utils;

import org.nmcpye.datarun.datatemplateelement.DataOption;
import org.nmcpye.datarun.optionset.OptionSet;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class OptionSetUtil {

    public static Map<String, List<DataOption>> createOptionMap(List<DataOption> options) {
        // Group options by listName
        return options.stream()
            .collect(Collectors.groupingBy(DataOption::getListName));
    }

    public static List<OptionSet> createOptionSets(List<DataOption> options) {
        // Group options by listName
        Map<String, List<DataOption>> groupedOptions = createOptionMap(options);

        // Create OptionSet objects from the grouped options
        List<OptionSet> optionSets = new ArrayList<>();
        for (Map.Entry<String, List<DataOption>> entry : groupedOptions.entrySet()) {
            OptionSet optionSet = new OptionSet();
            optionSet.setName(entry.getKey());
            optionSet.setOptions(entry.getValue());
            optionSets.add(optionSet);
        }

        return optionSets;
    }

    public static void main(String[] args) {
        // Example list of options
        List<DataOption> options = Arrays.asList(
            new DataOption().listName("status").name("IN_PROGRESS"),
            new DataOption().listName("status").name("DONE"),
            new DataOption().listName("status").name("CANCELLED"),
            new DataOption().listName("workDays").name("2"),
            new DataOption().listName("workDays").name("3"),
            new DataOption().listName("workDays").name("1")
        );

        // Create OptionSets
        List<OptionSet> optionSets = createOptionSets(options);

        // Print OptionSets for verification
        for (OptionSet optionSet : optionSets) {
            System.out.println("OptionSet Name: " + optionSet.getName());
            for (DataOption option : optionSet.getOptions()) {
                System.out.println(" - " + option.getName());
            }
        }

        List<String> keys = Arrays.asList("key1", "key2", "key3");
        List<String> values = Arrays.asList("value1", "value2", "value3");

        Map<String, String> result = IntStream.range(0, keys.size())
            .boxed()
            .collect(Collectors.toMap(keys::get, values::get));

        result.forEach((key, value) -> System.out.println(key + ": " + value));

        // Merging a List of Maps into a Single Map
        System.out.println("\n-----------------");
        System.out.println("Merging a List of Maps into a Single Map");
        List<Map<String, String>> listOfMaps = Arrays.asList(
            Map.of("key1", "value1", "key2", "value2"),
            Map.of("key2", "value3", "key3", "value4")
        );

        Map<String, String> mergedMap = listOfMaps.stream()
            .flatMap(map -> map.entrySet().stream())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (existing, replacement) -> existing + ", " + replacement
            ));

        mergedMap.forEach((key, value) -> System.out.println(key + ": " + value));

        // Summing Values for Duplicate Keys
        System.out.println("\n-----------------");
        System.out.println("Summing Values for Duplicate Keys");
        Map<String, Integer> map1 = Map.of("key1", 1, "key2", 2);
        Map<String, Integer> map2 = Map.of("key2", 3, "key3", 4);

        Map<String, Integer> mergedMap2 = Stream.of(map1, map2)
            .map(Map::entrySet)
            .flatMap(Set::stream)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                Integer::sum
            ));

        mergedMap2.forEach((key, value) -> System.out.println(key + ": " + value));

        // 5. Merging Nested Structures
        System.out.println("\n-----------------");
        System.out.println("Merging Nested Structures");
        Map<String, List<String>> map3 = Map.of("key1", List.of("a", "b"), "key2", List.of("c"));
        Map<String, List<String>> map4 = Map.of("key1", List.of("d"), "key3", List.of("e"));

        Map<String, List<String>> mergedMapNested = Stream.of(map3, map4)
            .flatMap(map -> map.entrySet().stream())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (list1, list2) -> Stream.concat(list1.stream(), list2.stream()).collect(Collectors.toList())
            ));

        mergedMapNested.forEach((key, value) -> System.out.println(key + ": " + value));
    }
}
