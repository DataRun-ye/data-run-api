package org.nmcpye.datarun.config;

import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepositoryImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
//@EnableJpaRepositories({"org.nmcpye.datarun.repository",
//    "org.nmcpye.datarun.common",
//    "org.nmcpye.datarun.drun.postgres.repository"})
@EnableJpaRepositories(value = {"org.nmcpye.datarun.jpa",
    "org.nmcpye.datarun.common"},
    repositoryBaseClass = BaseJpaIdentifiableRepositoryImpl.class
)
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
@EnableTransactionManagement
public class DatabaseConfiguration {
}
