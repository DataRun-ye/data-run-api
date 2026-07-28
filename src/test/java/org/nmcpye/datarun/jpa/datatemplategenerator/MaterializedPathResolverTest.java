package org.nmcpye.datarun.jpa.datatemplategenerator;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MaterializedPathResolverTest {

    @Test
    void resolvesOnlyRepeatSectionsAboveTheCurrentSectionAsParents() {
        FormSectionConf households = repeat("households", "households");
        FormSectionConf members = repeat("members", "households.members");
        MaterializedPathResolver resolver = new MaterializedPathResolver(
            Map.of("households", households, "members", members));

        PathMetadata root = resolver.resolveForSection(households);
        PathMetadata nested = resolver.resolveForSection(members);

        assertNull(root.getParentRepeatIdPath());
        assertEquals("households", nested.getParentRepeatIdPath());
        assertEquals("households", nested.getCanonicalParentRepeatPath());
    }

    private static FormSectionConf repeat(String name, String path) {
        FormSectionConf section = new FormSectionConf();
        section.setName(name);
        section.setPath(path);
        section.setRepeatable(true);
        return section;
    }
}
