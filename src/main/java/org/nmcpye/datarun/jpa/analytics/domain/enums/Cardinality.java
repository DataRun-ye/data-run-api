package org.nmcpye.datarun.jpa.analytics.domain.enums;

/**
 * @author Hamza Assada
 * @since 11/09/2025
 */
public enum Cardinality {
    ONE_TO_MANY, ONE_TO_ONE, MANY_TO_ONE;

    @Override
    public String toString() {
        switch (this) {
            case ONE_TO_MANY:
                return "1:N";
            case ONE_TO_ONE:
                return "1:1";
            case MANY_TO_ONE:
                return "N:1";
            default:
                return super.toString();
        }
    }
}
