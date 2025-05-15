package org.nmcpye.datarun.web.rest.v1.paging;

import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <05-05-2025>
 */
public class PagingConfigurator {
    public static <T> PagedResponse<T> initPageResponse(Page<T> page, String next, String resourceName) {
        PagedResponse<T> response = new PagedResponse<>(page, resourceName, next);
        response.setNextPage(next);
//        response.setEntityName(name);
        return response;
    }

    public static String createNextPageLink(Page<?> page) {
        if (page.hasNext()) {
            return ServletUriComponentsBuilder.fromCurrentRequest()
                .queryParam("page", page.getNumber() + 1) // page is 0-based, but we display it 1-based
                .toUriString();
        } else {
            return null;
        }
    }
}
