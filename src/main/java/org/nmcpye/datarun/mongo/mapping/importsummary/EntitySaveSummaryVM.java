package org.nmcpye.datarun.mongo.mapping.importsummary;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntitySaveSummaryVM implements Serializable {
    public Map<String, String> getFailed() {
        return failed;
    }

    public void setFailed(Map<String, String> failed) {
        this.failed = failed;
    }

    public List<String> getUpdated() {
        return updated;
    }

    public void setUpdated(List<String> updated) {
        this.updated = updated;
    }

    public List<String> getCreated() {
        return created;
    }

    public void setCreated(List<String> created) {
        this.created = created;
    }

    private List<String> created = new ArrayList<>();
    private List<String> updated = new ArrayList<>();
    private Map<String, String> failed = new HashMap<>();
}
