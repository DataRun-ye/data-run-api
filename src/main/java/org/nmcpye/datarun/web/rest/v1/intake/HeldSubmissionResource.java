package org.nmcpye.datarun.web.rest.v1.intake;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.datasubmission.command.HeldDecisionResult;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionActor;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionCommand;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionCommandService;
import org.nmcpye.datarun.jpa.intake.HeldFilter;
import org.nmcpye.datarun.jpa.intake.HeldState;
import org.nmcpye.datarun.jpa.intake.HeldSubmissionRepository;
import org.nmcpye.datarun.jpa.intake.HeldSubmissionRow;
import org.nmcpye.datarun.jpa.intake.HeldSummaryRow;
import org.nmcpye.datarun.jpa.intake.HoldReason;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Uploads kept out of the real data until an admin decides
 * (DataRun-ye/data-run-api#42): practice uploads, and uploads started before
 * their campaign's go-live. Filters are optional; {@code orgUnit} includes
 * everything under it, and {@code enteredFrom}/{@code enteredTo} compare the
 * earlier of when the form was started on the phone and when it arrived.
 */
@RestController
@RequestMapping("/api/v1/admin/heldSubmissions")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
@RequiredArgsConstructor
public class HeldSubmissionResource {

    private static final int MAX_PAGE_SIZE = 500;

    private final HeldSubmissionRepository held;
    private final HeldSubmissionReview review;
    private final SubmissionCommandService commands;

    /** Counts by org unit, form, reason and state, with the first and last entry times. */
    @GetMapping("/summary")
    public List<HeldSummaryRow> summary(
        @RequestParam(required = false) String activity,
        @RequestParam(required = false) String orgUnit,
        @RequestParam(required = false) String form,
        @RequestParam(required = false) HoldReason reason,
        @RequestParam(required = false) Set<HeldState> state,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant enteredFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant enteredTo) {
        return held.summary(new HeldFilter(activity, orgUnit, form, reason, state, enteredFrom, enteredTo));
    }

    /** Newest first, without the stored uploads. */
    @GetMapping
    public List<HeldSubmissionRow> list(
        @RequestParam(required = false) String activity,
        @RequestParam(required = false) String orgUnit,
        @RequestParam(required = false) String form,
        @RequestParam(required = false) HoldReason reason,
        @RequestParam(required = false) Set<HeldState> state,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant enteredFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant enteredTo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size) {
        return held.find(
            new HeldFilter(activity, orgUnit, form, reason, state, enteredFrom, enteredTo),
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    /** One held upload, with the upload as received. */
    @GetMapping("/{uid}")
    public ResponseEntity<HeldSubmissionRow> get(@PathVariable String uid) {
        return ResponseEntity.of(held.findByUid(uid));
    }

    @PostMapping("/release")
    public HeldDecisionResult release(@RequestBody HeldDecisionRequest request) {
        return review.release(request);
    }

    @PostMapping("/discard")
    public HeldDecisionResult discard(@RequestBody HeldDecisionRequest request) {
        return commands.discardHeld(new SubmissionCommand.DiscardHeld(SubmissionActor.current(), request));
    }
}
