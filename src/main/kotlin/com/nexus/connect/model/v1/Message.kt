package com.nexus.connect.model.v1

import java.time.Instant

/**
 * A lightweight attachment referencing an external file.
 *
 * Only the bare minimum fields needed for clients to render or download media —
 * no embedded blobs, no thumbnails, no variant representations.
 */
data class Attachment(
    /** Permalink / URL where the file can be fetched (required). */
    val url: String,
    /** Original filename as uploaded by the sender. */
    val filename: String,
    /** MIME type hinting at how the client should render the attachment. */
    val mimeType: String,
    /** File size in bytes; `null` when unknown or omitted. */
    val sizeBytes: Long? = null,
) {
    init {
        require(url.isNotBlank()) { "Attachment URL must not be blank" }
        require(filename.isNotBlank()) { "Attachment filename must not be blank" }
        require(mimeType.isNotBlank()) { "Attachment mimeType must not be blank" }
        require(sizeBytes == null || sizeBytes >= 0) { "sizeBytes must be non-negative when present" }
    }
}

/**
 * Minimalist message model for a lean IM protocol.
 *
 * Every field is deliberately kept simple to keep payloads small and sync cheap:
 * - ULID-based IDs are sortable, deduplicatable, and referenceable without relying on timestamps alone.
 * - Threading is opt-in via `threadParentId`; top-level messages carry `null`.
 * - Soft-delete preserves the message identity so replies remain valid while hiding content.
 * - Reactions are stored as a compact emoji → count map (net-change only, no per-user history).
 * - Edit history is flattened to a single nullable timestamp — clients show "(edited)".
 */
data class Message(

    /** hardcoded version number for this specific message data type **/
    val version: Int = 1,

    /** Unique, sortable identifier (ULID recommended). Serves as the primary key. */
    val id: String,

    /** Lightweight reference to the sender — just an ID, not a full profile object. */
    val senderId: String,

    /** When the message was originally created (immutable after send). */
    val createdAt: Instant,

    /** When the message content was last edited; `null` if never modified. */
    val editedAt: Instant? = null,

    /** Plain text or a restricted Markdown subset (bold, italic, inline code, links). */
    val body: String,

    /** ID of the parent/top-level message this reply belongs to; `null` for top-level messages. */
    val threadParentId: String? = null,

    /** Soft-delete flag — hides content without removing the message record. */
    val isDeleted: Boolean = false,

    /** Attachments (files/media) referenced in the message body. Empty list when none. */
    val attachments: List<Attachment> = emptyList(),

    /** Compact reaction map: emoji string → net count. Absent or empty means no reactions. */
    val reactions: Map<String, Int> = emptyMap(),
) {
    init {
        require(id.isNotBlank()) { "Message ID must not be blank" }
        require(senderId.isNotBlank()) { "Sender ID must not be blank" }
        require(body.isNotEmpty()) { "Message body must not be empty" }
        if (threadParentId != null) {
            require(threadParentId.isNotBlank() && threadParentId != id) {
                "threadParentId must reference a different message than the message itself"
            }
        }
    }

    // ── Convenience accessors ────────────────────────────────────────────────

    /** `true` if this message has been edited since creation. */
    val isEdited: Boolean
        get() = editedAt != null && editedAt > createdAt

    /** Total number of attachment files associated with this message. */
    val attachmentCount: Int
        get() = attachments.size

    /** Total count across all reactions (sum of values). */
    val totalReactionCount: Int
        get() = reactions.values.sum()

    /**
     * Creates a copy of this message with updated content — useful for editing in-place.
     */
    fun withEditedBody(newBody: String): Message = copy(
        body = newBody,
        editedAt = Instant.now(),
    )

    /**
     * Returns this message's ID as its thread reference (alias for clarity at call sites).
     */
    fun asThreadRef(): String = id
}
