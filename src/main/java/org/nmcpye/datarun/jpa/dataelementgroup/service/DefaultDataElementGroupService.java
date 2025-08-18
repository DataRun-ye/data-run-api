package org.nmcpye.datarun.jpa.dataelementgroup.service;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaIdentifiableService;
import org.nmcpye.datarun.jpa.dataelement.DataElement;
import org.nmcpye.datarun.jpa.dataelement.repository.DataElementRepository;
import org.nmcpye.datarun.jpa.dataelementgroup.DataElementGroup;
import org.nmcpye.datarun.jpa.dataelementgroup.repository.DataElementGroupRepository;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@Primary
@Transactional
public class DefaultDataElementGroupService
    extends DefaultJpaIdentifiableService<DataElementGroup>
    implements DataElementGroupService {

    private final DataElementRepository dataElementRepository;

    public DefaultDataElementGroupService(DataElementGroupRepository repository,
                                          DataElementRepository dataElementRepository,
                                          UserAccessService userAccessService, CacheManager cacheManager) {
        super(repository, cacheManager, userAccessService);
        this.dataElementRepository = dataElementRepository;
    }


    @Override
    public void preSaveHook(DataElementGroup object) {
        if (!object.getDataElements().isEmpty()) {
            Set<DataElement> dataTemplateElements = new HashSet<>();
            for (DataElement dataTemplateElement : object.getDataElements()) {
                dataTemplateElements.add(findOrgUnit(dataTemplateElement));
            }

            object.setDataElements(dataTemplateElements);
        }
    }

    private DataElement findOrgUnit(DataElement dataTemplateElement) {
        return Optional.ofNullable(dataTemplateElement.getUid())
            .flatMap(dataElementRepository::findByUid)
            .or(() -> Optional.ofNullable(dataTemplateElement.getId())
                .flatMap(dataElementRepository::findById))
            .or(() -> Optional.ofNullable(dataTemplateElement.getCode())
                .flatMap(dataElementRepository::findByCode))
            .orElseThrow(() -> new PropertyNotFoundException("data element not found: " + dataTemplateElement.getUid()));
    }
}
