package org.nmcpye.datarun.web.v2.idempotency;

import jakarta.servlet.http.HttpServletRequest;
import org.nmcpye.datarun.web.v2.caller.ApiCaller;
import org.nmcpye.datarun.web.v2.concurrency.IfMatch;
import org.nmcpye.datarun.web.v2.problem.ApiProblemException;
import org.nmcpye.datarun.web.v2.problem.ProblemCode;
import org.nmcpye.datarun.web.v2.problem.ProblemError;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Collections;
import java.util.List;

/**
 * Gives a handler parameter of type {@link CommandRequest} or {@link CreateRequest} its caller and headers, refusing
 * the request before the handler runs and before any database work: no caller 401; no or malformed
 * {@code Idempotency-Key} 400. A change also needs {@code If-Match}: missing or {@code *} 428, malformed 400. A
 * create takes none: one sent is 400.
 */
public class CommandRequestResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == CommandRequest.class
            || parameter.getParameterType() == CreateRequest.class;
    }

    @Override
    public IdempotentRequest resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                             NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        ApiCaller caller = ApiCaller.current();
        java.util.UUID key = IdempotencyKey.parse(lines(request, IdempotencyKey.HEADER));
        List<String> ifMatch = lines(request, IfMatch.HEADER);
        if (parameter.getParameterType() == CreateRequest.class) {
            if (!ifMatch.isEmpty()) {
                throw ApiProblemException.of(ProblemCode.INVALID_IF_MATCH)
                    .detail("A create names no version; send no If-Match.")
                    .error(ProblemError.atHeader(IfMatch.HEADER, "not-allowed", null));
            }
            return new CreateRequest(caller, key, request);
        }
        return new CommandRequest(caller, key, IfMatch.required(IfMatch.parse(ifMatch)), request);
    }

    private static List<String> lines(HttpServletRequest request, String header) {
        return Collections.list(request.getHeaders(header));
    }
}
