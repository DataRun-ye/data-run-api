package org.nmcpye.datarun.config.activation;

/** An operation was asked for while its activation switch is off. A v2 request answers it 422. */
public class OperationNotEnabledException extends RuntimeException {

    private final String switchName;

    public OperationNotEnabledException(String switchName) {
        super("Activation switch off: " + switchName, null, false, false);
        this.switchName = switchName;
    }

    public String switchName() {
        return switchName;
    }
}
