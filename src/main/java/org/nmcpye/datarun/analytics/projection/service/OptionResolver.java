package org.nmcpye.datarun.analytics.projection.service;

/**
 * @author Hamza Assada
 * @since 18/09/2025
 */
public interface OptionResolver {
   OptionResolution resolve(String optionSetUid, String codeOrUid);
}
