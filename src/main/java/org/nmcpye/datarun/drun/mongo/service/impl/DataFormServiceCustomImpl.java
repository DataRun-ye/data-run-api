package org.nmcpye.datarun.drun.mongo.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.mongo.domain.DataForm;
import org.nmcpye.datarun.drun.mongo.repository.DataFormRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.service.DataFormServiceCustom;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRepositoryCustom;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * Service Implementation for managing {@link DataForm}.
 */
@Service
@Primary
public class DataFormServiceCustomImpl
    extends IdentifiableMongoServiceImpl<DataForm>
    implements DataFormServiceCustom {

    private final Logger log = LoggerFactory.getLogger(DataFormServiceCustomImpl.class);

    private final DataFormRepositoryCustom dataFormRepository;

    private final ActivityRepositoryCustom activityRepository;

    public DataFormServiceCustomImpl(DataFormRepositoryCustom dataFormRepository, ActivityRepositoryCustom activityRepository) {
        super(dataFormRepository);
        this.dataFormRepository = dataFormRepository;
        this.activityRepository = activityRepository;
    }

    @Override
    public DataForm saveWithRelations(DataForm dataForm) {
        Activity activity = activityRepository.findByUid(dataForm.getActivity())
            .orElseThrow(() -> new PropertyNotFoundException("Activity not found: " + dataForm.getActivity()));
        dataForm.setActivity(activity.getUid());
        // Generate UID if not present
        if (dataForm.getUid() == null || dataForm.getUid().isEmpty()) {
            dataForm.setUid(CodeGenerator.generateUid());
        }

        // Save the DataForm entity
        return dataFormRepository.save(dataForm);
    }

    @Override
    public DataForm save(DataForm object) {
        final Integer version =
            Objects.requireNonNullElse(object.getVersion(), 0) + 1;
        object.setVersion(version);
        return super.save(object);
    }

    @Override
    public DataForm update(DataForm object) {
        final Integer version =
            Objects.requireNonNullElse(object.getVersion(), 0) + 1;
        object.setVersion(version);
        return super.update(object);
    }

    @Override
    public Optional<DataForm> partialUpdate(DataForm dataForm) {
        log.debug("Request to partially update DataForm : {}", dataForm);

        return dataFormRepository
            .findById(dataForm.getId())
            .map(existingDataForm -> {
                if (dataForm.getUid() != null) {
                    existingDataForm.setUid(dataForm.getUid());
                }
                if (dataForm.getCode() != null) {
                    existingDataForm.setCode(dataForm.getCode());
                }
                if (dataForm.getName() != null) {
                    existingDataForm.setName(dataForm.getName());
                }
                if (dataForm.getDescription() != null) {
                    existingDataForm.setDescription(dataForm.getDescription());
                }
                if (dataForm.getDisabled() != null) {
                    existingDataForm.setDisabled(dataForm.getDisabled());
                }

                return existingDataForm;
            })
            .map(dataFormRepository::save);
    }
}
