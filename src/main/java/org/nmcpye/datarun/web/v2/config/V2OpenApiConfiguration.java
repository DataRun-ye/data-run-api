package org.nmcpye.datarun.web.v2.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.nmcpye.datarun.web.v2.ApiV2;
import org.nmcpye.datarun.web.v2.caller.ApiCaller;
import org.nmcpye.datarun.web.v2.concurrency.IfMatch;
import org.nmcpye.datarun.web.v2.idempotency.CommandRequest;
import org.nmcpye.datarun.web.v2.idempotency.CreateRequest;
import org.nmcpye.datarun.web.v2.idempotency.IdempotencyKey;
import org.nmcpye.datarun.web.v2.ids.ApiIds;
import org.nmcpye.datarun.web.v2.paging.CursorCodec;
import org.nmcpye.datarun.web.v2.paging.PageQuery;
import org.nmcpye.datarun.web.v2.problem.ProblemCode;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.List;

/**
 * The v2 OpenAPI document, group {@value #GROUP}: every route under {@code /api/v2}, served at {@value #DOCS_PATH}
 * (admin only, where the api-docs profile is on) and checked in as {@code docs/api/openapi-v2.json}, which a unit
 * test holds the code to. It documents the conventions themselves, so no module repeats them:
 * <ul>
 *   <li>the bearer JWT as the only security scheme, required everywhere;</li>
 *   <li>the {@code Id} schema, {@code Problem} and {@code ProblemError}, and a {@code default} response on every
 *       operation: any error is an RFC 9457 problem;</li>
 *   <li>for a command (a handler taking {@link CommandRequest}): the {@code Idempotency-Key} and {@code If-Match}
 *       headers and the 412 and 428 answers; for a create ({@link CreateRequest}), the key alone;</li>
 *   <li>for a list (a handler taking {@link PageQuery}): the {@code limit} and {@code cursor} parameters.</li>
 * </ul>
 */
@Configuration
public class V2OpenApiConfiguration {

    public static final String GROUP = "v2";
    public static final String DOCS_PATH = "/v3/api-docs/" + GROUP;

    static final String BEARER = "bearerAuth";
    static final String PROBLEM = "Problem";
    static final String PROBLEM_ERROR = "ProblemError";
    static final String PROBLEM_REF = "#/components/responses/" + PROBLEM;

    static {
        // Supplied by v2's argument resolvers, not read from the request as such; documented by conventions().
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(CommandRequest.class, CreateRequest.class,
            ApiCaller.class, PageQuery.class);
    }

    @Bean
    public GroupedOpenApi v2OpenApi() {
        return GroupedOpenApi.builder()
            .group(GROUP)
            .pathsToMatch(ApiV2.BASE + "/**")
            .addOperationCustomizer(V2OpenApiConfiguration::conventions)
            .addOpenApiCustomizer(V2OpenApiConfiguration::contract)
            .build();
    }

    static Operation conventions(Operation operation, HandlerMethod handler) {
        List<Class<?>> parameters = Arrays.asList(handler.getMethod().getParameterTypes());
        if (parameters.contains(CommandRequest.class) || parameters.contains(CreateRequest.class)) {
            operation.addParametersItem(new HeaderParameter().name(IdempotencyKey.HEADER).required(true)
                .description("A UUID chosen for this change and sent unchanged on every retry of it; quoted as a "
                    + "Structured Field string or bare. A retry with the same key and request replays the first "
                    + "outcome (Idempotent-Replayed: true); the same key with another request is 422.")
                .schema(new StringSchema().format("uuid")));
        }
        if (parameters.contains(CommandRequest.class)) {
            operation.addParametersItem(new HeaderParameter().name(IfMatch.HEADER).required(true)
                .description("The ETag of the version this change was decided on. Missing or * is 428; stale is "
                    + "412.")
                .schema(new StringSchema()));
            responses(operation).addApiResponse("412", new ApiResponse().$ref(PROBLEM_REF));
            responses(operation).addApiResponse("428", new ApiResponse().$ref(PROBLEM_REF));
        }
        if (parameters.contains(PageQuery.class)) {
            operation.addParametersItem(new QueryParameter().name(PageQuery.LIMIT).required(false)
                .description("Items per page.")
                .schema(new IntegerSchema().minimum(java.math.BigDecimal.ONE)
                    .maximum(java.math.BigDecimal.valueOf(PageQuery.MAX_LIMIT))._default(PageQuery.DEFAULT_LIMIT)));
            operation.addParametersItem(new QueryParameter().name(PageQuery.CURSOR).required(false)
                .description("The next value of the previous page, unchanged; it only continues the same query.")
                .schema(new StringSchema().maxLength(CursorCodec.MAX_LENGTH)));
        }
        return operation;
    }

