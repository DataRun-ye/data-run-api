package org.nmcpye.datarun.drun.postgres.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.common.jpa.impl.DefaultJpaAuditableService;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroup;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroupSet;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitGroupRepository;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitGroupSetRepository;
import org.nmcpye.datarun.drun.postgres.service.OrgUnitGroupSetService;
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
public class DefaultOrgUnitGroupSetService extends DefaultJpaAuditableService<OrgUnitGroupSet> implements OrgUnitGroupSetService {

    private final OrgUnitGroupSetRepository repository;
    private final OrgUnitGroupRepository orgUnitGroupRepository;

    public DefaultOrgUnitGroupSetService(OrgUnitGroupSetRepository repository, OrgUnitGroupRepository orgUnitGroupRepository, CacheManager cacheManager, UserAccessService userAccessService) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
        this.orgUnitGroupRepository = orgUnitGroupRepository;
    }


    @Override
    public OrgUnitGroupSet saveWithRelations(OrgUnitGroupSet object) {
        if (!object.getOrgUnitGroups().isEmpty()) {
            Set<OrgUnitGroup> orgUnitGroups = new HashSet<>();
            for (OrgUnitGroup orgUnitGroup : object.getOrgUnitGroups()) {
                orgUnitGroups.add(findOrgUnitGroup(orgUnitGroup));
            }

            object.setOrgUnitGroups(orgUnitGroups);
            return save(object);
        }

        return save(object);
    }

    private OrgUnitGroup findOrgUnitGroup(OrgUnitGroup orgUnitGroup) {
        return Optional.ofNullable(orgUnitGroup.getUid()).flatMap(orgUnitGroupRepository::findByUid).or(() -> Optional.ofNullable(orgUnitGroup.getId()).flatMap(orgUnitGroupRepository::findById)).or(() -> Optional.ofNullable(orgUnitGroup.getCode()).flatMap(orgUnitGroupRepository::findByCode)).orElseThrow(() -> new PropertyNotFoundException("orgUnitGroup not found: " + orgUnitGroup));
    }
}
