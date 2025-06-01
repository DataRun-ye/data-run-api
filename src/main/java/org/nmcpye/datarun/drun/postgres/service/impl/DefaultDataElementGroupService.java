package org.nmcpye.datarun.drun.postgres.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.common.jpa.DefaultJpaAuditableService;
import org.nmcpye.datarun.dataelement.DataElement;
import org.nmcpye.datarun.dataelement.repository.DataElementRepository;
import org.nmcpye.datarun.dataelementgroup.DataElementGroup;
import org.nmcpye.datarun.dataelementgroup.repository.DataElementGroupRepository;
import org.nmcpye.datarun.drun.postgres.service.DataElementGroupService;
import org.nmcpye.datarun.security.useraccess.UserAccessService;
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
    extends DefaultJpaAuditableService<DataElementGroup>
    implements DataElementGroupService {

    private final DataElementRepository dataElementRepository;

    public DefaultDataElementGroupService(DataElementGroupRepository repository,
                                          DataElementRepository dataElementRepository,
                                          UserAccessService userAccessService, CacheManager cacheManager) {
        super(repository, cacheManager, userAccessService);
        this.dataElementRepository = dataElementRepository;
    }


    @Override
    public DataElementGroup saveWithRelations(DataElementGroup object) {
        if (!object.getDataElements().isEmpty()) {
            Set<DataElement> dataElements = new HashSet<>();
            for (DataElement dataElement : object.getDataElements()) {
                dataElements.add(findOrgUnit(dataElement));
            }

            object.setDataElements(dataElements);
            return save(object);
        }

        return save(object);
    }

    private DataElement findOrgUnit(DataElement dataElement) {
        return Optional.ofNullable(dataElement.getUid())
            .flatMap(dataElementRepository::findByUid)
            .or(() -> Optional.ofNullable(dataElement.getId())
                .flatMap(dataElementRepository::findById))
            .or(() -> Optional.ofNullable(dataElement.getCode())
                .flatMap(dataElementRepository::findByCode))
            .orElseThrow(() -> new PropertyNotFoundException("data element not found: " + dataElement.getUid()));
    }
}
