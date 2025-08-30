package org.nmcpye.datarun.jpa.dataelementgroupset.service;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaIdentifiableService;
import org.nmcpye.datarun.jpa.dataelementgroup.DataElementGroup;
import org.nmcpye.datarun.jpa.dataelementgroup.repository.DataElementGroupRepository;
import org.nmcpye.datarun.jpa.dataelementgroupset.DataElementGroupSet;
import org.nmcpye.datarun.jpa.dataelementgroupset.repository.DataElementGroupSetRepository;
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
public class DefaultDataElementGroupSetService
    extends DefaultJpaIdentifiableService<DataElementGroupSet>
    implements DataElementGroupSetService {

    private final DataElementGroupRepository dataElementGroupRepository;

    public DefaultDataElementGroupSetService(DataElementGroupSetRepository repository,
                                             DataElementGroupRepository dataElementGroupRepository,
                                             CacheManager cacheManager, UserAccessService userAccessService) {
        super(repository, cacheManager, userAccessService);
        this.dataElementGroupRepository = dataElementGroupRepository;
    }

    @Override
    public DataElementGroupSet saveWithRelations(DataElementGroupSet object) {
        if (!object.getDataElementGroups().isEmpty()) {
            Set<DataElementGroup> dataElementGroups = new HashSet<>();
            for (DataElementGroup dataElementGroup : object.getDataElementGroups()) {
                dataElementGroups.add(findOrgUnitGroup(dataElementGroup));
            }

            object.setDataElementGroups(dataElementGroups);
        }
        return save(object);
    }

    private DataElementGroup findOrgUnitGroup(DataElementGroup dataElementGroup) {
        return Optional.ofNullable(dataElementGroup.getUid())
            .flatMap(dataElementGroupRepository::findByUid)
            .or(() -> Optional.ofNullable(dataElementGroup.getId())
                .flatMap(dataElementGroupRepository::findById))
            .or(() -> Optional.ofNullable(dataElementGroup.getCode())
                .flatMap(dataElementGroupRepository::findByCode))
            .orElseThrow(() -> new PropertyNotFoundException("dataElementGroup not found: " + dataElementGroup));
    }
}
