package org.nmcpye.datarun.captureshadow;

public record CaptureIdentityResolution(
    CaptureIdentityLink identity,
    boolean inserted
) {
}
