package org.nmcpye.datarun.web.v2.caller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.v2.problem.ApiProblemException;
import org.nmcpye.datarun.web.v2.problem.ProblemCode;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The caller is the account the v2 chain loaded, by durable id; anything else is not a caller. */
class ApiCallerTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void theCallerIsTheLoadedAccountsIdAndCurrentAuthorities() {
        CurrentUserDetails account = mock(CurrentUserDetails.class);
        when(account.getId()).thenReturn("01JDQ3ZK5V8Y7M2N4P6R8T0W2X");
        doReturn(AuthorityUtils.createAuthorityList("ROLE_USER")).when(account).getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(account, "token", account.getAuthorities()));

        ApiCaller caller = ApiCaller.current();

        assertThat(caller.userId()).isEqualTo("01JDQ3ZK5V8Y7M2N4P6R8T0W2X");
        assertThat(caller.hasAuthority("ROLE_USER")).isTrue();
        assertThat(caller.hasAuthority("ROLE_ADMIN")).isFalse();
    }

    @Test
    void noAccountOrAnotherKindOfPrincipalIsNoCaller() {
        assertUnauthenticated();
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", "anonymous",
            AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertUnauthenticated();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            new User("someone", "", AuthorityUtils.NO_AUTHORITIES), "", AuthorityUtils.NO_AUTHORITIES));
        assertUnauthenticated();
    }

    private static void assertUnauthenticated() {
        ApiProblemException problem = catchThrowableOfType(ApiProblemException.class, ApiCaller::current);
        assertThat(problem).isNotNull();
        assertThat(problem.code()).isEqualTo(ProblemCode.UNAUTHENTICATED);
    }
}
