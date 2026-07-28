package org.nmcpye.datarun.assignmentshadow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nmcpye.datarun.common.enumeration.FormPermission;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.userdetail.UserFormAccess;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class CanonicalCaptureFormResolver {

    private final ObjectMapper objectMapper;

    public CanonicalCaptureFormResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<String> resolve(String assignmentForms, String formPermissions) {
        JsonNode assigned = readRequiredArray(assignmentForms, "assignment.forms");
        Set<String> assignedFormUids = new LinkedHashSet<>();
        for (JsonNode formUid : assigned) {
            assignedFormUids.add(requiredUidText(formUid, "assignment.forms entry"));
        }

        JsonNode permissionEntries = readRequiredArray(
            formPermissions,
            "team.form_permissions"
        );
        Set<String> capturePermissionFormUids = new LinkedHashSet<>();
        for (JsonNode permissionEntry : permissionEntries) {
            if (!permissionEntry.isObject()) {
                throw new IllegalArgumentException(
                    "team.form_permissions entry is not an object"
                );
            }
            String formUid = requiredUidText(
                permissionEntry.get("form"),
                "team permission form"
            );
            JsonNode permissions = permissionEntry.get("permissions");
            if (permissions == null || !permissions.isArray()) {
                throw new IllegalArgumentException(
                    "team permission permissions is not an array"
                );
            }
            boolean grantsCapture = false;
            for (JsonNode permission : permissions) {
                if (!permission.isTextual()) {
                    throw new IllegalArgumentException("team permission value is not text");
                }
                FormPermission parsed;
                try {
                    parsed = FormPermission.valueOf(permission.textValue());
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException(
                        "unknown team form permission " + permission.textValue(),
                        exception
                    );
                }
                grantsCapture = grantsCapture
                    || parsed == FormPermission.ADD_SUBMISSIONS
                    || parsed == FormPermission.EDIT_SUBMISSIONS;
            }
            if (grantsCapture) {
                capturePermissionFormUids.add(formUid);
            }
        }

        return assignedFormUids.stream()
            .filter(capturePermissionFormUids::contains)
            .sorted()
            .toList();
    }

    public List<String> readCanonicalFormUidList(String value, String fieldName) {
        JsonNode formUids = readRequiredArray(value, fieldName);
        LinkedHashSet<String> values = new LinkedHashSet<>();
        formUids.forEach(formUid -> values.add(requiredUidText(formUid, fieldName + " entry")));
        return values.stream().sorted().toList();
    }

    public List<String> resolveForActor(
        Collection<String> assignmentForms,
        Collection<UserFormAccess> formAccess,
        String teamUid,
        Instant now
    ) {
        LinkedHashSet<String> assignedFormUids = new LinkedHashSet<>();
        if (assignmentForms == null) {
            throw new IllegalArgumentException("assignment.forms is null");
        }
        assignmentForms.forEach(formUid ->
            assignedFormUids.add(requiredUid(formUid, "assignment.forms entry"))
        );

        LinkedHashSet<String> capturePermissionFormUids = new LinkedHashSet<>();
        if (formAccess != null) {
            formAccess.stream()
                .filter(access -> Objects.equals(access.getTeam(), teamUid))
                .filter(access ->
                    (access.getValidFrom() == null
                        || access.getValidFrom().isBefore(now))
                        && (access.getValidTo() == null
                        || access.getValidTo().isAfter(now)))
                .forEach(access -> {
                    String formUid = requiredUid(
                        access.getForm(),
                        "team permission form"
                    );
                    if (access.getPermissions() == null) {
                        throw new IllegalArgumentException(
                            "team permission permissions is null"
                        );
                    }
                    if (access.getPermissions().contains(FormPermission.ADD_SUBMISSIONS)
                        || access.getPermissions().contains(FormPermission.EDIT_SUBMISSIONS)) {
                        capturePermissionFormUids.add(formUid);
                    }
                });
        }

        return assignedFormUids.stream()
            .filter(capturePermissionFormUids::contains)
            .sorted()
            .toList();
    }

    private JsonNode readRequiredArray(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is null");
        }
        try {
            JsonNode parsed = objectMapper.readTree(value);
            if (parsed == null || !parsed.isArray()) {
                throw new IllegalArgumentException(fieldName + " is not an array");
            }
            return parsed;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(fieldName + " is malformed", exception);
        }
    }

    private String requiredUidText(JsonNode value, String fieldName) {
        if (value == null || !value.isTextual() || !CodeGenerator.isValidUid(value.textValue())) {
            throw new IllegalArgumentException(fieldName + " is not a valid UID");
        }
        return value.textValue();
    }

    private String requiredUid(String value, String fieldName) {
        if (!CodeGenerator.isValidUid(value)) {
            throw new IllegalArgumentException(fieldName + " is not a valid UID");
        }
        return value;
    }
}