    static void contract(OpenAPI openApi) {
        openApi.setInfo(new Info().title("DataRun API v2").version("2")
            .description("Conventions: docs/agent-context/api-v2.md. Errors are RFC 9457 problems; ids are opaque "
                + "strings; lists page by cursor; changes are named actions with If-Match and Idempotency-Key."));
        Components components = openApi.getComponents() != null ? openApi.getComponents() : new Components();
        openApi.setComponents(components);
        components.addSecuritySchemes(BEARER, new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
            .bearerFormat("JWT").description("The access token from POST /api/v1/authenticate."));
        components.addSchemas(ApiIds.SCHEMA_NAME, new StringSchema().minLength(1).maxLength(ApiIds.MAX_LENGTH)
            .description(ApiIds.DESCRIPTION));
        components.addSchemas(PROBLEM_ERROR, problemError());
        components.addSchemas(PROBLEM, problem());
        components.addResponses(PROBLEM, new ApiResponse()
            .description("An RFC 9457 problem; code is one entry of the problem registry.")
            .content(new Content().addMediaType("application/problem+json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM)))));
        openApi.setSecurity(List.of(new SecurityRequirement().addList(BEARER)));
        if (openApi.getPaths() != null) {
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                ApiResponses responses = responses(operation);
                // Error responses springdoc derives from the application's controller advice describe v1: that
                // advice never answers a v2 request, which the v2 problem resolver answers first.
                responses.entrySet().removeIf(response -> !response.getKey().startsWith("2")
                    && !PROBLEM_REF.equals(response.getValue().get$ref()));
                responses.addApiResponse("default", new ApiResponse().$ref(PROBLEM_REF));
            }));
        }
    }

    private static ApiResponses responses(Operation operation) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        return operation.getResponses();
    }

    private static Schema<?> problem() {
        Schema<?> code = new StringSchema().description("The problem's code; clients ignore codes they do not know.");
        code.addExtension("x-extensible-enum", ProblemCode.codes());
        ObjectSchema problem = new ObjectSchema();
        problem.description("RFC 9457 problem details. Extension members such as currentVersion may follow.");
        problem.addProperty("type", new StringSchema().format("uri")
            .description("tag:nmcpye.org,2026:datarun/problem/<code>; an identifier that resolves nowhere."));
        problem.addProperty("title", new StringSchema().description("The code's fixed title."));
        problem.addProperty("status", new IntegerSchema().description("The HTTP status (RFC 9457)."));
        problem.addProperty("detail", new StringSchema()
            .description("This occurrence in words; never a submitted value or a caller's reason."));
        problem.addProperty("instance", new StringSchema().format("uri")
            .description("urn:uuid: and the request id, also sent as X-Request-ID."));
        problem.addProperty("code", code);
        problem.addProperty("errors", new ArraySchema().items(new Schema<>().$ref("#/components/schemas/" + PROBLEM_ERROR)));
        problem.setRequired(List.of("type", "title", "status", "code"));
        problem.setAdditionalProperties(Boolean.TRUE);
        return problem;
    }

    private static Schema<?> problemError() {
        ObjectSchema error = new ObjectSchema();
        error.description("One finding, with at most one locator.");
        error.addProperty("code", new StringSchema().description("What is wrong, kebab-case."));
        error.addProperty("detail", new StringSchema());
        error.addProperty("pointer", new StringSchema()
            .description("A JSON Pointer into the request body, in RFC 9457's fragment form: #/reason."));
        error.addProperty("parameter", new StringSchema().description("A query or path parameter."));
        error.addProperty("header", new StringSchema().description("A request header."));
        error.addProperty("field", new Schema<>().$ref("#/components/schemas/" + ApiIds.SCHEMA_NAME));
        error.setRequired(List.of("code"));
        return error;
    }
}
