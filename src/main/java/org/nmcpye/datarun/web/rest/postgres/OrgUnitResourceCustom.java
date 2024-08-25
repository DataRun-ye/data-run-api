package org.nmcpye.datarun.web.rest.postgres;

import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.OrgUnitServiceCustom;
import org.nmcpye.datarun.web.rest.exception.PathUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Extended controller for managing {@link OrgUnit}.
 */
@RestController
@RequestMapping("/api/custom/orgUnits")
public class OrgUnitResourceCustom extends AbstractRelationalResource<OrgUnit> {

    private final Logger log = LoggerFactory.getLogger(OrgUnitResourceCustom.class);

    private final OrgUnitServiceCustom serviceCustom;

    private final OrgUnitRelationalRepositoryCustom repositoryCustom;

    public OrgUnitResourceCustom(OrgUnitServiceCustom serviceCustom, OrgUnitRelationalRepositoryCustom repositoryCustom) {
        super(serviceCustom, repositoryCustom);
        this.repositoryCustom = repositoryCustom;
        this.serviceCustom = serviceCustom;
    }

    @Override
    protected String getName() {
        return "orgUnits";
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
