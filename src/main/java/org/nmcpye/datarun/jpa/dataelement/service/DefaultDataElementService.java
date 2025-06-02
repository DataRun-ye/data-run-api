package org.nmcpye.datarun.jpa.dataelement.service;

import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaAuditableService;
import org.nmcpye.datarun.jpa.dataelement.DataElement;
import org.nmcpye.datarun.jpa.dataelement.repository.DataElementRepository;
import org.nmcpye.datarun.jpa.optionset.repository.OptionSetRepository;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class DefaultDataElementService extends DefaultJpaAuditableService<DataElement> implements DataElementService {
    private final OptionSetRepository optionSetRepository;

    public DefaultDataElementService(DataElementRepository repository, CacheManager cacheManager, OptionSetRepository optionSetRepository, UserAccessService userAccessService) {
        super(repository, cacheManager, userAccessService);
        this.optionSetRepository = optionSetRepository;
    }

    @Override
    public DataElement saveWithRelations(DataElement element) {
        if (element.getType().isOptionsType() && element.getOptionSet() != null) {
            final var optionSet = optionSetRepository.findByUid(element.getOptionSet().getUid()).or(() -> optionSetRepository.findById(element.getOptionSet().getId())).orElseThrow(() -> new IllegalStateException(element.getName() + "'s Option Set not found"));
            element.setOptionSet(optionSet);
        }
        return save(element);
    }
}
