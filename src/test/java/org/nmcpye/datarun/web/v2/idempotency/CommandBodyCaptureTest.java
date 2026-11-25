package org.nmcpye.datarun.web.v2.idempotency;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.web.v2.caller.ApiCaller;
import org.nmcpye.datarun.web.v2.concurrency.IfMatch;
import org.nmcpye.datarun.web.v2.config.V2Properties;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The body capture holds for a command wherever its controller is declared, and a body that was sent but not
 * captured never hashes as empty.
 */
class CommandBodyCaptureTest {

    /** A command in some feature package, outside web.v2. */
    static class ElsewhereController {
        public void command(CommandRequest command, String body) {
        }

        public void read(String body) {
        }
    }

    @Test
    void theCaptureIsNotLimitedToAPackageOnlyToCommands() throws Exception {
        assertThat(CommandBodyCapture.class.getAnnotation(ControllerAdvice.class).basePackages()).isEmpty();
        assertThat(CommandBodyCapture.class.getAnnotation(ControllerAdvice.class).value()).isEmpty();
        CommandBodyCapture capture = new CommandBodyCapture(new V2Properties());
        MethodParameter command = new MethodParameter(
            ElsewhereController.class.getMethod("command", CommandRequest.class, String.class), 1);
        MethodParameter read = new MethodParameter(ElsewhereController.class.getMethod("read", String.class), 0);
        assertThat(capture.supports(command, String.class, null)).isTrue();
        assertThat(capture.supports(read, String.class, null)).isFalse();
    }

    @Test
    void aBodySentButNotCapturedFailsInsteadOfHashingAsEmpty() {
        MockHttpServletRequest sent = new MockHttpServletRequest("POST", "/api/v2/things/7:touch");
        sent.setContent("{\"by\": 1}".getBytes());
        CommandRequest uncaptured = new CommandRequest(new ApiCaller("u1", Set.of()), UUID.randomUUID(),
            IfMatch.parse(List.of("\"0\"")), sent);
        assertThatThrownBy(uncaptured::fingerprint).isInstanceOf(IllegalStateException.class);

        MockHttpServletRequest chunked = new MockHttpServletRequest("POST", "/api/v2/things/7:touch");
        chunked.addHeader("Transfer-Encoding", "chunked");
        assertThatThrownBy(new CommandRequest(new ApiCaller("u1", Set.of()), UUID.randomUUID(),
            IfMatch.parse(List.of("\"0\"")), chunked)::fingerprint).isInstanceOf(IllegalStateException.class);

        MockHttpServletRequest empty = new MockHttpServletRequest("POST", "/api/v2/things/7:touch");
        assertThat(new CommandRequest(new ApiCaller("u1", Set.of()), UUID.randomUUID(),
            IfMatch.parse(List.of("\"0\"")), empty).fingerprint()).as("no body: nothing to capture").hasSize(32);

        sent.setAttribute(CommandRequest.BODY_ATTRIBUTE, "{\"by\": 1}".getBytes());
        assertThat(new CommandRequest(new ApiCaller("u1", Set.of()), UUID.randomUUID(),
            IfMatch.parse(List.of("\"0\"")), sent).fingerprint()).hasSize(32);
    }
}
