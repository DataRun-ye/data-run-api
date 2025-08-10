//package org.nmcpye.datarun.jpa.audit;
//
//import org.javers.spring.auditable.AuthorProvider;
//import org.nmcpye.datarun.config.Constants;
//import org.nmcpye.datarun.security.SecurityUtils;
//import org.springframework.stereotype.Component;
//
//@Component
//public class JaversAuthorProvider implements AuthorProvider {
//
//    @Override
//    public String provide() {
//        return SecurityUtils.getCurrentUserLogin().orElse(Constants.SYSTEM);
//    }
//}
