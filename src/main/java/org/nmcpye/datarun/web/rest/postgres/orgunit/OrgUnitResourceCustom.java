package org.nmcpye.datarun.web.rest.postgres.orgunit;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.OrgUnitService;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.exception.PathUpdateException;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;
import java.util.List;

/**
 * REST Extended controller for managing {@link OrgUnit}.
 */
@RestController
@RequestMapping("/api/custom/orgUnits")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class OrgUnitResourceCustom extends JpaBaseResource<OrgUnit> {

    private final Logger log = LoggerFactory.getLogger(OrgUnitResourceCustom.class);

    private final OrgUnitService serviceCustom;

    private final OrgUnitRepositoryCustom repositoryCustom;

    public OrgUnitResourceCustom(OrgUnitService serviceCustom, OrgUnitRepositoryCustom repositoryCustom) {
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
            throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1003, e.getMessage()));
        }
    }

    @ExceptionHandler(PathUpdateException.class)
    public ResponseEntity<String> handlePathUpdateException(PathUpdateException ex) {
        log.error("Handling PathUpdateException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<OrgUnit> updateEntity(String uid, OrgUnit entity) throws URISyntaxException {
        return super.updateEntity(uid, entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<?> saveReturnSaved(OrgUnit entity) {
        return super.saveReturnSaved(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveOne(OrgUnit entity) {
        return super.saveOne(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<OrgUnit> entities) {
        return super.saveAll(entities);
    }
}
