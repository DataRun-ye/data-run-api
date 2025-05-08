//package org.nmcpye.datarun.web.mvc;
//
//import jakarta.servlet.http.HttpServletRequest;
//import org.nmcpye.datarun.common.DRunApiVersion;
//import org.springframework.core.MethodParameter;
//import org.springframework.stereotype.Component;
//import org.springframework.web.bind.support.WebDataBinderFactory;
//import org.springframework.web.context.request.NativeWebRequest;
//import org.springframework.web.method.support.HandlerMethodArgumentResolver;
//import org.springframework.web.method.support.ModelAndViewContainer;
//
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//@Component
//public class DRunApiVersionHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {
//    private Pattern API_VERSION_PATTERN = Pattern.compile("/api/(?<version>[0-9]{1,2})/");
//
//    @Override
//    public boolean supportsParameter(MethodParameter parameter) {
//        return DRunApiVersion.class.isAssignableFrom(parameter.getParameterType());
//    }
//
//    @Override
//    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
//                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory)
//        throws Exception {
//        String requestURI = ((HttpServletRequest) webRequest.getNativeRequest()).getRequestURI();
//        Matcher matcher = API_VERSION_PATTERN.matcher(requestURI);
//
//        if (matcher.find()) {
//            Integer version = Integer.valueOf(matcher.group("version"));
//            return DRunApiVersion.getVersion(version);
//        }
//
//        return DRunApiVersion.DEFAULT;
//    }
//}
