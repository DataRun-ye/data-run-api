package org.nmcpye.datarun.acl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.*;
import org.springframework.security.acls.jdbc.BasicLookupStrategy;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.sql.DataSource;

/**
 * @author Hamza Assada 16/05/2025 <7amza.it@gmail.com>
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Slf4j
public class AclConfig {
    @Bean
    public MutableAclService aclService(ObjectProvider<DataSource> dataSource,
                                        LookupStrategy lookupStrategy,
                                        AclCache aclCache) {
        return new JdbcMutableAclService(dataSource.getIfUnique(), lookupStrategy, aclCache);
    }

    @Bean
    @ConditionalOnMissingBean({SidRetrievalStrategy.class})
    public SidRetrievalStrategy sidRetrievalStrategy() {
        SidRetrievalStrategyImpl impl = new SidRetrievalStrategyImpl();
        log.info("Creating new 'SidRetrievalStrategy' ...");
        return impl;
    }

    @Bean
    public MethodSecurityExpressionHandler defaultMethodSecurityExpressionHandler(MutableAclService aclService) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        AclPermissionEvaluator permissionEvaluator = new AclPermissionEvaluator(aclService);
        expressionHandler.setPermissionEvaluator(permissionEvaluator);
        return expressionHandler;
    }

    @Bean
    public AclAuthorizationStrategy aclAuthorizationStrategy() {
        return new AclAuthorizationStrategyImpl(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Bean
    public PermissionGrantingStrategy permissionGrantingStrategy() {
        return new DefaultPermissionGrantingStrategy(new ConsoleAuditLogger());
    }

    @Bean
    public SpringCacheBasedAclCache aclCache() {
        final ConcurrentMapCache aclCache = new ConcurrentMapCache("acl_cache");
        return new SpringCacheBasedAclCache(aclCache, permissionGrantingStrategy(), aclAuthorizationStrategy());
    }

    @Bean
    public LookupStrategy lookupStrategy(ObjectProvider<DataSource> dataSource, AclCache aclCache,
                                         PermissionGrantingStrategy permissionGrantingStrategy,
                                         AclAuthorizationStrategy aclAuthorizationStrategy) {
        return new BasicLookupStrategy(dataSource.getIfUnique(), aclCache, aclAuthorizationStrategy,
            permissionGrantingStrategy);
    }
}
