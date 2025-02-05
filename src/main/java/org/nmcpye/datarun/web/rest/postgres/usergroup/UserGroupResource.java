package org.nmcpye.datarun.web.rest.postgres.usergroup;

import org.nmcpye.datarun.drun.postgres.domain.UserGroup;
import org.nmcpye.datarun.drun.postgres.repository.UserGroupRepository;
import org.nmcpye.datarun.drun.postgres.service.UserGroupService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.nmcpye.datarun.web.rest.postgres.AbstractRelationalResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * REST controller for managing {@link UserGroup}.
 */
@RestController
@RequestMapping("/api/custom/userGroups")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class UserGroupResource extends AbstractRelationalResource<UserGroup> {
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
        Pageable pageable = PageRequest.of(queryRequest.getPage(), queryRequest.getSize());

        if (!queryRequest.isPaged()) {
            pageable = Pageable.unpaged();
        }

        Page<UserGroup> processedPage = service.findAllManagedByUser(pageable);

        String next = createNextPageLink(processedPage);

        PagedResponse<UserGroup> response = initPageResponse(processedPage, next);
        return ResponseEntity.ok(response);
    }
}
