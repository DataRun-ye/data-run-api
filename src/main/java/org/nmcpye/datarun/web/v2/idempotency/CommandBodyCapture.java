package org.nmcpye.datarun.web.v2.idempotency;

import org.nmcpye.datarun.web.v2.config.V2Properties;
import org.nmcpye.datarun.web.v2.problem.ApiProblemException;
import org.nmcpye.datarun.web.v2.problem.ProblemCode;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * Keeps the body of a command (any handler that takes {@link CommandRequest}, wherever it is declared) as received,
 * for its fingerprint, and hands the same bytes on to the handler's body parameter. A body over the configured size
 * is refused, 413, before it is parsed. It applies to every controller; {@link #supports} limits it to commands.
 */
@ControllerAdvice
public class CommandBodyCapture extends RequestBodyAdviceAdapter {

    private final V2Properties properties;

    public CommandBodyCapture(V2Properties properties) {
        this.properties = properties;
    }

    @Override
    public boolean supports(MethodParameter parameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return parameter.getMethod() != null && Arrays.stream(parameter.getMethod().getParameterTypes())
            .anyMatch(type -> type == CommandRequest.class);
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage input, MethodParameter parameter, Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType)
        throws IOException {
        long max = properties.getCommands().getMaxBodySize().toBytes();
        byte[] body;
        try (InputStream in = input.getBody()) {
            body = in.readNBytes((int) Math.min(Integer.MAX_VALUE - 8, max + 1));
        }
        if (body.length > max) {
            throw ApiProblemException.of(ProblemCode.PAYLOAD_TOO_LARGE)
                .detail("A command body may be at most " + max + " bytes.");
        }
        RequestContextHolder.currentRequestAttributes()
            .setAttribute(CommandRequest.BODY_ATTRIBUTE, body, RequestAttributes.SCOPE_REQUEST);
        HttpHeaders headers = input.getHeaders();
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(body);
            }

            @Override
            public HttpHeaders getHeaders() {
                return headers;
            }
        };
    }
}
