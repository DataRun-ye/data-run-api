package org.nmcpye.datarun.web.rest.postgres.orgunit;

import org.nmcpye.datarun.apiquery.QueryRequest;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.jpa.orgunit.service.OrgUnitService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.web.rest.exception.PathUpdateException;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.nmcpye.datarun.web.rest.v1.paging.PagingConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(OrgUnitV1Resource.V1)
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class OrgUnitV1Resource extends JpaBaseResource<OrgUnit> {

    static final String V1 = ApiVersion.API_V1 + "/orgUnits";

    private final Logger log = LoggerFactory.getLogger(OrgUnitV1Resource.class);
    private final OrgUnitService orgUnitService;

    public OrgUnitV1Resource(
        OrgUnitService orgUnitService,
        OrgUnitRepository orgUnitRepository
    ) {
        super(orgUnitService, orgUnitRepository);
        this.orgUnitService = orgUnitService;
    }

    @Override
    @GetMapping("")
    protected ResponseEntity<PagedResponse<?>> getAll(QueryRequest queryRequest) {
        Page<OrgUnit> processedPage =
            orgUnitService.findAllReleasedWork(queryRequest, null);
        String next = PagingConfigurator.createNextPageLink(processedPage);
        PagedResponse<OrgUnit> response = PagingConfigurator.initPageResponse(
            processedPage,
            next,
            getName()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/updatePaths")
    public ResponseEntity<String> updatePaths(
        @RequestParam(name = "forceUpdate", required = false, defaultValue = "false")
        boolean forceUpdate
    ) {
        var user = SecurityUtils.getCurrentUserDetailsOrThrow();
        requireResourceApiAccess(user);
        log.debug("REST request to update orgUnit paths");
        try {
            if (forceUpdate) {
                orgUnitService.forceUpdatePaths();
            } else {
                orgUnitService.updatePaths();
            }
            return ResponseEntity.ok("Paths updated successfully");
        } catch (Exception exception) {
            log.error("Error occurred while updating paths", exception);
            throw new IllegalQueryException(
                new ErrorMessage(ErrorCode.E1003, exception.getMessage())
            );
        }
    }

    @ExceptionHandler(PathUpdateException.class)
    public ResponseEntity<String> handlePathUpdateException(
        PathUpdateException exception
    ) {
        log.error("Handling PathUpdateException: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(exception.getMessage());
    }

    @Override
    protected String getName() {
        return "orgUnits";
    }
}
