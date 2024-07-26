package org.nmcpye.datarun.drun.postgres.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.domain.ChvSession;
import org.nmcpye.datarun.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.ChvSessionRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.ChvSessionServiceCustom;
import org.nmcpye.datarun.drun.postgres.service.IdentifiableServiceImpl;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Primary
@Transactional
public class ChvSessionServiceCustomImpl
    extends IdentifiableServiceImpl<ChvSession>
    implements ChvSessionServiceCustom {

    private final Logger log = LoggerFactory.getLogger(ChvSessionServiceCustomImpl.class);

    private final ChvSessionRepositoryCustom chvSessionRepository;
    private final ActivityRepositoryCustom activityRepository;
    private final TeamRepositoryCustom teamRepository;

    public ChvSessionServiceCustomImpl(ChvSessionRepositoryCustom chvSessionRepository,
        ActivityRepositoryCustom activityRepository,
        TeamRepositoryCustom teamRepository) {
        super(chvSessionRepository);
        this.chvSessionRepository = chvSessionRepository;
        this.activityRepository = activityRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    public ChvSession saveWithRelations(ChvSession chvSession) {
        Activity activity = activityRepository
            .findByUid(chvSession.getActivity().getUid())
            .orElseThrow(() -> new EntityNotFoundException("Activity not found: " + chvSession.getActivity().getUid()));
        Team team = teamRepository
            .findByUid(chvSession.getTeam().getUid())
            .orElseThrow(() -> new EntityNotFoundException("Team not found: " + chvSession.getTeam().getUid()));

        chvSession.setActivity(activity);

        chvSession.setTeam(team);

        if (chvSession.getUid() == null || chvSession.getUid().isEmpty()) {
            chvSession.setUid(CodeGenerator.generateUid());
        }

        return repository.save(chvSession);
    }

    @Override
    public Optional<ChvSession> partialUpdate(ChvSession chvSession) {
        log.debug("Request to partially update ChvSession : {}", chvSession);

        return chvSessionRepository
            .findById(chvSession.getId())
            .map(existingChvSession -> {
                if (chvSession.getUid() != null) {
                    existingChvSession.setUid(chvSession.getUid());
                }
                if (chvSession.getCode() != null) {
                    existingChvSession.setCode(chvSession.getCode());
                }
                if (chvSession.getName() != null) {
                    existingChvSession.setName(chvSession.getName());
                }
                if (chvSession.getSessionDate() != null) {
                    existingChvSession.setSessionDate(chvSession.getSessionDate());
                }
                if (chvSession.getSubject() != null) {
                    existingChvSession.setSubject(chvSession.getSubject());
                }
                if (chvSession.getSessions() != null) {
                    existingChvSession.setSessions(chvSession.getSessions());
                }
                if (chvSession.getPeople() != null) {
                    existingChvSession.setPeople(chvSession.getPeople());
                }
                if (chvSession.getComment() != null) {
                    existingChvSession.setComment(chvSession.getComment());
                }
                if (chvSession.getDeleted() != null) {
                    existingChvSession.setDeleted(chvSession.getDeleted());
                }
                if (chvSession.getStartEntryTime() != null) {
                    existingChvSession.setStartEntryTime(chvSession.getStartEntryTime());
                }
                if (chvSession.getFinishedEntryTime() != null) {
                    existingChvSession.setFinishedEntryTime(chvSession.getFinishedEntryTime());
                }
                if (chvSession.getStatus() != null) {
                    existingChvSession.setStatus(chvSession.getStatus());
                }
                if (chvSession.getCreatedBy() != null) {
                    existingChvSession.setCreatedBy(chvSession.getCreatedBy());
                }
                if (chvSession.getCreatedDate() != null) {
                    existingChvSession.setCreatedDate(chvSession.getCreatedDate());
                }
                if (chvSession.getLastModifiedBy() != null) {
                    existingChvSession.setLastModifiedBy(chvSession.getLastModifiedBy());
                }
                if (chvSession.getLastModifiedDate() != null) {
                    existingChvSession.setLastModifiedDate(chvSession.getLastModifiedDate());
                }

                return existingChvSession;
            })
            .map(chvSessionRepository::save);
    }
}
