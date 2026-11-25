package org.nmcpye.datarun.web.v2.paging;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Gives a handler parameter of type {@link PageQuery} the request's {@code limit} and {@code cursor}. */
public class PageQueryResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == PageQuery.class;
    }

    @Override
    public PageQuery resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                     NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return PageQuery.of(webRequest.getParameter(PageQuery.LIMIT), webRequest.getParameter(PageQuery.CURSOR));
    }
}
