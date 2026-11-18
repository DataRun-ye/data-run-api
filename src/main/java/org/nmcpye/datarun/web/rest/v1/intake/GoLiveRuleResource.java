package org.nmcpye.datarun.web.rest.v1.intake;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.intake.GoLiveRule;
import org.nmcpye.datarun.jpa.intake.GoLiveRuleService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * When real data starts for a campaign, optionally per org unit and per form
 * (DataRun-ye/data-run-api#42). Uploads started before the go-live that
 * applies to them are held; see {@link HeldSubmissionResource}.
 */
@RestController
@RequestMapping("/api/v1/admin/goLiveRules")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
@RequiredArgsConstructor
public class GoLiveRuleResource {

    private final GoLiveRuleService service;

    /**
     * Sets when real data starts for a scope: the campaign, optionally one org
     * unit and everything under it, optionally one form. Setting the same
     * scope again moves its go-live.
     */
    public record GoLiveRuleRequest(String activity, String orgUnit, String form, Instant liveFrom, String note) {
    }

    @GetMapping
    public List<GoLiveRule> list(@RequestParam String activity) {
        return service.list(activity);
    }

    @PutMapping
    public GoLiveRuleService.PutResult put(@RequestBody GoLiveRuleRequest request) {
        return service.put(request.activity(), request.orgUnit(), request.form(), request.liveFrom(), request.note());
    }

    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> delete(@PathVariable String uid) {
        return service.delete(uid) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /** For every form of the campaign, the go-live that applies in {@code orgUnit}, and the rule that sets it. */
    @GetMapping("/effective")
    public List<GoLiveRuleService.EffectiveGoLive> effective(
        @RequestParam String activity,
        @RequestParam(required = false) String orgUnit) {
        return service.effective(activity, orgUnit);
    }
}
