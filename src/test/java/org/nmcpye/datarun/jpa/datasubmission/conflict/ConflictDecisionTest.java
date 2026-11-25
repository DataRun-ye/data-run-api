package org.nmcpye.datarun.jpa.datasubmission.conflict;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.datasubmission.revision.SubmissionRevisionReader.Revision;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongFunction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Conflict detection's decision on plain values alone (spec, Conflicts): revisions, states and copies, with no
 * database, no submission entity and no access snapshot. Invented values.
 */
class ConflictDecisionTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String UPLOADER = "user1";
    private static final String OTHER = "user2";

    // ---------------------------------------------------------------- uploader with a base of their own

    @Test
    void anUnchangedReSendAfterAServerSideChangeIsAnsweredBeforeTheChecks() {
        ConflictCheck check = decide(copy("first"), stored("edited", false, false),
            List.of(upload(0, UPLOADER), edit(1)), Map.of(0L, state("first")));

        assertThat(check.answeredBeforeChecks()).isTrue();
    }

    @Test
    void aChangedCopyAfterAServerSideChangeIsKeptAside() {
        ConflictCheck.Analysis analysis = analyse(copy("changed"), stored("edited", false, false),
            List.of(upload(0, UPLOADER), edit(1)), Map.of(0L, state("first")));
        ConflictCheck check = ConflictCheck.analysed(null, context(copy("changed"), stored("edited", false, false)),
            analysis);

        assertThat(analysis.ownBase()).isTrue();
        assertThat(analysis.base()).isZero();
        assertThat(analysis.serverSideChange()).isTrue();
        assertThat(check.answeredBeforeChecks()).isFalse();
        assertThat(check.afterChecks()).isEqualTo(ConflictCheck.Outcome.KEEP_ASIDE);
    }

    @Test
    void withoutAServerSideChangeAChangedCopyApplies() {
        ConflictCheck check = decide(copy("changed"), stored("second", false, false),
            List.of(upload(0, UPLOADER), upload(1, UPLOADER)), Map.of(1L, state("second")));

        assertThat(check.answeredBeforeChecks()).isFalse();
        assertThat(check.afterChecks()).isEqualTo(ConflictCheck.Outcome.APPLY);
    }

    /** The form version is not compared, since an update never writes it: the copy kept is the same copy. */
    @Test
    void aCopyNamingAnotherFormVersionWithTheSameDataIsTheCopyKept() {
        PhoneCopy copy = new PhoneCopy(UploadRoute.VERSIONED, item("changed", "version2"), state("changed"), false);
        ConflictDetection.Facts open = new ConflictDetection.Facts(List.of(upload(0, UPLOADER), edit(1)), Set.of(),
            Optional.of(item("changed", "version1").toString()), Optional.empty());

        assertThat(ConflictDetection.analysis(context(copy, stored("edited", false, false)), open,
            states(Map.of(0L, state("first")))).open()).isEqualTo(ConflictCheck.OpenCopy.SAME);
    }

    // ---------------------------------------------------------------- uploader without a base of their own

    @Test
    void anUploaderWithoutABaseIsComparedFromTheFirstRevision() {
        List<Revision> created = List.of(upload(0, OTHER));
        List<Revision> edited = List.of(upload(0, OTHER), edit(1));

        ConflictCheck.Analysis analysis = analyse(copy("first"), stored("first", false, false), created, Map.of());
        assertThat(analysis.ownBase()).isFalse();
        assertThat(analysis.base()).as("the first revision").isZero();
        assertThat(decide(copy("first"), stored("first", false, false), created, Map.of()).afterChecks())
            .as("equal to the current state").isEqualTo(ConflictCheck.Outcome.NOTHING);
        assertThat(decide(copy("changed"), stored("first", false, false), created, Map.of()).afterChecks())
            .isEqualTo(ConflictCheck.Outcome.APPLY);
        assertThat(decide(copy("first"), stored("edited", false, false), edited, Map.of()).afterChecks())
            .as("equal to the first revision, but a server-side change since")
            .isEqualTo(ConflictCheck.Outcome.KEEP_ASIDE);
    }

    @Test
    void aCreatorsLiveBaselineIsTheirBase() {
        List<Revision> baselineOnly = List.of(baseline(3, false));

        assertThat(analyse(copy("first"), stored("first", false, true), baselineOnly, Map.of(3L, state("first")))
            .ownBase()).isTrue();
        assertThat(analyse(copy("first"), stored("first", false, false), baselineOnly, Map.of()).ownBase())
            .as("not the creator").isFalse();
    }

    /** Deleted before the journal began: the deleted first baseline counts for every uploader, its creator included. */
    @Test
    void aDeletedFirstBaselineIsAServerSideChangeForEveryUploader() {
        ConflictCheck.Analysis analysis = analyse(copy("first"), stored("first", true, true),
            List.of(baseline(3, true)), Map.of());

        assertThat(analysis.ownBase()).isFalse();
        assertThat(analysis.serverSideChange()).isTrue();
        assertThat(decide(copy("first"), stored("first", true, true), List.of(baseline(3, true)), Map.of())
            .afterChecks()).isEqualTo(ConflictCheck.Outcome.KEEP_ASIDE);
    }

    // ---------------------------------------------------------------- the move rule and an open conflict

    @Test
    void anAccessRefusalIsKeptAsideOnlyAfterAMoveSinceTheUploadersOwnBase() {
        List<Revision> moved = List.of(upload(0, UPLOADER), move(1));
        ConflictCheck afterAMove = decide(copy("changed"), stored("first", false, false), moved,
            Map.of(0L, state("first")));

        assertThat(afterAMove.keepsAsideOnRefusal(AccessRefusal.OUT_OF_SCOPE)).isTrue();
        assertThat(afterAMove.keepsAsideOnRefusal(AccessRefusal.FORM_NOT_PERMITTED)).isTrue();
        assertThat(afterAMove.keepsAsideOnRefusal(null)).as("any other refusal").isFalse();
        assertThat(decide(copy("changed"), stored("edited", false, false), List.of(upload(0, UPLOADER), edit(1)),
            Map.of(0L, state("first"))).keepsAsideOnRefusal(AccessRefusal.OUT_OF_SCOPE)).as("no move").isFalse();
        assertThat(decide(copy("changed"), stored("first", false, false), List.of(upload(0, OTHER), move(1)),
            Map.of()).keepsAsideOnRefusal(AccessRefusal.OUT_OF_SCOPE)).as("no base of their own").isFalse();
    }

    /** While a conflict of theirs is open, it comes first: even an unchanged re-send replaces its copy. */
    @Test
    void anOpenConflictComesFirst() {
        StoredState stored = stored("edited", false, false);
        List<Revision> revisions = List.of(upload(0, UPLOADER), edit(1));
        PhoneCopy unchanged = copy("first");
        ConflictDetection.Facts openWithAnother = new ConflictDetection.Facts(revisions, Set.of(),
            Optional.of(item("changed", "version1").toString()), Optional.empty());

        ConflictCheck.Analysis analysis = ConflictDetection.analysis(context(unchanged, stored), openWithAnother,
            states(Map.of(0L, state("first"))));
        ConflictCheck check = ConflictCheck.analysed(null, context(unchanged, stored), analysis);

        assertThat(analysis.open()).isEqualTo(ConflictCheck.OpenCopy.DIFFERENT);
        assertThat(check.answeredBeforeChecks()).isFalse();
        assertThat(check.afterChecks()).isEqualTo(ConflictCheck.Outcome.KEEP_ASIDE);
        assertThat(ConflictDetection.analysis(context(unchanged, stored), new ConflictDetection.Facts(revisions,
                Set.of(), Optional.of(item("first", "version1").toString()), Optional.empty()),
            states(Map.of(0L, state("first")))).open()).isEqualTo(ConflictCheck.OpenCopy.SAME);
    }

    @Test
    void aFailedDetectionKeepsTheCopyAsideAndLetsEveryRefusalStand() {
        ConflictCheck failed = ConflictCheck.failed(context(copy("changed"), stored("first", false, false)), null);

        assertThat(failed.answeredBeforeChecks()).isFalse();
        assertThat(failed.keepsAsideOnRefusal(AccessRefusal.OUT_OF_SCOPE)).isFalse();
        assertThat(failed.afterChecks()).isEqualTo(ConflictCheck.Outcome.KEEP_ASIDE);
        assertThat(ConflictCheck.OFF.afterChecks()).isEqualTo(ConflictCheck.Outcome.APPLY);
    }

    // ---------------------------------------------------------------- values

    private static ConflictCheck decide(PhoneCopy copy, StoredState stored, List<Revision> revisions,
                                        Map<Long, ComparedState> states) {
        return ConflictCheck.analysed(null, context(copy, stored), analyse(copy, stored, revisions, states));
    }

    private static ConflictCheck.Analysis analyse(PhoneCopy copy, StoredState stored, List<Revision> revisions,
                                                  Map<Long, ComparedState> states) {
        return ConflictDetection.analysis(context(copy, stored),
            new ConflictDetection.Facts(revisions, Set.of(), Optional.empty(), Optional.empty()), states(states));
    }

    private static LongFunction<ComparedState> states(Map<Long, ComparedState> states) {
        return version -> {
            ComparedState state = states.get(version);
            if (state == null) {
                throw new IllegalStateException("No state at revision " + version);
            }
            return state;
        };
    }

    private static ConflictDetection.Context context(PhoneCopy copy, StoredState stored) {
        return new ConflictDetection.Context(copy, stored, UPLOADER);
    }

    private static JsonNode document(String name) {
        ObjectNode main = JSON.createObjectNode().put("name", name);
        return JSON.createObjectNode().set("main", main);
    }

    private static ComparedState state(String name) {
        return new ComparedState(document(name), false, "assignment1", null);
    }

    private static ObjectNode item(String name, String formVersion) {
        ObjectNode item = JSON.createObjectNode().put("uid", "submission1").put("formVersion", formVersion)
            .put("assignment", "assignment1").put("deleted", false);
        item.set("formData", document(name));
        return item;
    }

    private static PhoneCopy copy(String name) {
        return new PhoneCopy(UploadRoute.VERSIONED, item(name, "version1"), state(name), false);
    }

    private static StoredState stored(String name, boolean deleted, boolean uploaderIsCreator) {
        return new StoredState("submission1-id", 1, document(name).toString(), deleted, "assignment1", null,
            uploaderIsCreator);
    }

    private static Revision upload(long version, String actor) {
        return new Revision(version, version == 0 ? "CREATE" : "UPDATE", "UPLOAD", actor, null, false);
    }

    private static Revision edit(long version) {
        return new Revision(version, "UPDATE", "EDIT", "admin1", null, false);
    }

    private static Revision move(long version) {
        return new Revision(version, "UPDATE", "MOVE", "admin1", null, false);
    }

    private static Revision baseline(long version, boolean deleted) {
        return new Revision(version, "BASELINE", "MIGRATION", null, null, deleted);
    }
}
