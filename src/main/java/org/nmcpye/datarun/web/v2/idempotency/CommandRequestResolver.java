package org.nmcpye.datarun.web.v2.idempotency;

import jakarta.servlet.http.HttpServletRequest;
import org.nmcpye.datarun.web.v2.caller.ApiCaller;
import org.nmcpye.datarun.web.v2.concurrency.IfMatch;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Collections;
import java.util.List;

/**
 * Gives a handler parameter of type {@link CommandRequest} its caller and headers, refusing the request before the
 * handler runs and before any database work: no caller 401; no or malformed {@code Idempotency-Key} 400; no
 * {@code If-Match}, or {@code *}, 428; malformed {@code If-Match} 400.
 */
public class CommandRequestResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == CommandRequest.class;
    }

    @Override
    public CommandRequest resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        ApiCaller caller = ApiCaller.current();
        return new CommandRequest(caller,
            IdempotencyKey.parse(lines(request, IdempotencyKey.HEADER)),
            IfMatch.required(IfMatch.parse(lines(request, IfMatch.HEADER))),
            request);
    }

    private static List<String> lines(HttpServletRequest request, String header) {
        return Collections.list(request.getHeaders(header));
    }
}
