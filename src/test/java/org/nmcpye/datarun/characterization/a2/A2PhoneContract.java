package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.databind.JsonNode;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Group;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * What an installed phone needs from a 2xx upload response (A2 inventory, section 2.4).
 * <ul>
 *   <li>M1, G1 to G3 (m@v6.1.2 lib/core/sync/sync_summary_model.dart:20-63; data_submissions_dao.dart:46-82): a
 *       JSON object; created and updated absent or lists of strings; failed absent or an object with string keys;
 *       every attempted uid in one of the three, or the row is failed as a bad response.</li>
 *   <li>M0, G0 (m@v5.3.0+20 packages/drun_sdk/lib/database/dao/data_submissions_dao.dart:84-107,743-757): M1, and
 *       every failed value a non-null string, else the whole batch is failed; an attempted uid that is missing
 *       leaves its row in "uploading" for good.</li>
 * </ul>
 */
final class A2PhoneContract {

    private A2PhoneContract() {
    }

    static List<String> violations(Group group, JsonNode requestBody, int status, JsonNode body) {
        List<String> problems = new ArrayList<>();
        if (status >= 400) {
            return problems; // the phone fails the whole batch; nothing more to check
        }
        if (body == null || !body.isObject()) {
            problems.add("the body is not a JSON object");
            return problems;
        }
        Set<String> answered = new HashSet<>();
        for (String list : List.of("created", "updated")) {
            JsonNode ids = body.get(list);
            if (ids == null || ids.isNull()) {
                continue;
            }
            if (!ids.isArray()) {
                problems.add(list + " is not a list");
                continue;
            }
            ids.forEach(id -> {
                if (!id.isTextual()) {
                    problems.add(list + " holds a non-string " + id);
                } else {
                    answered.add(id.asText());
                }
            });
        }
        JsonNode failed = body.get("failed");
        if (failed != null && !failed.isNull()) {
            if (!failed.isObject()) {
                problems.add("failed is not an object");
            } else {
                Iterator<String> keys = failed.fieldNames();
                while (keys.hasNext()) {
                    String key = keys.next();
                    answered.add(key);
                    JsonNode value = failed.get(key);
                    if (group == Group.G0 && (value == null || !value.isTextual())) {
                        problems.add("G0 needs a string failure for " + key + ", got " + value);
                    }
                }
            }
        }
        if (requestBody != null && requestBody.isArray()) {
            for (JsonNode item : requestBody) {
                JsonNode uid = item.get("uid");
                if (uid != null && uid.isTextual() && !answered.contains(uid.asText())) {
                    problems.add("attempted uid " + uid.asText() + " is missing from the response");
                }
            }
        }
        return problems;
    }
}
