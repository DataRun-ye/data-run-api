package org.nmcpye.datarun.web.rest.postgres.team;

import jakarta.validation.constraints.NotNull;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.TeamRepository;
import org.nmcpye.datarun.drun.postgres.service.TeamService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.web.rest.errors.BadRequestAlertException;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
public class TeamResource extends JpaBaseResource<Team> {
    private final Logger log = LoggerFactory.getLogger(TeamResource.class);

    private final TeamService teamService;

    private final TeamRepository teamRepository;

    public TeamResource(TeamService teamService,
                        TeamRepository teamRepository) {
        super(teamService, teamRepository);
        this.teamService = teamService;
        this.teamRepository = teamRepository;
    }

    @Override
    protected String getName() {
        return "teams";
    }

    @GetMapping("managed")
    protected ResponseEntity<PagedResponse<?>> getAllManaged(
        QueryRequest queryRequest) {
        Pageable pageable = PageRequest.of(queryRequest.getPage(), queryRequest.getSize());

        if (!queryRequest.isPaged()) {
            pageable = Pageable.unpaged();
        }

        Page<Team> processedPage = teamService.findAllManagedByUser(pageable, queryRequest);

        String next = createNextPageLink(processedPage);

        PagedResponse<Team> response = initPageResponse(processedPage, next);
        return ResponseEntity.ok(response);
    }

    /**
     * {@code PATCH  /teams/:id} : Partial updates given fields of an existing team, field will ignore if it is null
     *
     * @param uid  the id of the team to save.
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

        if (!teamService.existsByUid(uid)) {
            throw new BadRequestAlertException("Entity not found", getName(), "idnotfound");
        }

        Optional<Team> result = teamService.partialUpdate(team);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, getName(), team.getUid())
        );
    }

    @GetMapping("/migrate")
    public ResponseEntity<String> updatePaths() throws Exception {
        log.info("REST request to migrate team form permissions");
        teamService.runFormPermissionsMigration();

        return ResponseEntity.ok("Paths updated successfully");
    }
}
