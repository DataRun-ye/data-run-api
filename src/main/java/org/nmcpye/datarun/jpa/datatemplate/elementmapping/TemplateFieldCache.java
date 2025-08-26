package org.nmcpye.datarun.jpa.datatemplate.elementmapping;

import org.nmcpye.datarun.jpa.datatemplate.ElementTemplateConfig;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple in-memory cache for template fields keyed by templateId + ":" + versionId.
 * to do replace with a Redis-backed cache later.
 *
 * @author Hamza Assada 18/08/2025 (7amza.it@gmail.com)
 */
public class TemplateFieldCache {
    private final ConcurrentMap<String, List<ElementTemplateConfig>> map = new ConcurrentHashMap<>();

    private String key(String templateId, String versionId) {
        return templateId + ":" + versionId;
    }

    public void put(String templateId, String versionId, List<ElementTemplateConfig> fields) {
        Objects.requireNonNull(templateId);
        Objects.requireNonNull(versionId);
        Objects.requireNonNull(fields);
        map.put(key(templateId, versionId), fields);
    }

    public List<ElementTemplateConfig> get(String templateId, String versionId) {
        return map.get(key(templateId, versionId));
    }

    public void invalidate(String templateId, String versionId) {
        map.remove(key(templateId, versionId));
    }
}
