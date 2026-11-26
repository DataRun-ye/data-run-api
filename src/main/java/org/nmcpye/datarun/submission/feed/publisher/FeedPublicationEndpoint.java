package org.nmcpye.datarun.submission.feed.publisher;

import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The admin's first publication and republication (contract 11, feed build step F4), as a management route,
 * {@code /management/feedpublication}, which only an admin reaches (the management routes' rule). It only starts the
 * work: the publisher writes it in batches behind the live backlog, and writes no outbox row.
 *
 * <ul>
 *   <li>{@code GET}: where the first publication stands, and how many republish marks wait.</li>
 *   <li>{@code POST {"action": "first-publication"}}: every submission's head, in id order from the beginning, resumed
 *       from its mark after a restart; refused (409) while one is under way.</li>
 *   <li>{@code POST {"action": "republication"}}, with {@code "formId"} or {@code "submissionIds"} (comma-separated), or
 *       neither for every form: the scope's records are written again at the versions the feed holds, with new
 *       positions; a newer held version is skipped.</li>
 * </ul>
 * <p>Refused (409) while the feed is off. Its answers carry ids and counts only.</p>
 */
@Component
@WebEndpoint(id = "feedpublication")
public class FeedPublicationEndpoint {

    /** HTTP 409: the same request may succeed later. */
    private static final int CONFLICT = 409;

    private final SubmissionPublisher publisher;

    public FeedPublicationEndpoint(SubmissionPublisher publisher) {
        this.publisher = publisher;
    }

    @ReadOperation
    public Map<String, Object> state() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("feedOn", publisher.isOn());
        state.put("firstPublication", publisher.firstPublicationState().map(FeedPublicationEndpoint::describe)
            .orElse(null));
        state.put("republishMarksWaiting", publisher.republishMarksWaiting());
        return state;
    }

    @WriteOperation
    public WebEndpointResponse<Map<String, Object>> start(String action, @Nullable String formId,
                                                          @Nullable String submissionIds) {
        if (!publisher.isOn()) {
            return refused(CONFLICT, "the submission feed is off (submission-feed)");
        }
        return switch (action) {
            case "first-publication" -> {
                if (formId != null || submissionIds != null) {
                    yield refused(WebEndpointResponse.STATUS_BAD_REQUEST, "a first publication has no scope");
                }
                Optional<FirstPublication.State> started = publisher.startFirstPublication();
                yield started.map(state -> answer("firstPublication", describe(state)))
                    .orElseGet(() -> refused(CONFLICT,
                        "a first publication is under way; it goes on"));
            }
            case "republication" -> {
                if (formId != null && submissionIds != null) {
                    yield refused(WebEndpointResponse.STATUS_BAD_REQUEST, "name a form or list submissions, not both");
                }
                List<String> listed = submissionIds == null ? null : Arrays.stream(submissionIds.split(","))
                    .map(String::trim).filter(id -> !id.isEmpty()).toList();
                if (listed != null && listed.isEmpty()) {
                    yield refused(WebEndpointResponse.STATUS_BAD_REQUEST, "no submission listed");
                }
                yield answer("marked", publisher.markForRepublication(formId, listed));
            }
            default -> refused(WebEndpointResponse.STATUS_BAD_REQUEST,
                "the action is first-publication or republication");
        };
    }

    private static Map<String, Object> describe(FirstPublication.State state) {
        Map<String, Object> described = new LinkedHashMap<>();
        described.put("requestedAt", state.requestedAt().toInstant().toString());
        described.put("mark", state.mark());
        described.put("finishedAt", state.finishedAt() == null ? null : state.finishedAt().toInstant().toString());
        described.put("written", state.written());
        described.put("skipped", state.skipped());
        described.put("failed", state.failed());
        return described;
    }

    private static WebEndpointResponse<Map<String, Object>> answer(String name, Object value) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(name, value);
        return new WebEndpointResponse<>(body, WebEndpointResponse.STATUS_OK);
    }

    private static WebEndpointResponse<Map<String, Object>> refused(int status, String reason) {
        return new WebEndpointResponse<>(Map.of("refused", reason), status);
    }
}
