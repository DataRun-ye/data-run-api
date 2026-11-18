package org.nmcpye.datarun.jpa.datatemplate.service;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateDto;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.jpa.datatemplate.dto.FormTemplateVersionDto;
import org.nmcpye.datarun.jpa.datatemplate.mapper.DataTemplateMapper;
import org.nmcpye.datarun.jpa.datatemplate.mapper.FormJpaTemplateVersionMapper;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateVersionRepository;
import org.nmcpye.datarun.jpa.datatemplategenerator.TemplateElementGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Publishing a version must clear the template lookups cached by uid and
 * number, or the previous version stays "current" until the cache expires.
 */
@SpringJUnitConfig(DataTemplatePublishCacheTest.Config.class)
class DataTemplatePublishCacheTest {

    private static final List<String> TEMPLATE_CACHES = List.of(
        DataTemplateRepository.TEMPLATE_BY_UID_CACHE,
        TemplateVersionResolver.TEMPLATE_VERSION_CONTEXT_CACHE,
        TemplateVersionRepository.TEMPLATE_UID_VERSION_UID_JPA_CACHE,
        TemplateVersionRepository.TEMPLATE_UID_VERSION_NO_JPA_CACHE,
        TemplateVersionRepository.TEMPLATE_UID_LATEST_VERSION_JPA_CACHE);

    @Configuration
    @EnableCaching
    static class Config {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(TEMPLATE_CACHES.toArray(String[]::new));
        }

        @Bean
        DataTemplateService masterService() {
            return mock(DataTemplateService.class);
        }

        @Bean
        DataTemplateRepository masterRepository() {
            return mock(DataTemplateRepository.class);
        }

        @Bean
        FormJpaTemplateVersionMapper versionMapper() {
            return mock(FormJpaTemplateVersionMapper.class);
        }

        @Bean
        TemplateVersionRepository versionRepository() {
            return mock(TemplateVersionRepository.class);
        }

        @Bean
        DataTemplateMapper templateMapper() {
            return mock(DataTemplateMapper.class);
        }

        @Bean
        TemplateElementGeneratorService elementGenerator() {
            return mock(TemplateElementGeneratorService.class);
        }

        @Bean
        DataTemplateInstanceService service(
            DataTemplateService masterService,
            DataTemplateRepository masterRepository,
            FormJpaTemplateVersionMapper versionMapper,
            TemplateVersionRepository versionRepository,
            DataTemplateMapper templateMapper,
            TemplateElementGeneratorService elementGenerator) {
            return new DataTemplateInstanceServiceImpl(
                masterService, masterRepository, versionMapper,
                versionRepository, templateMapper, elementGenerator);
        }
    }

    @Autowired
    private DataTemplateInstanceService service;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private DataTemplateRepository masterRepository;
    @Autowired
    private TemplateVersionRepository versionRepository;
    @Autowired
    private FormJpaTemplateVersionMapper versionMapper;
    @Autowired
    private DataTemplateMapper templateMapper;

    @Test
    void publishingClearsEveryTemplateLookupCache() {
        for (String cache : TEMPLATE_CACHES) {
            cacheManager.getCache(cache).put("Tcf3Ks9ZRpB", "previous version");
        }
        DataTemplateInstanceDto request = stubPublication("Tcf3Ks9ZRpB");

        service.publishVersion(request);

        for (String cache : TEMPLATE_CACHES) {
            assertThat(cacheManager.getCache(cache).get("Tcf3Ks9ZRpB"))
                .as(cache)
                .isNull();
        }
    }

    private DataTemplateInstanceDto stubPublication(String uid) {
        DataTemplateInstanceDto request = mock(DataTemplateInstanceDto.class);
        DataTemplate template = new DataTemplate();
        template.setUid(uid);
        template.setName("Form");
        template.setVersionNumber(30);
        template.setVersionUid("cz06mCos84F");
        TemplateVersion version = new TemplateVersion();
        DataTemplateDto templateDto = mock(DataTemplateDto.class);
        FormTemplateVersionDto versionDto = mock(FormTemplateVersionDto.class);
        DataTemplateInstanceDto saved = mock(DataTemplateInstanceDto.class);
        when(saved.getUid()).thenReturn(uid);
        when(saved.getVersionUid()).thenReturn("TLpAhIscGLm");

        when(templateMapper.fromInstanceDto(request)).thenReturn(template);
        when(versionMapper.fromInstanceDto(request)).thenReturn(version);
        when(masterRepository.findByUidForWrite(uid)).thenReturn(Optional.of(template));
        when(versionRepository.persist(version)).thenReturn(version);
        when(masterRepository.merge(template)).thenReturn(template);
        when(templateMapper.toDto(template)).thenReturn(templateDto);
        when(versionMapper.toDto(version)).thenReturn(versionDto);
        when(templateMapper.toInstanceDto(templateDto, versionDto)).thenReturn(saved);
        return request;
    }
}
