package org.nmcpye.datarun.drun.postgres.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.domain.ChvSupply;
import org.nmcpye.datarun.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.ChvSupplyRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.ChvSupplyServiceCustom;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service Implementation for managing {@link ChvSupply}.
 */
@Service
@Primary
@Transactional
public class ChvSupplyServiceCustomImpl
    extends IdentifiableServiceImpl<ChvSupply>
    implements ChvSupplyServiceCustom {

    private final Logger log = LoggerFactory.getLogger(ChvSupplyServiceCustomImpl.class);

    private final ChvSupplyRepositoryCustom chvSupplyRepository;
    private final ActivityRepositoryCustom activityRepository;
    private final TeamRepositoryCustom teamRepository;

    public ChvSupplyServiceCustomImpl(ChvSupplyRepositoryCustom chvSupplyRepository,
                                      ActivityRepositoryCustom activityRepository,
                                      TeamRepositoryCustom teamRepository) {
        super(chvSupplyRepository);
        this.chvSupplyRepository = chvSupplyRepository;
        this.activityRepository = activityRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    public ChvSupply saveWithRelations(ChvSupply chvSupply) {
        Activity activity = activityRepository
            .findByUid(chvSupply.getActivity().getUid())
            .orElseThrow(() -> new EntityNotFoundException("Activity not found: " + chvSupply.getActivity().getUid()));
        Team team = teamRepository
            .findByUid(chvSupply.getTeam().getUid())
            .orElseThrow(() -> new EntityNotFoundException("Team not found: " + chvSupply.getTeam().getUid()));

        chvSupply.setActivity(activity);

        chvSupply.setTeam(team);

        return repository.save(chvSupply);
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
}
