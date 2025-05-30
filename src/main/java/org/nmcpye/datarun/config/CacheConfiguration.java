package org.nmcpye.datarun.config;

import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.jsr107.Eh107Configuration;
import org.hibernate.cache.jcache.ConfigSettings;
import org.nmcpye.datarun.activity.Activity;
import org.nmcpye.datarun.assignment.Assignment;
import org.nmcpye.datarun.assignment.AssignmentForm;
import org.nmcpye.datarun.caching.UserKeyGenerator;
import org.nmcpye.datarun.dataelement.DataElement;
import org.nmcpye.datarun.dataelementgroup.DataElementGroup;
import org.nmcpye.datarun.dataelementgroupset.DataElementGroupSet;
import org.nmcpye.datarun.optionset.OptionSet;
import org.nmcpye.datarun.orgunit.OrgUnit;
import org.nmcpye.datarun.orgunitgroup.OrgUnitGroup;
import org.nmcpye.datarun.orgunitgroupset.OrgUnitGroupSet;
import org.nmcpye.datarun.oulevel.OuLevel;
import org.nmcpye.datarun.project.Project;
import org.nmcpye.datarun.team.Team;
import org.nmcpye.datarun.team.TeamFormAccess;
import org.nmcpye.datarun.usegroup.UserGroup;
import org.nmcpye.datarun.user.User;
import org.nmcpye.datarun.user.repository.UserRepository;
import org.nmcpye.datarun.userauthority.Authority;
import org.nmcpye.datarun.userole.Privilege;
import org.nmcpye.datarun.userole.Role;
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

    @Bean("userKeyGen")
    public KeyGenerator userKeyGen() {
        return new UserKeyGenerator();
    }

    @Bean
    public JCacheManagerCustomizer cacheManagerCustomizer() {
        return cm -> {
            createCache(cm, UserRepository.USERS_BY_LOGIN_CACHE);
            createCache(cm, UserRepository.USERS_BY_EMAIL_CACHE);
            createCache(cm, UserRepository.USER_TEAM_IDS_CACHE);
            createCache(cm, UserRepository.USER_GROUP_IDS_CACHE);
            createCache(cm, UserRepository.USER_ACTIVITY_IDS_CACHE);
            createCache(cm, UserRepository.USER_TEAM_FORM_ACCESS_CACHE);
            createCache(cm, User.class.getName());
            createCache(cm, User.class.getName());
            createCache(cm, Authority.class.getName());
            createCache(cm, Role.class.getName());
            createCache(cm, Privilege.class.getName());
            createCache(cm, User.class.getName() + ".authorities");
            createCache(cm, User.class.getName() + ".teams");
            createCache(cm, User.class.getName() + ".roles");
            createCache(cm, User.class.getName() + ".userGroups");
            createCache(cm, Role.class.getName() + ".privileges");
            createCache(cm, Privilege.class.getName() + ".roles");

            createCache(cm, UserGroup.class.getName());
            createCache(cm, UserGroup.class.getName() + ".users");
            createCache(cm, UserGroup.class.getName() + ".managedByGroups");
            createCache(cm, UserGroup.class.getName() + ".managedGroups");

            createCache(cm, org.nmcpye.datarun.domain.EntityAuditEvent.class.getName());
            createCache(cm, Project.class.getName());
            createCache(cm, Project.class.getName() + ".activities");
            createCache(cm, Activity.class.getName());
            createCache(cm, Activity.class.getName() + ".assignments");

            createCache(cm, AssignmentForm.class.getName());
            createCache(cm, Assignment.class.getName());
            createCache(cm, Assignment.class.getName() + ".children");
            createCache(cm, Assignment.class.getName() + ".assignmentForms");

            createCache(cm, TeamFormAccess.class.getName());
            createCache(cm, Team.class.getName());
            createCache(cm, Team.class.getName() + ".assignments");
            createCache(cm, Team.class.getName() + ".users");
            createCache(cm, Team.class.getName() + ".managedTeams");
            createCache(cm, Team.class.getName() + ".managedByTeams");
            createCache(cm, Team.class.getName() + ".teamFormAccesses");


            createCache(cm, OrgUnit.class.getName());
            createCache(cm, OrgUnit.class.getName() + ".assignments");
            createCache(cm, OrgUnit.class.getName() + ".orgUnitGroups");
            createCache(cm, OrgUnit.class.getName() + ".children");
            createCache(cm, OuLevel.class.getName());

            createCache(cm, OrgUnitGroup.class.getName());
            createCache(cm, OrgUnitGroup.class.getName() + ".orgUnits");
            createCache(cm, OrgUnitGroup.class.getName() + ".orgUnitGroupSets");
            createCache(cm, OrgUnitGroupSet.class.getName());
            createCache(cm, OrgUnitGroupSet.class.getName() + ".orgUnitGroups");
            createCache(cm, DataElement.class.getName());
            createCache(cm, DataElement.class.getName() + ".dataElementGroups");
            createCache(cm, DataElementGroup.class.getName());
            createCache(cm, DataElementGroup.class.getName() + ".dataElements");
            createCache(cm, DataElementGroup.class.getName() + ".dataElementGroupSets");
            createCache(cm, DataElementGroupSet.class.getName());
            createCache(cm, DataElementGroupSet.class.getName() + ".dataElementGroups");

            createCache(cm, OptionSet.class.getName());
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
