package org.nmcpye.datarun.web.rest.errors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exception-translator-test")
public class ExceptionTranslatorTestController {

    @GetMapping("/concurrency-failure")
    public void concurrencyFailure() {
        throw new ConcurrencyFailureException("test concurrency failure");
    }

    @PostMapping("/method-argument")
    public void methodArgument(@Valid @RequestBody TestDTO testDTO) {}

    @GetMapping("/missing-servlet-request-part")
    public void missingServletRequestPartException(@RequestPart("part") String part) {}

    @GetMapping("/missing-servlet-request-parameter")
    public void missingServletRequestParameterException(@RequestParam("param") String param) {}

    @GetMapping("/access-denied")
    public void accessdenied() {
        throw new AccessDeniedException("test access denied!");
    }

    @GetMapping("/unauthorized")
    public void unauthorized() {
        throw new BadCredentialsException("test authentication failed!");
    }

    @GetMapping("/response-status")
    public void exceptionWithResponseStatus() {
        throw new TestResponseStatusException();
    }

    @GetMapping("/internal-server-error")
    public void internalServerError() {
        throw new RuntimeException();
    }

    /** A database failure whose messages quote values, as PostgreSQL's and Hibernate's do. */
    @GetMapping("/database-failure")
    public void databaseFailure() {
        throw new DataIntegrityViolationException("could not execute statement [insert ... 'secret-sql-7']",
            new PSQLException(new ServerErrorMessage("SERROR\0C23505\0"
                + "Mduplicate key value violates unique constraint \"uk_test_name\"\0"
                + "DKey (name)=(secret-detail-7) already exists.\0nuk_test_name\0tdata_test\0")));
    }

    @PostMapping("/method-argument-size")
    public void methodArgumentSize(@Valid @RequestBody SizedDTO sizedDTO) {}

    public static class SizedDTO {

        @Size(max = 3)
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    public static class TestDTO {

        @NotNull
        private String test;

        public String getTest() {
            return test;
        }

        public void setTest(String test) {
            this.test = test;
        }
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "test response status")
    @SuppressWarnings("serial")
    public static class TestResponseStatusException extends RuntimeException {}
}
