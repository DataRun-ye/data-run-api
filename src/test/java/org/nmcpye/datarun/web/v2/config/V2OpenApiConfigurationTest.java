package org.nmcpye.datarun.web.v2.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Response enumerations are published as open lists; request enumerations stay closed. */
class V2OpenApiConfigurationTest {

    private static StringSchema enumeration(String... values) {
        StringSchema schema = new StringSchema();
        schema.setEnum(List.of(values));
        return schema;
    }

    private static Schema<?> ref(String name) {
        return new Schema<>().$ref("#/components/schemas/" + name);
    }

    @Test
    void responseOnlyEnumsBecomeExtensibleAndSharedOnesSaySo() {
        StringSchema inline = enumeration("FIRST", "SECOND");
        ObjectSchema thing = new ObjectSchema();
        thing.addProperty("state", ref("State"));
        thing.addProperty("kind", ref("Shared"));
        thing.addProperty("phase", inline);
        ObjectSchema change = new ObjectSchema();
        change.addProperty("kind", ref("Kind"));
        change.addProperty("shared", ref("Shared"));

        OpenAPI api = new OpenAPI().components(new Components()
                .addSchemas("State", enumeration("OPEN", "DONE"))
                .addSchemas("Kind", enumeration("SOFT", "HARD"))
                .addSchemas("Shared", enumeration("ONE", "TWO"))
                .addSchemas("Thing", thing)
                .addSchemas("Change", change))
            .paths(new Paths().addPathItem("/api/v2/things/{id}:touch", new PathItem().post(new Operation()
                .requestBody(new RequestBody().content(new Content().addMediaType("application/json",
                    new MediaType().schema(ref("Change")))))
                .responses(new ApiResponses().addApiResponse("200", new ApiResponse().content(new Content()
                    .addMediaType("application/json", new MediaType().schema(ref("Thing")))))))));

        V2OpenApiConfiguration.markResponseEnumsOpen(api);

        Schema<?> state = api.getComponents().getSchemas().get("State");
        assertThat(state.getEnum()).isNull();
        assertThat(state.getExtensions()).containsEntry("x-extensible-enum", List.of("OPEN", "DONE"));
        assertThat(inline.getEnum()).isNull();
        assertThat(inline.getExtensions()).containsEntry("x-extensible-enum", List.of("FIRST", "SECOND"));

        Schema<?> kind = api.getComponents().getSchemas().get("Kind");
        assertThat(kind.getEnum()).as("a request's list stays closed").isEqualTo(List.of("SOFT", "HARD"));
        assertThat(kind.getExtensions()).isNull();

        Schema<?> shared = api.getComponents().getSchemas().get("Shared");
        assertThat(shared.getEnum()).isEqualTo(List.of("ONE", "TWO"));
        assertThat(shared.getDescription()).isEqualTo(V2OpenApiConfiguration.OPEN_IN_RESPONSES);

        V2OpenApiConfiguration.markResponseEnumsOpen(api);
        assertThat(shared.getDescription()).as("said once").isEqualTo(V2OpenApiConfiguration.OPEN_IN_RESPONSES);
    }
}
