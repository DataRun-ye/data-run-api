package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.domain.WarehouseTransaction;
import org.nmcpye.datarun.drun.postgres.repository.WarehouseTransactionRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.WarehouseTransactionServiceCustom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Primary
@Transactional
public class WarehouseTransactionServiceCustomImpl
    extends IdentifiableServiceImpl<WarehouseTransaction>
    implements WarehouseTransactionServiceCustom {

    private final Logger log = LoggerFactory.getLogger(WarehouseTransactionServiceCustomImpl.class);

    final private WarehouseTransactionRepositoryCustom warehouseTransactionRepository;

    public WarehouseTransactionServiceCustomImpl(WarehouseTransactionRepositoryCustom warehouseTransactionRepository) {
        super(warehouseTransactionRepository);
        this.warehouseTransactionRepository = warehouseTransactionRepository;
    }

    @Override
    public Optional<WarehouseTransaction> partialUpdate(WarehouseTransaction warehouseTransaction) {
        log.debug("Request to partially update WarehouseTransaction : {}", warehouseTransaction);

        return warehouseTransactionRepository
            .findById(warehouseTransaction.getId())
            .map(existingWarehouseTransaction -> {
                if (warehouseTransaction.getUid() != null) {
                    existingWarehouseTransaction.setUid(warehouseTransaction.getUid());
                }
                if (warehouseTransaction.getCode() != null) {
                    existingWarehouseTransaction.setCode(warehouseTransaction.getCode());
                }
                if (warehouseTransaction.getName() != null) {
                    existingWarehouseTransaction.setName(warehouseTransaction.getName());
                }
                if (warehouseTransaction.getImovUid() != null) {
                    existingWarehouseTransaction.setImovUid(warehouseTransaction.getImovUid());
                }
                if (warehouseTransaction.getTransactionDate() != null) {
                    existingWarehouseTransaction.setTransactionDate(warehouseTransaction.getTransactionDate());
                }
                if (warehouseTransaction.getPhaseNo() != null) {
                    existingWarehouseTransaction.setPhaseNo(warehouseTransaction.getPhaseNo());
                }
                if (warehouseTransaction.getEntryType() != null) {
                    existingWarehouseTransaction.setEntryType(warehouseTransaction.getEntryType());
                }
                if (warehouseTransaction.getQuantity() != null) {
                    existingWarehouseTransaction.setQuantity(warehouseTransaction.getQuantity());
                }
                if (warehouseTransaction.getNotes() != null) {
                    existingWarehouseTransaction.setNotes(warehouseTransaction.getNotes());
                }
                if (warehouseTransaction.getPersonName() != null) {
                    existingWarehouseTransaction.setPersonName(warehouseTransaction.getPersonName());
                }
                if (warehouseTransaction.getWorkDayId() != null) {
                    existingWarehouseTransaction.setWorkDayId(warehouseTransaction.getWorkDayId());
                }
                if (warehouseTransaction.getSubmissionTime() != null) {
                    existingWarehouseTransaction.setSubmissionTime(warehouseTransaction.getSubmissionTime());
                }
                if (warehouseTransaction.getSubmissionId() != null) {
                    existingWarehouseTransaction.setSubmissionId(warehouseTransaction.getSubmissionId());
                }
                if (warehouseTransaction.getDeleted() != null) {
                    existingWarehouseTransaction.setDeleted(warehouseTransaction.getDeleted());
                }
                if (warehouseTransaction.getSubmissionUuid() != null) {
                    existingWarehouseTransaction.setSubmissionUuid(warehouseTransaction.getSubmissionUuid());
                }
                if (warehouseTransaction.getStartEntryTime() != null) {
                    existingWarehouseTransaction.setStartEntryTime(warehouseTransaction.getStartEntryTime());
                }
                if (warehouseTransaction.getFinishedEntryTime() != null) {
                    existingWarehouseTransaction.setFinishedEntryTime(warehouseTransaction.getFinishedEntryTime());
                }
                if (warehouseTransaction.getStatus() != null) {
                    existingWarehouseTransaction.setStatus(warehouseTransaction.getStatus());
                }
                if (warehouseTransaction.getCreatedBy() != null) {
                    existingWarehouseTransaction.setCreatedBy(warehouseTransaction.getCreatedBy());
                }
                if (warehouseTransaction.getCreatedDate() != null) {
                    existingWarehouseTransaction.setCreatedDate(warehouseTransaction.getCreatedDate());
                }
                if (warehouseTransaction.getLastModifiedBy() != null) {
                    existingWarehouseTransaction.setLastModifiedBy(warehouseTransaction.getLastModifiedBy());
                }
                if (warehouseTransaction.getLastModifiedDate() != null) {
                    existingWarehouseTransaction.setLastModifiedDate(warehouseTransaction.getLastModifiedDate());
                }

                return existingWarehouseTransaction;
            })
            .map(warehouseTransactionRepository::save);
    }

}
