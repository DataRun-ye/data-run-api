//package org.nmcpye.datarun.config.datarun;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.util.Base64;
//
//public class CustomBasicAuthenticationFilter extends BasicAuthenticationFilter {
//
//    public CustomBasicAuthenticationFilter(AuthenticationManager authenticationManager) {
//        super(authenticationManager);
//    }
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
//        throws IOException, ServletException {
//        String header = request.getHeader("Authorization");
//
//        if (header != null && header.startsWith("Basic ")) {
//            String[] tokens = extractAndDecodeHeader(header);
//            assert tokens.length == 2;
//
//            String username = tokens[0];
//            String password = tokens[1];
//
//            Authentication authentication = new UsernamePasswordAuthenticationToken(username, password);
//            Authentication result = getAuthenticationManager().authenticate(authentication);
//
//            if (result.isAuthenticated()) {
//                chain.doFilter(request, response);
//            } else {
//                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            }
//        } else {
//            chain.doFilter(request, response);
//        }
//    }
//
//    protected String obtainUsername(String header) {
//        String encodedUserPassword = header.replace("Basic ", "");
//        String decodedUserPassword = new String(Base64.getDecoder().decode(encodedUserPassword), StandardCharsets.UTF_8);
//        int delim = decodedUserPassword.indexOf(":");
//
//        if (delim == -1) {
//            throw new RuntimeException("Invalid basic authentication token");
//        }
//        return decodedUserPassword.substring(0, delim);
//    }
//
//    protected String obtainPassword(String header) {
//        String encodedUserPassword = header.replace("Basic ", "");
//        String decodedUserPassword = new String(Base64.getDecoder().decode(encodedUserPassword), StandardCharsets.UTF_8);
//        int delim = decodedUserPassword.indexOf(":");
//
//        if (delim == -1) {
//            throw new RuntimeException("Invalid basic authentication token");
//        }
//        return decodedUserPassword.substring(delim + 1);
//    }
//
//    private String[] extractAndDecodeHeader(String header) throws IOException {
//        String encodedUserPassword = header.replace("Basic ", "");
//        String decodedUserPassword = new String(Base64.getDecoder().decode(encodedUserPassword), StandardCharsets.UTF_8);
//        return decodedUserPassword.split(":");
//    }
//
//    private String[] extractAndDecodeHeader_(String header) throws IOException {
//        byte[] base64Token = header.substring(6).getBytes(StandardCharsets.UTF_8);
//        byte[] decoded;
//        try {
//            decoded = Base64.getDecoder().decode(base64Token);
//        } catch (IllegalArgumentException e) {
//            throw new RuntimeException("Failed to decode basic authentication token");
//        }
//
//        String token = new String(decoded, StandardCharsets.UTF_8);
//        int delim = token.indexOf(":");
//
//        if (delim == -1) {
//            throw new RuntimeException("Invalid basic authentication token");
//        }
//        return new String[]{token.substring(0, delim), token.substring(delim + 1)};
//    }
//}
