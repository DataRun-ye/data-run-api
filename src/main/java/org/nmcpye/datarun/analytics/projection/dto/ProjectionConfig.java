package org.nmcpye.datarun.analytics.projection.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ProjectionConfig {
    public String id; // e.g. "proj_supply_v1"
    public String uid; // e.g. "proj_supply_v1"
    public String targetType;
    public Source source;
    public Target target;
    public Dedupe dedupe;
    public Provenance provenance;
    public Map<String, Object> meta;       // free-form metadata (optional)

    @Getter
    @Setter
    public static class Source {
        public String repeatUid;          // required: canonical repeat id e.g. RPT_aaf9...
        public String filterExpression;   // optional (e.g. "payload->>'received_quantity' IS NOT NULL")
    }

    @Getter
    @Setter
    public static class Target {
        public String table;              // target table name
        public String mode;               // "append" or "upsert"
        public String naturalKeyExpr;     // expression to build deterministic key (string expression referring to payload, submission_uid, occurrence_index)
        public Map<String, Mapping> mappings; // target_column -> mapping rule
        public List<PostTransform> postTransform; // optional steps to cast/set computed values
    }

    @Getter
    @Setter
    public static class Mapping {
        public String expr;               // expression to evaluate (e.g. "payload->>'amd'" or "payload->>'_id'")
        public String type;               // optional hint: "string", "int", "date", "option", "jsonb"
        public String optionSetUid;       // if type == "option", optionSetUid for resolving labels/uid (optional)
        public Boolean explode;           // for select-multi mapping: explode boolean -> write separate exploded rows if true (worker handles)
    }

    @Getter
    @Setter
    public static class PostTransform {
        public String op;                 // e.g. "cast", "set", "sha256"
        public Map<String, String> args;   // parameters for op (e.g. cast: {"field":"supply_date","to":"date"})
    }

    @Getter
    @Setter
    public static class Dedupe {
        public String strategy;           // "natural_key" | "payload_checksum" | "none"
        public String conflict;           // "update" | "ignore"
    }

    @Getter
    @Setter
    public static class Provenance {
        public boolean copySubmissionUid = true;
        public boolean copyTemplateVersion = true;
        public boolean copyRawPayload = false; // if true, store raw payload json
    }
}
