package org.nmcpye.datarun.jpa.datatemplategenerator;

import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Produce a small, immutable snapshot that's easy to consume by the generator.
 *
 * @author Hamza Assada
 * @since 09/09/2025
 */
@Component
public class FlatTemplateProcessor {

    public static class TemplateFlatSnapshot {
        public final Map<String, FormSectionConf> sectionByName;
        public final List<FormDataElementConf> fields;

        public TemplateFlatSnapshot(Map<String, FormSectionConf> sectionByName,
                                    List<FormDataElementConf> fields) {
            this.sectionByName = Collections.unmodifiableMap(sectionByName);
            this.fields = Collections.unmodifiableList(fields);
        }
    }

    public TemplateFlatSnapshot process(TemplateVersion dtv) {
        Objects.requireNonNull(dtv, "DataTemplateVersion required");
        List<FormSectionConf> sections = Optional.ofNullable(dtv.getSections()).orElse(Collections.emptyList());
        Map<String, FormSectionConf> sectionByName = sections.stream()
            .filter(s -> s.getName() != null && !s.getName().isEmpty())
            .collect(Collectors.toMap(FormSectionConf::getName, s -> s, (a, b) -> a, LinkedHashMap::new));

        List<FormDataElementConf> fields = Optional.ofNullable(dtv.getFields()).orElse(Collections.emptyList());

        return new TemplateFlatSnapshot(sectionByName, fields);
    }
}
