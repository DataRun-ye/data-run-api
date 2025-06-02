package org.nmcpye.datarun.jpa.entityattribute.service;

import org.nmcpye.datarun.jpa.entityattribute.repository.EntityAttributeValueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultEntityAttributeValueService
    implements EntityAttributeValueService {

    private final EntityAttributeValueRepository repository;

    public DefaultEntityAttributeValueService(EntityAttributeValueRepository repository) {
        this.repository = repository;
    }
}
