package org.nmcpye.datarun.jpa.datasubmissionbatching.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersionContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Which documents the legacy route would give row ids, asked without minting any. */
class MigrationRepeatIdGeneratorTest {

    private final ObjectMapper json = new ObjectMapper();
    private final MigrationRepeatIdGenerator generator = new MigrationRepeatIdGenerator(context());

    private static TemplateVersionContext context() {
        FormSectionConf rows = new FormSectionConf().path("main.rows").name("rows");
        rows.setRepeatable(true);
        FormSectionConf subrows = new FormSectionConf().path("main.rows.subrows").name("subrows");
        subrows.setRepeatable(true);
        FormSectionConf main = new FormSectionConf().path("main").name("main");
        TemplateVersionContext context = mock(TemplateVersionContext.class);
        when(context.getElementsByPath())
            .thenReturn(Map.of("main", main, "main.rows", rows, "main.rows.subrows", subrows));
        return context;
    }

    private JsonNode document(String text) throws Exception {
        return json.readTree(text);
    }

    @Test
    void aRowWithoutAnIdAnywhereInARepeatableSectionIsFound() throws Exception {
        assertThat(generator.hasRowsWithoutIds(document("""
            {"main": {"rows": [{"item": "r1", "subrows": [{"sub": "s1"}]}]}}"""))).as("apps 5.2 and 5.3").isTrue();
        assertThat(generator.hasRowsWithoutIds(document("""
            {"main": {"rows": [{"_id": "row1", "item": "r1", "subrows": [{"sub": "s1"}]}]}}""")))
            .as("a nested row without one").isTrue();
        assertThat(generator.hasRowsWithoutIds(document("""
            {"main": {"rows": [{"_id": "row1", "item": "r1", "subrows": [{"_id": "row2", "sub": "s1"}]}]}}""")))
            .as("apps 6.0 and later").isFalse();
        assertThat(generator.hasRowsWithoutIds(document("""
            {"main": {"rows": [{"_uid": "row1", "item": "r1"}]}}"""))).as("an id in _uid counts").isFalse();
    }

    /** Lists the form version does not name as repeatable sections are not rows: the route mints nothing there. */
    @Test
    void onlyRepeatableSectionsHoldRows() throws Exception {
        assertThat(generator.hasRowsWithoutIds(document("""
            {"main": {"name": "first", "extras": [{"k": "v"}]}}"""))).isFalse();
        assertThat(generator.hasRowsWithoutIds(document("{}"))).isFalse();
        assertThat(generator.hasRowsWithoutIds(null)).isFalse();
        assertThat(generator.hasRowsWithoutIds(json.nullNode())).isFalse();
    }

    @Test
    void askingChangesNothing() throws Exception {
        JsonNode copy = document("""
            {"main": {"rows": [{"item": "r1", "subrows": [{"sub": "s1"}]}]}}""");
        String before = copy.toString();

        generator.hasRowsWithoutIds(copy);

        assertThat(copy).hasToString(before);
    }
}
