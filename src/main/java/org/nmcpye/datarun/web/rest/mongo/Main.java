package org.nmcpye.datarun.web.rest.mongo;

import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // Sample input
        Map<String, Object> formData = new HashMap<>();
        formData.put("mainSection", Map.of(
            "workDate", "2024-09-19T00:00:00.000",
            "workDay", "1"
        ));

        formData.put("teamWorkSummary", new Object[]{
            Map.of(
                "teamid", 13,
                "districts", "Pw7O7DL5xn9",
                "temporarilyEliminatedBreedingSources", 30,
                "undisposedSources", Map.of(
                    "cumulativeNumberOfVillagesCovered", 40,
                    "cumulativeNumberOfRemainingVillages", 160,
                    "CumulativeCompletionRate", 20
                ),
                "TotalTiresCollectedByTeam", 15
            )
        });

        // Flatten the formData
//        Map<String, Object> flattenedFormData = JsonFlattener.flatten(formData);
        Map<String, Object> flattenedFormData = JsonFlattener.flattenIncludingArrays(formData);

        // Print the flattened result
        flattenedFormData.forEach((key, value) -> {
            System.out.println(key + " : " + value);
        });
    }
}
