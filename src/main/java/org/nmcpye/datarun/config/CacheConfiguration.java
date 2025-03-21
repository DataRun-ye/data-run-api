package org.nmcpye.datarun.config;

import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.jsr107.Eh107Configuration;
import org.hibernate.cache.jcache.ConfigSettings;
import org.nmcpye.datarun.common.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.jhipster.config.JHipsterProperties;
import tech.jhipster.config.cache.PrefixedKeyGenerator;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfiguration {

    private GitProperties gitProperties;
    private BuildProperties buildProperties;
    private final javax.cache.configuration.Configuration<Object, Object> jcacheConfiguration;

    public CacheConfiguration(JHipsterProperties jHipsterProperties) {
        JHipsterProperties.Cache.Ehcache ehcache = jHipsterProperties.getCache().getEhcache();

        jcacheConfiguration = Eh107Configuration.fromEhcacheCacheConfiguration(
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                Object.class,
                Object.class,
                ResourcePoolsBuilder.heap(ehcache.getMaxEntries())
            )
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(ehcache.getTimeToLiveSeconds())))
                .build()
        );
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(javax.cache.CacheManager cacheManager) {
        return hibernateProperties -> hibernateProperties.put(ConfigSettings.CACHE_MANAGER, cacheManager);
    }

    @Bean
    public JCacheManagerCustomizer cacheManagerCustomizer() {
        return cm -> {
            createCache(cm, UserRepository.USERS_BY_LOGIN_CACHE);
            createCache(cm, UserRepository.USERS_BY_EMAIL_CACHE);
            createCache(cm, org.nmcpye.datarun.domain.User.class.getName());
            createCache(cm, org.nmcpye.datarun.domain.Authority.class.getName());
            createCache(cm, org.nmcpye.datarun.domain.User.class.getName() + ".authorities");
            createCache(cm, org.nmcpye.datarun.domain.User.class.getName() + ".teams");
            createCache(cm, org.nmcpye.datarun.domain.EntityAuditEvent.class.getName());
            createCache(cm, org.nmcpye.datarun.domain.Project.class.getName());
            createCache(cm, org.nmcpye.datarun.domain.Project.class.getName() + ".activities");
            createCache(cm, org.nmcpye.datarun.domain.Activity.class.getName());
            createCache(cm, org.nmcpye.datarun.drun.postgres.domain.Assignment.class.getName());
            createCache(cm, org.nmcpye.datarun.drun.postgres.domain.Team.class.getName());
            createCache(cm, org.nmcpye.datarun.drun.postgres.domain.Team.class.getName() + ".assignments");
            createCache(cm, org.nmcpye.datarun.drun.postgres.domain.Team.class.getName() + ".users");
            createCache(cm, org.nmcpye.datarun.drun.postgres.domain.Team.class.getName() + ".managedTeams");
            createCache(cm, org.nmcpye.datarun.drun.postgres.domain.Team.class.getName() + ".managedByTeams");
            createCache(cm, org.nmcpye.datarun.drun.postgres.domain.OrgUnit.class.getName());
            createCache(cm, org.nmcpye.datarun.drun.postgres.domain.OrgUnit.class.getName() + ".assignments");
            createCache(cm, org.nmcpye.datarun.drun.postgres.domain.OrgUnit.class.getName() + ".groups");
            createCache(cm, org.nmcpye.datarun.drun.postgres.domain.OrgUnit.class.getName() + ".children");

            createCache(cm, org.nmcpye.datarun.drun.postgres.domain.OuLevel.class.getName());
            createCache(cm, org.nmcpye.datarun.domain.WarehouseItem.class.getName());
            createCache(cm, org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroup.class.getName());
            createCache(cm, org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroup.class.getName() + ".members");
            createCache(cm, org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroup.class.getName() + ".groupSets");
            createCache(cm, org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroupSet.class.getName());
            createCache(cm, org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroupSet.class.getName() + ".orgUnitGroups");

            // jhipster-needle-ehcache-add-entry
        };
    }

    private void createCache(javax.cache.CacheManager cm, String cacheName) {
        javax.cache.Cache<Object, Object> cache = cm.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        } else {
            cm.createCache(cacheName, jcacheConfiguration);
        }
    }

    @Autowired(required = false)
    public void setGitProperties(GitProperties gitProperties) {
        this.gitProperties = gitProperties;
    }

    @Autowired(required = false)
    public void setBuildProperties(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Bean
    public KeyGenerator keyGenerator() {
        return new PrefixedKeyGenerator(this.gitProperties, this.buildProperties);
    }
}
