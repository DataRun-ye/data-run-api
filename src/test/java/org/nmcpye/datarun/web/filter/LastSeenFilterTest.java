package org.nmcpye.datarun.web.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.user.service.LastSeenService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LastSeenFilterTest {

    private final LastSeenService lastSeenService = mock(LastSeenService.class);
    private final LastSeenFilter filter = new LastSeenFilter(lastSeenService);

    @BeforeEach
    void signIn() {
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.getId()).thenReturn("user000001");
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(user, null, "ROLE_USER"));
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsAppVersionAndReferenceVersionOnTheAssignmentsSync() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/assignments/forms");
        request.setParameter("paged", "false");
        request.setParameter("referenceVersion", "1");
        request.addHeader("X-App-Version", "6.1.0+55");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(lastSeenService).touch(eq("user000001"), any(Instant.class), eq("6.1.0+55"), eq(1));
    }

    @Test
    void recordsZeroWhenAnOldAppSyncsAssignmentsWithoutReferenceVersion() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/assignments/forms");
        request.setParameter("paged", "false");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(lastSeenService).touch(eq("user000001"), any(Instant.class), isNull(), eq(0));
    }

    @Test
    void leavesReferenceVersionUnknownOnOtherRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/myDetails");
        request.addHeader("X-App-Version", "6.1.0+55");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(lastSeenService).touch(eq("user000001"), any(Instant.class), eq("6.1.0+55"), isNull());
    }

    @Test
    void ignoresAMalformedAppVersion() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/myDetails");
        request.addHeader("X-App-Version", "6.1.0 <script>");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(lastSeenService).touch(eq("user000001"), any(Instant.class), isNull(), isNull());
    }
}
