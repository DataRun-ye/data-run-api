package org.nmcpye.datarun.jpa.team.service;

import org.nmcpye.datarun.assignmentshadow.AssignmentAuthorityCommandService;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaIdentifiableService;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.apiquery.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.nmcpye.datarun.jpa.team.repository.TeamSpecifications.getManagedSpecification;

@Service
@Primary
@Transactional
public class DefaultTeamService extends DefaultJpaIdentifiableService<Team> implements TeamService {
    private static final Logger log = LoggerFactory.getLogger(DefaultTeamService.class);

    final private TeamRepository repository;

    private final AssignmentAuthorityCommandService authorityCommands;

    public DefaultTeamService(TeamRepository repository, CacheManager cacheManager,
                              UserAccessService userAccessService,
                              AssignmentAuthorityCommandService authorityCommands) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
        this.authorityCommands = authorityCommands;
    }

    @Override
    public Team saveWithRelations(Team team) {
        return authorityCommands.saveTeam(team);
    }

    @Override
    public Team save(Team team) {
        return authorityCommands.saveTeam(team);
    }

    @Override
    public Team update(Team team) {
        return authorityCommands.updateTeam(team);
    }

    @Override
    public void delete(Team team) {
        authorityCommands.deleteTeam(team);
    }

    @Override
    public void deleteByUid(String uid) {
        authorityCommands.deleteTeam(findByUid(uid).orElseThrow());
    }

//    @Override
//    public Page<Team> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
//        Specification<Team> spec = canRead();
//        if (queryRequest == null || !queryRequest.isIncludeDisabled()) {
//            spec = spec.and(TeamSpecifications.isEnabled());
//        }
//
//        return repository.fetchBagRelationships(repository.findAll(spec, pageable));
//    }
//
//    @Override
//    public List<Team> findAllByUser(QueryRequest queryRequest) {
//        Specification<Team> spec = canRead();
//        if (queryRequest == null || !queryRequest.isIncludeDisabled()) {
//            spec = spec.and(TeamSpecifications.isEnabled());
//        }
//
//        return repository.fetchBagRelationships(repository.findAll(spec));
//    }

    @Override
    public Page<Team> findAllManagedByUser(Pageable pageable, QueryRequest queryRequest) {

//        Specification<Team> specManage = TeamSpecifications.getManagedTeamsByUserTeams(SecurityUtils.getCurrentUserLoginOrThrow(new ErrorMessage(ErrorCode.E3004, getClass().getName()))).and(TeamSpecifications.isEnabled());
//        if (queryRequest == null || !queryRequest.isIncludeDisabled()) {
//            specManage = Specification.where(specManage).and(TeamSpecifications.isEnabled());
//        }
        final var managedSpec = getManagedSpecification(SecurityUtils
                .getCurrentUserDetails().orElseThrow(() ->
                    new IllegalQueryException(new ErrorMessage(ErrorCode.E3004, getClass().getName()))),
            queryRequest);

        return repository.fetchBagRelationships(repository.findAll(managedSpec, pageable));
    }

    @Override
    public Optional<Team> partialUpdate(Team team) {
        log.debug("Request to partially update Team : {}", team);
        return authorityCommands.partialUpdateTeam(team);
    }

}
