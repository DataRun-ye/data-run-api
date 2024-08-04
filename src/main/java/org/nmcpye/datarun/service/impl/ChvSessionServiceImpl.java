package org.nmcpye.datarun.service.impl;

import java.util.Optional;
import org.nmcpye.datarun.domain.ChvSession;
import org.nmcpye.datarun.repository.ChvSessionRepository;
import org.nmcpye.datarun.service.ChvSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link org.nmcpye.datarun.domain.ChvSession}.
 */
@Service
@Transactional
public class ChvSessionServiceImpl implements ChvSessionService {

    private final Logger log = LoggerFactory.getLogger(ChvSessionServiceImpl.class);

    private final ChvSessionRepository chvSessionRepository;

    public ChvSessionServiceImpl(ChvSessionRepository chvSessionRepository) {
        this.chvSessionRepository = chvSessionRepository;
    }

    @Override
    public ChvSession save(ChvSession chvSession) {
        log.debug("Request to save ChvSession : {}", chvSession);
        return chvSessionRepository.save(chvSession);
    }

    @Override
    public ChvSession update(ChvSession chvSession) {
        log.debug("Request to update ChvSession : {}", chvSession);
        chvSession.setIsPersisted();
        return chvSessionRepository.save(chvSession);
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
                if (chvSession.getPeopleItns() != null) {
                    existingChvSession.setPeopleItns(chvSession.getPeopleItns());
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

    @Override
    @Transactional(readOnly = true)
    public Page<ChvSession> findAll(Pageable pageable) {
        log.debug("Request to get all ChvSessions");
        return chvSessionRepository.findAll(pageable);
    }

    public Page<ChvSession> findAllWithEagerRelationships(Pageable pageable) {
        return chvSessionRepository.findAllWithEagerRelationships(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChvSession> findOne(Long id) {
        log.debug("Request to get ChvSession : {}", id);
        return chvSessionRepository.findOneWithEagerRelationships(id);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete ChvSession : {}", id);
        chvSessionRepository.deleteById(id);
    }
}
