package org.nmcpye.datarun.drun.postgres.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.common.jpa.DefaultJpaAuditableService;
import org.nmcpye.datarun.dataelementgroup.DataElementGroup;
import org.nmcpye.datarun.dataelementgroup.repository.DataElementGroupRepository;
import org.nmcpye.datarun.dataelementgroupset.DataElementGroupSet;
import org.nmcpye.datarun.dataelementgroupset.repository.DataElementGroupSetRepository;
import org.nmcpye.datarun.drun.postgres.service.DataElementGroupSetService;
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
public class DefaultDataElementGroupSetService
    extends DefaultJpaAuditableService<DataElementGroupSet>
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
            return save(object);
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
