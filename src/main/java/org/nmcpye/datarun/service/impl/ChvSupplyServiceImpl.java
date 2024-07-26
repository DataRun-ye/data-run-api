package org.nmcpye.datarun.service.impl;

import java.util.List;
import java.util.Optional;
import org.nmcpye.datarun.domain.ChvSupply;
import org.nmcpye.datarun.repository.ChvSupplyRepository;
import org.nmcpye.datarun.service.ChvSupplyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link org.nmcpye.datarun.domain.ChvSupply}.
 */
@Service
@Transactional
public class ChvSupplyServiceImpl implements ChvSupplyService {

    private final Logger log = LoggerFactory.getLogger(ChvSupplyServiceImpl.class);

    private final ChvSupplyRepository chvSupplyRepository;

    public ChvSupplyServiceImpl(ChvSupplyRepository chvSupplyRepository) {
        this.chvSupplyRepository = chvSupplyRepository;
    }

    @Override
    public ChvSupply save(ChvSupply chvSupply) {
        log.debug("Request to save ChvSupply : {}", chvSupply);
        return chvSupplyRepository.save(chvSupply);
    }

    @Override
    public ChvSupply update(ChvSupply chvSupply) {
        log.debug("Request to update ChvSupply : {}", chvSupply);
        return chvSupplyRepository.save(chvSupply);
    }

    @Override
    public Optional<ChvSupply> partialUpdate(ChvSupply chvSupply) {
        log.debug("Request to partially update ChvSupply : {}", chvSupply);

        return chvSupplyRepository
            .findById(chvSupply.getId())
            .map(existingChvSupply -> {
                if (chvSupply.getUid() != null) {
                    existingChvSupply.setUid(chvSupply.getUid());
                }
                if (chvSupply.getCode() != null) {
                    existingChvSupply.setCode(chvSupply.getCode());
                }
                if (chvSupply.getName() != null) {
                    existingChvSupply.setName(chvSupply.getName());
                }
                if (chvSupply.getItem() != null) {
                    existingChvSupply.setItem(chvSupply.getItem());
                }
                if (chvSupply.getPreviousBalance() != null) {
                    existingChvSupply.setPreviousBalance(chvSupply.getPreviousBalance());
                }
                if (chvSupply.getNewSupply() != null) {
                    existingChvSupply.setNewSupply(chvSupply.getNewSupply());
                }
                if (chvSupply.getConsumed() != null) {
                    existingChvSupply.setConsumed(chvSupply.getConsumed());
                }
                if (chvSupply.getLostCorrupt() != null) {
                    existingChvSupply.setLostCorrupt(chvSupply.getLostCorrupt());
                }
                if (chvSupply.getRemaining() != null) {
                    existingChvSupply.setRemaining(chvSupply.getRemaining());
                }
                if (chvSupply.getComment() != null) {
                    existingChvSupply.setComment(chvSupply.getComment());
                }
                if (chvSupply.getDeleted() != null) {
                    existingChvSupply.setDeleted(chvSupply.getDeleted());
                }
                if (chvSupply.getStartEntryTime() != null) {
                    existingChvSupply.setStartEntryTime(chvSupply.getStartEntryTime());
                }
                if (chvSupply.getFinishedEntryTime() != null) {
                    existingChvSupply.setFinishedEntryTime(chvSupply.getFinishedEntryTime());
                }
                if (chvSupply.getStatus() != null) {
                    existingChvSupply.setStatus(chvSupply.getStatus());
                }

                return existingChvSupply;
            })
            .map(chvSupplyRepository::save);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChvSupply> findAll() {
        log.debug("Request to get all ChvSupplies");
        return chvSupplyRepository.findAll();
    }

    public Page<ChvSupply> findAllWithEagerRelationships(Pageable pageable) {
        return chvSupplyRepository.findAllWithEagerRelationships(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChvSupply> findOne(Long id) {
        log.debug("Request to get ChvSupply : {}", id);
        return chvSupplyRepository.findOneWithEagerRelationships(id);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete ChvSupply : {}", id);
        chvSupplyRepository.deleteById(id);
    }
}
