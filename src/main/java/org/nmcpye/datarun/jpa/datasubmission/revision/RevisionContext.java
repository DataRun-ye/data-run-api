package org.nmcpye.datarun.jpa.datasubmission.revision;

import java.util.Objects;

/**
 * Who changed a submission, through which command, and why: what a revision records besides the state. The command
 * service names it for each command and the persistence layer writes it with every change the command makes.
 *
 * @param command          the command
 * @param actorUserId      the durable id ({@code app_user.id}) of the user who performed it: the uploader, or for a
 *                         release or a delete, the admin
 * @param onBehalfOfUserId for a release, the uploader the admin acts for; otherwise null
 * @param reason           the admin's note on a release, if any; otherwise null
 */
public record RevisionContext(RevisionCommand command, String actorUserId, String onBehalfOfUserId, String reason) {

    public RevisionContext {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(actorUserId, "actorUserId");
        if ((command == RevisionCommand.HELD_RELEASE) != (onBehalfOfUserId != null)) {
            throw new IllegalArgumentException("A release, and only a release, names the uploader it acts for");
        }
    }

    /** A phone upload on the versioned route, by its uploader. */
    public static RevisionContext upload(String uploaderId) {
        return new RevisionContext(RevisionCommand.UPLOAD, uploaderId, null, null);
    }

    /** An upload on the legacy route, by its uploader. */
    public static RevisionContext legacyUpload(String uploaderId) {
        return new RevisionContext(RevisionCommand.LEGACY_UPLOAD, uploaderId, null, null);
    }

    /** An admin's release of a held upload, for the user who uploaded it, with the admin's note. */
    public static RevisionContext heldRelease(String adminId, String uploaderId, String note) {
        return new RevisionContext(RevisionCommand.HELD_RELEASE, adminId, uploaderId, note);
    }

    /** An admin's delete. */
    public static RevisionContext adminDelete(String adminId) {
        return new RevisionContext(RevisionCommand.ADMIN_DELETE, adminId, null, null);
    }
}
