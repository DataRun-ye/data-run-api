package org.nmcpye.datarun.jpa.intake;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmissionIntakeTest {

    private static final Instant GO_LIVE = Instant.parse("2026-11-21T00:00:00Z");
    private static final Instant BEFORE = Instant.parse("2026-11-19T10:00:00Z");
    private static final Instant AFTER = Instant.parse("2026-11-22T10:00:00Z");
    private static final String PATH = ",country0001,districtX01,village00001";

    private GoLiveRuleRepository rules;
    private DataSubmissionRepository submissions;
    private SubmissionIntake intake;
    private DataSubmission submission;

    @BeforeEach
    void setUp() {
        rules = mock(GoLiveRuleRepository.class);
        submissions = mock(DataSubmissionRepository.class);
        intake = new SubmissionIntake(rules, mock(HeldSubmissionRepository.class), submissions);

        submission = new DataSubmission();
        submission.setUid("submission1");
        submission.setActivity("campaign001");
        submission.setOrgUnit("village00001");
        submission.setForm("household1");
        submission.setFormVersion("version0001");
        submission.setTeam("team0000001");
        submission.setAssignment("assignment1");
        submission.setStartEntryTime(BEFORE);

        when(rules.findByActivity("campaign001")).thenReturn(List.of(
            new GoLiveRule("ruleCampain", "campaign001", null, null, GO_LIVE, null, "admin", GO_LIVE)));
    }

    @Test
    void anUploadStartedBeforeTheGoLiveIsHeldWithItsContext() {
        IntakeDecision decision = intake.decide(IntakeMode.LIVE, submission, PATH, "field-user", AFTER);

        assertThat(decision.held()).isTrue();
        assertThat(decision.holdReason()).isEqualTo(HoldReason.BEFORE_GO_LIVE);
        assertThat(decision.rule().uid()).isEqualTo("ruleCampain");
        assertThat(decision.context()).isEqualTo(new HeldContext(
            "campaign001", "village00001", PATH, "household1", "version0001",
            "team0000001", "assignment1", "field-user", BEFORE, AFTER));
    }

    @Test
    void anUploadStartedAfterTheGoLiveIsSaved() {
        submission.setStartEntryTime(AFTER);

        assertThat(intake.decide(IntakeMode.LIVE, submission, PATH, "field-user", AFTER).held()).isFalse();
    }

    @Test
    void anUpdateOfRealDataIsNeverHeld() {
        when(submissions.existsByUid("submission1")).thenReturn(true);

        assertThat(intake.decide(IntakeMode.LIVE, submission, PATH, "field-user", AFTER).held()).isFalse();
    }

    @Test
    void aCampaignWithoutRulesSavesEverything() {
        when(rules.findByActivity("campaign001")).thenReturn(List.of());

        assertThat(intake.decide(IntakeMode.LIVE, submission, PATH, "field-user", AFTER).held()).isFalse();
        verify(submissions, never()).existsByUid("submission1");
    }

    @Test
    void practiceIsAlwaysHeldAndReleaseAlwaysSaved() {
        submission.setStartEntryTime(AFTER);

        IntakeDecision practice = intake.decide(IntakeMode.PRACTICE, submission, PATH, "field-user", AFTER);
        assertThat(practice.holdReason()).isEqualTo(HoldReason.PRACTICE);
        assertThat(practice.rule()).isNull();

        submission.setStartEntryTime(BEFORE);
        assertThat(intake.decide(IntakeMode.RELEASE, submission, PATH, "field-user", AFTER).held()).isFalse();
    }

    @Test
    void thePathIsStoredWithTheOrgUnitItselfEvenWhenItWasMissing() {
        assertThat(SubmissionIntake.areaChain("village00001", ",country0001,districtX01"))
            .containsExactly("village00001", "districtX01", "country0001");
        assertThat(SubmissionIntake.pathOf(SubmissionIntake.areaChain("village00001", ",country0001,districtX01")))
            .isEqualTo(PATH);
        assertThat(SubmissionIntake.areaChain("village00001", PATH))
            .containsExactly("village00001", "districtX01", "country0001");
    }
}
