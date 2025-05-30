package org.nmcpye.datarun.web.rest.postgres.usergroup;

import org.nmcpye.datarun.usegroup.UserGroup;
import org.nmcpye.datarun.usegroup.repository.UserGroupRepository;
import org.nmcpye.datarun.drun.postgres.service.UserGroupService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.nmcpye.datarun.web.rest.postgres.usergroup.UserGroupResource.CUSTOM;
import static org.nmcpye.datarun.web.rest.postgres.usergroup.UserGroupResource.V1;


/**
 * REST controller for managing {@link UserGroup}.
 */
@RestController
@RequestMapping(value = {CUSTOM, V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class UserGroupResource extends JpaBaseResource<UserGroup> {
    protected static final String NAME = "/userGroups";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    private final UserGroupService service;

    public UserGroupResource(UserGroupService service,
                             UserGroupRepository repository) {
        super(service, repository);
        this.service = service;
    }

    @Override
    protected String getName() {
        return "userGroups";
    }

    @GetMapping("managed")
    protected ResponseEntity<PagedResponse<?>> getAllManaged(
        QueryRequest queryRequest) {
        Pageable pageable = queryRequest.getPageable();

        Page<UserGroup> processedPage = service.findAllManagedByUser(pageable);

        String next = createNextPageLink(processedPage);

        PagedResponse<UserGroup> response = initPageResponse(processedPage, next);
        return ResponseEntity.ok(response);
    }
}
