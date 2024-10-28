package org.nmcpye.datarun.web.rest.postgres;

import org.nmcpye.datarun.drun.common.TeamSpecifications;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.TeamServiceCustom;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.exception.PathUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


/**
 * REST controller for managing {@link Team}.
 */
@RestController
@RequestMapping("/api/custom/teams")
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
    protected Specification<Team> buildSpecification(Map<String, Object> params) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return super.buildSpecification(params);
        } else if (SecurityUtils.getCurrentUserLogin().isEmpty()) {
            return null;
        }
        return super.buildSpecification(params)
            .and(TeamSpecifications.hasUser(SecurityUtils.getCurrentUserLogin().get()));
    }

    @Override
    protected String getName() {
        return "teams";
    }

    /**
     * {@code GET  /Ts} : updatePaths?forceUpdate=true update paths.
     *
     * @param forceUpdate force update paths of all.
     */
    @GetMapping("/updatePaths")
    public ResponseEntity<String> updateTeam(
        @RequestParam(name = "forceUpdate", required = false, defaultValue = "false") boolean forceUpdate) {
        log.debug("REST request to update orgUnit Paths");

        try {
            if (forceUpdate) {
                serviceCustom.forceUpdatePaths();
            } else {
                serviceCustom.updatePaths();
            }
            return ResponseEntity.ok("Paths updated successfully");
        } catch (Exception e) {
            log.error("Error occurred while updating paths", e);
            throw new PathUpdateException("Failed to update paths", e);
        }
    }

    @ExceptionHandler(PathUpdateException.class)
    public ResponseEntity<String> handlePathUpdateException(PathUpdateException ex) {
        log.error("Handling PathUpdateException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }
}
