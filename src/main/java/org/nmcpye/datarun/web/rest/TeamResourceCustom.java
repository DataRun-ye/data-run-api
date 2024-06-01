package org.nmcpye.datarun.web.rest;

import org.nmcpye.datarun.domain.Team;
import org.nmcpye.datarun.repository.TeamRepositoryCustom;
import org.nmcpye.datarun.service.custom.TeamServiceCustom;
import org.nmcpye.datarun.web.rest.common.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.web.util.ResponseUtil;

import java.util.Optional;


/**
 * REST controller for managing {@link Team}.
 */
@RestController
@RequestMapping("/api/custom/teams")
public class TeamResourceCustom extends AbstractResource<Team> {
    private final Logger log = LoggerFactory.getLogger(TeamResourceCustom.class);

    private final TeamServiceCustom teamService;

    private final TeamRepositoryCustom teamRepository;

    public TeamResourceCustom(TeamServiceCustom teamServiceCustom,
                              TeamRepositoryCustom teamRepositoryCustom) {
        this.teamService = teamServiceCustom;
        this.teamRepository = teamRepositoryCustom;
    }

    @Override
    protected Page<Team> getList(Pageable pageable, boolean eagerload) {
        if (eagerload) {
            return teamService.findAllWithEagerRelationships(pageable);
        } else {
            return teamService.findAll(pageable);
        }
    }

    @Override
    protected String getName() {
        return "teams";
    }

    /**
     * {@code GET  /teams/:id} : get the "id" team.
     *
     * @param id the id of the team to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the team, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Team> getOneByUser(@PathVariable("id") Long id) {
        log.debug("REST request to get Team : {}", id);
        Optional<Team> team = teamService.findOne(id);
        return ResponseUtil.wrapOrNotFound(team);
    }

}
