package org.nmcpye.datarun.web.rest.postgres;

import org.nmcpye.datarun.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.TeamServiceCustom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * REST controller for managing {@link Team}.
 */
@RestController
@RequestMapping("/api/custom/teams")
public class TeamResourceCustom extends AbstractRelationalResource<Team> {
    private final Logger log = LoggerFactory.getLogger(TeamResourceCustom.class);

    private final TeamServiceCustom teamService;

    private final TeamRelationalRepositoryCustom teamRepository;

    public TeamResourceCustom(TeamServiceCustom teamServiceCustom,
                              TeamRelationalRepositoryCustom teamRepositoryCustom) {
        super(teamServiceCustom, teamRepositoryCustom);
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
}
