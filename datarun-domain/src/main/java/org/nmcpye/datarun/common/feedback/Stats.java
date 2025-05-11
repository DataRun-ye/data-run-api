package org.nmcpye.datarun.common.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class Stats {
    private int created;

    private int updated;

    private int deleted;

    private int ignored;

    public Stats() {
    }

    public void merge(Stats stats) {
        created += stats.getCreated();
        updated += stats.getUpdated();
        deleted += stats.getDeleted();
        ignored += stats.getIgnored();
    }

    @JsonProperty
    public int getTotal() {
        return created + updated + deleted + ignored;
    }

    @JsonProperty
    public int getCreated() {
        return created;
    }

    public void incCreated() {
        created++;
    }

    public void incCreated(int n) {
        created += n;
    }

    @JsonProperty
    public int getUpdated() {
        return updated;
    }

    public void incUpdated() {
        updated++;
    }

    public void incUpdated(int n) {
        updated += n;
    }

    @JsonProperty
    public int getDeleted() {
        return deleted;
    }

    public void incDeleted() {
        deleted++;
    }

    public void incDeleted(int n) {
        deleted += n;
    }

    @JsonProperty
    public int getIgnored() {
        return ignored;
    }

    public void incIgnored() {
        ignored++;
    }

    public void incIgnored(int n) {
        ignored += n;
    }

    public void ignored() {
        ignored += created;
        ignored += updated;
        ignored += deleted;

        created = 0;
        updated = 0;
        deleted = 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("created", created)
            .add("updated", updated)
            .add("deleted", deleted)
            .add("ignored", ignored)
            .toString();
    }
}
