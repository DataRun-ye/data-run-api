package org.nmcpye.datarun.config.activation;

/**
 * Something was asked for while its activation switch is off: a whole method (a v2 request answers 422
 * {@code method-not-enabled}) or one kind of operation within a request (422 {@code operation-not-enabled}).
 */
public class NotEnabledException extends RuntimeException {

    /** What the switch guards. */
    public enum Scope { METHOD, OPERATION }

    private final String switchName;
    private final Scope scope;

    public NotEnabledException(String switchName, Scope scope) {
        super("Activation switch off: " + switchName, null, false, false);
        this.switchName = switchName;
        this.scope = scope;
    }

    public String switchName() {
        return switchName;
    }

    public Scope scope() {
        return scope;
    }
}
