package org.nmcpye.datarun.analytics.projection.service;

/**
 * @author Hamza Assada
 * @since 18/09/2025
 */
public class OptionResolution {
    public final String uid;
    public final String labelJson; // e.g. {"en":"X","ar":"Y"}

    public OptionResolution(String uid, String labelJson) {
        this.uid = uid;
        this.labelJson = labelJson;
    }
}
