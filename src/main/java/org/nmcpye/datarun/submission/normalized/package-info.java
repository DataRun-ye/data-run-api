/**
 * Normalized submissions: a stored submission resolved against its pinned form version, with values keyed by field
 * id, rows keyed by the submission's id and the row's id, and an unmapped bucket for values no field keys.
 *
 * <p>Both readers of stored submissions build on it: the feed publisher, and submission review's read model
 * (search, view, history). It never fails on data: what it meets is flagged and counted per submission.</p>
 *
 * <p><b>Name.</b> In DataRun, translation means labels in other languages
 * ({@code org.nmcpye.datarun.common.translation}), so this step is called normalizing.</p>
 *
 * <p><b>Ids</b> are opaque strings: the stored id text, whatever its form (a ULID, the 24-hex id of an older form),
 * never an 11-character uid and never parsed. Ids with nothing stored to carry them are derived by a published recipe
 * ({@link org.nmcpye.datarun.submission.normalized.DerivedIds}).</p>
 */
package org.nmcpye.datarun.submission.normalized;
