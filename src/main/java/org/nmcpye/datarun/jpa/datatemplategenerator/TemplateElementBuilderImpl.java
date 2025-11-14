package org.nmcpye.datarun.jpa.datatemplategenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.jpa.datatemplate.DataType;
import org.nmcpye.datarun.jpa.datatemplate.SemanticType;
import org.nmcpye.datarun.jpa.datatemplate.TemplateElement;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.UUID;

/**
 * Builds TemplateElement instances. Keeps responsibility small: mapping / normalization.
 * Persistence is delegated to Publisher.
 *
 * @author Hamza Assada
 * @since 09/09/2025
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateElementBuilderImpl implements TemplateElementBuilder {

    @Override
    public TemplateElement buildTemplateElementFromField(FormDataElementConf f,
                                                         PathMetadata meta,
                                                         TemplateVersion templateVersion) {
        Objects.requireNonNull(f);
        Objects.requireNonNull(meta);
        Objects.requireNonNull(templateVersion);

        final CanonicalKeys canonicalKeys = CanonicalKeys
            .builder().templateUid(templateVersion.getTemplateUid())
            .canonicalPath(meta.getCanonicalPath())
            .dataType(DataType.fromValueType(f.getType()))
            .semanticType(SemanticType.fromValueType(f.getType()))
            .optionSetUid(f.getOptionSet())
            .cardinality(Boolean.TRUE.equals(meta.getHasParentRepeat()) || f.getType().isMultiSelect() ? "N" : "1")
            .build();

        final var canonicalElementUid = canonicalUidFromStringAsUuid(canonicalKeys(canonicalKeys));

        final var h = hashToLong(String.join("|",
            templateVersion.getUid(),
            meta.getJsonDataPath(),
            canonicalKeys(canonicalKeys)));
        final var schemaFingerprint = "E_" + Long.toUnsignedString(h, 36);

        //        String parentCanonicalElement;
//        if(meta.getHasParentRepeat()) {
//            final CanonicalKeys parentCanonicalKeys = CanonicalKeys
//                .builder().templateUid(templateVersion.getTemplateUid())
//                .canonicalPath(meta.getCanonicalParentRepeatPath())
//                .dataType(DataType.ARRAY)
//                .semanticType(SemanticType.Repeat)
//                .cardinality(Boolean.TRUE.equals(meta.getHasParentRepeat()) || f.getType().isMultiSelect() ? "N" : "1")
//                .build();
//
//            parentCanonicalElement = canonicalUidFromStringAsUuid(canonicalKeys(parentCanonicalKeys));
//        }

        var cfg = TemplateElement.builder()
            .uid(CodeGenerator.generateUid())
            .canonicalElementUid(canonicalElementUid)
            .schemaFingerprint(schemaFingerprint)
            .templateUid(templateVersion.getTemplateUid())
            .templateVersionUid(templateVersion.getUid())
            .versionNo(templateVersion.getVersionNumber())
            .elementKind(TemplateElement.ElementKind.FIELD)
            .dataElementUid(f.getId())
            .name(f.getName())
            .idPath(meta.getJsonDataIdPath())
            .jsonDataPath(meta.getJsonDataPath())
            .canonicalPath(meta.getCanonicalPath())
            .cardinality(Boolean.TRUE.equals(meta.getHasParentRepeat()) || f.getType().isMultiSelect() ? "N" : "1")
            .dataType(DataType.fromValueType(f.getType()))
            .semanticType(SemanticType.fromValueType(f.getType()))
            .parentRepeatJsonDataPath(meta.getParentRepeatIdPath())
            .parentRepeatCanonicalPath(meta.getCanonicalParentRepeatPath())
            .sortOrder(f.getOrder())
            .valueType(f.getType())
            .optionSetUid(f.getOptionSet())
            .displayLabel(f.getLabel());
        return cfg.build();
    }

    @Override
    public TemplateElement buildTemplateElementFromRepeat(FormSectionConf section,
                                                          PathMetadata meta,
                                                          TemplateVersion templateVersion) {
        Objects.requireNonNull(section);
        Objects.requireNonNull(meta);
        Objects.requireNonNull(templateVersion);

        var cfg = TemplateElement.builder()
            .templateUid(templateVersion.getTemplateUid())
            .templateVersionUid(templateVersion.getUid())
            .versionNo(templateVersion.getVersionNumber())
            .elementKind(TemplateElement.ElementKind.REPEAT);

        final CanonicalKeys canonicalKeys = CanonicalKeys
            .builder().templateUid(templateVersion.getTemplateUid())
            .canonicalPath(meta.getCanonicalPath())
            .dataType(DataType.ARRAY)
            .semanticType(SemanticType.Repeat)
            .cardinality("N")
            .build();

        final var canonicalElementUid = canonicalUidFromStringAsUuid(canonicalKeys(canonicalKeys));

        final var h = hashToLong(String.join("|",
            templateVersion.getUid(),
            meta.getJsonDataPath(),
            canonicalKeys(canonicalKeys)));
        // synthetic dataElementUid for repeat grain
        final var schemaFingerprint = "RPT_" + Math.abs(h);

        cfg.canonicalElementUid(canonicalElementUid)
            .uid(CodeGenerator.generateUid())
            .dataElementUid(schemaFingerprint)
            .schemaFingerprint(schemaFingerprint)
            .idPath(meta.getJsonDataIdPath())
            .jsonDataPath(meta.getJsonDataPath())
            .name(section.getName())
            .semanticType(SemanticType.Repeat)
            .dataType(DataType.ARRAY)
            .canonicalPath(meta.getCanonicalPath())
            .cardinality("N")
            .parentRepeatJsonDataPath(meta.getParentRepeatIdPath())
            .parentRepeatCanonicalPath(meta.getCanonicalParentRepeatPath())
            .displayLabel(section.getLabel())
            .sortOrder(section.getOrder());
        if (section.getCategoryId() != null) {
            cfg.naturalKeyCandidate(section.getCategoryId());
        }
        return cfg.build();
    }

    // ---------- helper methods (small) ------------

    public static String canonicalKeys(CanonicalKeys canonicalKeys) {
        return String.join("|", canonicalKeys.templateUid(),
            canonicalKeys.canonicalPath(),
            canonicalKeys.dataType() == null ? "" : canonicalKeys.dataType().name(),
            canonicalKeys.semanticType() == null ? "" : canonicalKeys.semanticType().name(),
            canonicalKeys.optionSetUid() == null ? "" : canonicalKeys.optionSetUid(),
            canonicalKeys.cardinality());
    }

    public static String canonicalUidFromStringAsUuid(String key) {
        // Deterministic name-based UUID (same input -> same UUID)
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public static long hashToLong(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.wrap(digest, 0, Long.BYTES);
            return buf.getLong();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute hash", e);
        }
    }
}
