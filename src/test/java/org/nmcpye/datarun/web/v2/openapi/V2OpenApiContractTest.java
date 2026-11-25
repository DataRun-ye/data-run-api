package org.nmcpye.datarun.web.v2.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.web.v2.config.V2OpenApiConfiguration;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSecurityConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The v2 OpenAPI check, as a unit test so the release gate ({@code ./mvnw test} in {@code scripts/release/verify.sh})
 * runs it; the repository has no CI. It generates the v2 document from the v2 controllers alone, in a web slice
 * without a database, and:
 * <ol>
 *   <li>checks the conventions that show in the document ({@link V2Conventions});</li>
 *   <li>compares it with the published contract, {@code docs/api/openapi-v2.json} ({@link OpenApiCompatibility}):
 *       a breaking change fails; any other difference fails too until the snapshot is updated, so the snapshot
 *       always is the contract.</li>
 * </ol>
 * To publish an additive change: {@code ./mvnw test -Dtest=V2OpenApiContractTest -Dopenapi.v2.update=true}, then
 * commit the snapshot with the change. A breaking change also needs {@code -Dopenapi.v2.acceptBreaking=true}: v2 takes
 * one only by a reviewed decision. A controller with collaborators adds them here as {@code @MockitoBean}s; the
 * full application serves the same document ({@code V2OpenApiLiveIT}).
 */
@WebMvcTest(useDefaultFilters = false,
    includeFilters = @ComponentScan.Filter(type = FilterType.CUSTOM, classes = V2OpenApiContractTest.V2Routes.class))
@Import(V2OpenApiConfiguration.class)
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocConfigProperties.class,
    SpringDocWebMvcConfiguration.class, MultipleOpenApiSupportConfiguration.class,
    SpringDocSecurityConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "springdoc.api-docs.enabled=true")
class V2OpenApiContractTest {

    /** The routes of the main code under web.v2: its {@code @RestController}s, never the test fixtures. */
    static class V2Routes implements TypeFilter {
        @Override
        public boolean match(MetadataReader reader, MetadataReaderFactory factory) {
            String type = reader.getClassMetadata().getClassName();
            return type.startsWith("org.nmcpye.datarun.web.v2.")
                && !type.startsWith("org.nmcpye.datarun.web.v2.fixtures.")
                && reader.getAnnotationMetadata().hasAnnotation(RestController.class.getName());
        }
    }

    @Autowired
    private MockMvc mvc;

    private JsonNode generated() throws Exception {
        String json = mvc.perform(get(V2OpenApiConfiguration.DOCS_PATH)).andReturn().getResponse().getContentAsString();
        return OpenApiDocuments.read(json);
    }

    @Test
    void theDocumentKeepsTheV2Conventions() throws Exception {
        JsonNode document = generated();
        assertThat(V2Conventions.violations(document)).isEmpty();
        assertThat(document.path("components").path("schemas").path("Id").path("maxLength").asInt()).isEqualTo(36);
        assertThat(document.path("components").path("schemas").path("Id").has("format")).isFalse();
    }

    @Test
    void theDocumentIsThePublishedContract() throws Exception {
        JsonNode current = generated();
        JsonNode published = OpenApiDocuments.readSnapshot();
        boolean update = Boolean.getBoolean(OpenApiDocuments.UPDATE);
        boolean acceptBreaking = Boolean.getBoolean(OpenApiDocuments.ACCEPT_BREAKING);
        if (published == null) {
            if (!update) {
                fail("No published contract at " + OpenApiDocuments.SNAPSHOT + "; run with -D"
                    + OpenApiDocuments.UPDATE + "=true to write it");
            }
            OpenApiDocuments.writeSnapshot(current);
            return;
        }
        List<OpenApiCompatibility.Change> changes = OpenApiCompatibility.compare(published, current);
        String report = changes.stream().map(OpenApiCompatibility.Change::toString).collect(Collectors.joining("\n"));
        if (changes.stream().anyMatch(OpenApiCompatibility.Change::breaking) && !(update && acceptBreaking)) {
            fail("Breaking changes to the published v2 contract; v2 changes additively only:\n" + report);
        }
        if (!published.equals(current)) {
            if (!update) {
                fail("The v2 document differs from " + OpenApiDocuments.SNAPSHOT + ". If intended, publish it with -D"
                    + OpenApiDocuments.UPDATE + "=true and commit the snapshot:\n" + report);
            }
            OpenApiDocuments.writeSnapshot(current);
        }
    }

    /** The check fails on a deliberate breaking change to what the published contract holds. */
    @Test
    void aDeliberateBreakingChangeToThePublishedContractFails() throws Exception {
        JsonNode published = OpenApiDocuments.readSnapshot();
        assertThat(published).as("the published contract").isNotNull();
        JsonNode before = withOperation(published);
        ObjectNode after = (ObjectNode) withOperation(published);
        ((ObjectNode) after.path("components").path("schemas").path("Problem").path("properties")).remove("code");

        List<OpenApiCompatibility.Change> changes = OpenApiCompatibility.compare(before, after);

        assertThat(changes).filteredOn(OpenApiCompatibility.Change::breaking).extracting(Object::toString)
            .anyMatch(change -> change.contains("Problem") || change.contains(".code"));

        ObjectNode removed = (ObjectNode) withOperation(published);
        removed.remove("paths");
        assertThat(OpenApiCompatibility.compare(before, removed)).filteredOn(OpenApiCompatibility.Change::breaking)
            .extracting(Object::toString).containsExactly("BREAKING GET /api/v2/probe: operation removed");
    }

    /** The contract plus one operation answering problems, so the published components are in use. */
    private static JsonNode withOperation(JsonNode published) throws Exception {
        ObjectNode copy = published.deepCopy();
        ObjectNode operation = copy.putObject("paths").putObject("/api/v2/probe").putObject("get");
        operation.putObject("responses").putObject("default").put("$ref", "#/components/responses/Problem");
        return OpenApiDocuments.normal(copy);
    }
}
