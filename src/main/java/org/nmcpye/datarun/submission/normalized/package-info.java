/**
 * Normalized submissions: a stored submission resolved against its pinned form version, with values keyed by field
 * id, rows keyed by the submission's id and the row's id, and an unmapped bucket for values no field keys.
 *
 * <p>It is step S1 of submission review ({@code .review/data-chain/2026-11-25-e-design.md} §9), shared by the feed
 * publisher (step D) and submission review's read model (search, view, history). Its rules are the D feed spike's,
 * measured on every head of the staging clone ({@code 2026-11-25-d-spike-phase1.md} §2).</p>
 *
 * <p><b>Name.</b> The design notes call this step "the translation". DataRun already uses translation for labels in
 * other languages ({@code org.nmcpye.datarun.common.translation}), and the submission review glossary has no term for
 * this step, so the code says <i>normalized submission</i>.</p>
 *
 * <p><b>Ids</b> are opaque strings: the stored id text (ULIDs, and the 24-hex ids of older forms), never an
 * 11-character uid. Ids with nothing stored to carry them are derived by published recipes ({@link
 * org.nmcpye.datarun.submission.normalized.DerivedIds}).</p>
 */
package org.nmcpye.datarun.submission.normalized;
