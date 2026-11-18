package org.nmcpye.datarun.jpa.datatemplate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.nmcpye.datarun.apiquery.QueryRequest;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateVersionRepository;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultTemplateVersionServiceTest {

    private DataTemplateService dataTemplateService;
    private TemplateVersionRepository repository;
    private DefaultTemplateVersionService service;

    @BeforeEach
    void setUp() {
        dataTemplateService = mock(DataTemplateService.class);
        repository = mock(TemplateVersionRepository.class);
        service = new DefaultTemplateVersionService(
            repository,
            mock(CacheManager.class),
            dataTemplateService,
            mock(UserAccessService.class));
    }

    @Test
    void returnsEveryVersionOfAccessibleTemplatesForAnUnpagedRead() {
        accessibleTemplates("templateA01", "templateB01");
        when(repository.findAllByTemplateUidIn(List.of("templateA01", "templateB01")))
            .thenReturn(List.of(
                version("templateB01", 1),
                version("templateA01", 29),
                version("templateA01", 28),
                version("templateA01", 30)));

        Page<TemplateVersion> page = service.findAllByUser(unpaged(), null);

        assertThat(page.getContent())
            .extracting(v -> v.getTemplateUid() + "@" + v.getVersionNumber())
            .containsExactly("templateA01@28", "templateA01@29", "templateA01@30", "templateB01@1");
        assertThat(templatesRequest().isPaged()).isFalse();
    }

    @Test
    void pagesOverVersionsWhenPagingIsRequested() {
        accessibleTemplates("templateA01", "templateB01");
        when(repository.findAllByTemplateUidIn(List.of("templateA01", "templateB01")))
            .thenReturn(List.of(
                version("templateA01", 28),
                version("templateA01", 29),
                version("templateA01", 30),
                version("templateB01", 1)));
        QueryRequest request = new QueryRequest();
        request.setPage(1);
        request.setSize(2);

        Page<TemplateVersion> page = service.findAllByUser(request, null);

        assertThat(page.getContent())
            .extracting(v -> v.getTemplateUid() + "@" + v.getVersionNumber())
            .containsExactly("templateA01@30", "templateB01@1");
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(templatesRequest().isPaged())
            .as("templates are read in full; paging applies to versions")
            .isFalse();
        assertThat(request.isPaged()).as("the caller's request is not modified").isTrue();
    }

    @Test
    void returnsNothingWithoutQueryingVersionsWhenNoTemplateIsAccessible() {
        accessibleTemplates();

        Page<TemplateVersion> page = service.findAllByUser(unpaged(), null);

        assertThat(page.getContent()).isEmpty();
        verify(repository, never()).findAllByTemplateUidIn(anyCollection());
    }

    private void accessibleTemplates(String... uids) {
        List<DataTemplate> templates = java.util.Arrays.stream(uids)
            .map(uid -> {
                DataTemplate template = new DataTemplate();
                template.setUid(uid);
                return template;
            })
            .toList();
        when(dataTemplateService.findAllByUser(any(QueryRequest.class), any()))
            .thenReturn(new PageImpl<>(templates));
    }

    private QueryRequest templatesRequest() {
        ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(dataTemplateService).findAllByUser(captor.capture(), any());
        return captor.getValue();
    }

    private static QueryRequest unpaged() {
        QueryRequest request = new QueryRequest();
        request.setPaged(false);
        return request;
    }

    private static TemplateVersion version(String templateUid, int number) {
        TemplateVersion version = new TemplateVersion();
        version.setTemplateUid(templateUid);
        version.setVersionNumber(number);
        return version;
    }
}
