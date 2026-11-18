package org.nmcpye.datarun.jpa.intake;

/**
 * What happens to one checked upload: saved into the real data, or held.
 *
 * @param holdReason null when the upload is saved
 * @param rule       the go-live rule that held it, if any
 * @param context    the held upload's context; null when saved
 */
public record IntakeDecision(HoldReason holdReason, GoLiveRule rule, HeldContext context) {

    private static final IntakeDecision SAVE = new IntakeDecision(null, null, null);

    public static IntakeDecision save() {
        return SAVE;
    }

    public static IntakeDecision hold(HoldReason reason, GoLiveRule rule, HeldContext context) {
        return new IntakeDecision(reason, rule, context);
    }

    public boolean held() {
        return holdReason != null;
    }
}
