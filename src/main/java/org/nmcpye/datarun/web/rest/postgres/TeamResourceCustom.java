package org.nmcpye.datarun.web.rest.postgres;

import jakarta.validation.constraints.NotNull;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.TeamServiceCustom;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.errors.BadRequestAlertException;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;


/**
 * REST controller for managing {@link Team}.
 */
@RestController
@RequestMapping("/api/custom/teams")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class TeamResourceCustom extends AbstractRelationalResource<Team> {
    private final Logger log = LoggerFactory.getLogger(TeamResourceCustom.class);

    private final TeamServiceCustom serviceCustom;

    private final TeamRelationalRepositoryCustom repositoryCustom;

    public TeamResourceCustom(TeamServiceCustom serviceCustom,
                              TeamRelationalRepositoryCustom repositoryCustom) {
        super(serviceCustom, repositoryCustom);
        this.serviceCustom = serviceCustom;
        this.repositoryCustom = repositoryCustom;
    }

    @Override
    protected Specification<Team> buildSpecification(QueryRequest queryRequest) {
        Specification<Team> spec = super.buildSpecification(queryRequest);

        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return spec;
        } else if (SecurityUtils.getCurrentUserLogin().isEmpty()) {
            return null;
        }
        return spec
            .and(serviceCustom.hasAccess());
    }

    @Override
    protected String getName() {
        return "teams";
    }

//    @ExceptionHandler(PathUpdateException.class)
//    public ResponseEntity<String> handlePathUpdateException(PathUpdateException ex) {
//        log.error("Handling PathUpdateException: {}", ex.getMessage());
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
//    }
//
//    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//    @Override
//    public ResponseEntity<Team> updateEntity(Long aLong, Team entity) throws URISyntaxException {
//        return super.updateEntity(aLong, entity);
//    }
//
//    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//    @Override
//    public ResponseEntity<?> saveReturnSaved(Team entity) {
//        log.debug("REST request to saveOne, return saved {}", getName());
//        if (entity.getId() != null) {
//            throw new BadRequestAlertException("A new entity cannot already have an ID", getName() + ":" + entity.getId().toString(), "idexists");
//        }
//        return super.saveReturnSaved(entity);
//    }
//
//    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//    @Override
//    public ResponseEntity<EntitySaveSummaryVM> saveOne(Team entity) {
//        log.debug("REST request to saveOne {}", getName());
//        if (entity.getId() != null) {
//            throw new BadRequestAlertException("A new entity cannot already have an ID", getName() + ":" + entity.getId().toString(), "idexists");
//        }
//        return super.saveOne(entity);
//    }
//
//    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//    @Override
//    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<Team> entities) {
//        log.debug("REST request to saveAll {}", getName());
//        var withIds = entities.stream().filter((entity) -> entity.getId() != null).map(Identifiable::getId).toList();
//        if (!withIds.isEmpty()) {
//            throw new BadRequestAlertException("A new entity cannot already have an ID", getName() + ":" + withIds, "idexists");
//        }
//        return super.saveAll(entities);
//    }

    /**
     * {@code PATCH  /teams/:id} : Partial updates given fields of an existing team, field will ignore if it is null
     *
     * @param uid   the id of the team to save.
     * @param team the team to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated team,
     * or with status {@code 400 (Bad Request)} if the team is not valid,
     * or with status {@code 404 (Not Found)} if the team is not found,
     * or with status {@code 500 (Internal Server Error)} if the team couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/teams/{uid}", consumes = {"application/json", "application/merge-patch+json"})
    public ResponseEntity<Team> partialUpdateTeam(
        @PathVariable(value = "uid", required = false) final String uid,
        @NotNull @RequestBody Team team
    ) throws URISyntaxException {
        log.debug("REST request to partial update Team partially : {}, {}", uid, team);
        if (team.getUid() == null) {
            throw new BadRequestAlertException("Invalid uid", getName(), "idnull");
        }

        if (!Objects.equals(uid, team.getUid())) {
            throw new BadRequestAlertException("Invalid ID", getName(), "idinvalid");
        }

        if (!serviceCustom.existsByUid(uid)) {
            throw new BadRequestAlertException("Entity not found", getName(), "idnotfound");
        }

        Optional<Team> result = serviceCustom.partialUpdate(team);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, getName(), team.getUid())
        );
    }
}
