/**
 * The seam between the runtime that claims outbox rows and the outputs it applies their changes to: {@link
 * org.nmcpye.datarun.outbox.processing.ChangeOutput} for each change, {@link
 * org.nmcpye.datarun.outbox.processing.DeferredWork} for the work an output keeps for when no change is due. The runtime
 * depends on this package and the outputs implement it; neither depends on the other.
 */
package org.nmcpye.datarun.outbox.processing;
