package com.nexus.connect.model.v1

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant

/**
 * A lightweight attachment referencing an external file.
 *
 * Only the bare minimum fields needed for clients to render or download media —
 * no embedded blobs, no thumbnails, no variant representations.
 */
@Entity
@Table(name = "attachments")
data class Attachment(

    /** Permalink / URL where the file can be fetched (required). */
    @Id @Column(nullable = false)
    val url: String,

    /** Original filename as uploaded by the sender. */
    @Column(nullable = false)
    val filename: String,

    /** MIME type hinting at how the client should render the attachment. */
    @Column(name = "mime_type", nullable = false)
    val mimeType: String,

    /** File size in bytes; `null` when unknown or omitted. */
    @Column(name = "size_bytes")
    val sizeBytes: Long? = null,
) {
    init {
        val url = this@Attachment.url
        require(url.isNotBlank()) { "Attachment URL must not be blank" }
        val filename = this@Attachment.filename
        require(filename.isNotBlank()) { "Attachment filename must not be blank" }
        val mimeType = this@Attachment.mimeType
        require(mimeType.isNotBlank()) { "Attachment mimeType must not be blank" }
        require(this.sizeBytes == null || sizeBytes!! >= 0) { "sizeBytes must be non-negative when present" }
    }
}

/** Reaction kind for ordered storage in the DB. */
enum class ReactionKind { EMOJI, UNICODE }

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
@Entity
@Table(name = "messages")
data class Message(

    /** hardcoded version number for this specific message data type **/
    @Column(name = "version", nullable = false)
    val version: Int = 1,

    /** Unique, sortable identifier (ULID recommended). Serves as the primary key. */
    @Id
    val id: String,

    /** Lightweight reference to the sender — just an ID, not a full profile object. */
    @Column(name = "sender_id", nullable = false)
    val senderId: String,

    /** When the message was originally created (immutable after send). */
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,

    /** When the message content was last edited; `null` if never modified. */
    @Column(name = "edited_at")
    val editedAt: Instant? = null,

    /** Plain text or a restricted Markdown subset (bold, italic, inline code, links). */
    @Lob @Column(nullable = false)
    val body: String,

    /** ID of the parent/top-level message this reply belongs to; `null` for top-level messages. */
    @Column(name = "thread_parent_id")
    val threadParentId: String? = null,

    /** Soft-delete flag — hides content without removing the message record. */
    @Column(name = "is_deleted", nullable = false)
    val isDeleted: Boolean = false,

    /** Attachments (files/media) referenced in the message body. Empty list when none. */
    @OneToMany(mappedBy = "message", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val attachments: List<Attachment> = emptyList(),

    /** Compact reaction map: emoji string → net count. Absent or empty means no reactions. */
    @ElementCollection
    @MapKeyColumn(name = "emoji")
    @Column(name = "reaction_count")
    val reactions: Map<String, Int> = emptyMap(),
) {
    init {
        val id = this@Message.id
        require(id.isNotBlank()) { "Message ID must not be blank" }
        val senderId = this@Message.senderId
        require(senderId.isNotBlank()) { "Sender ID must not be blank" }
        val body = this@Message.body
        require(body.isNotEmpty()) { "Message body must not be empty" }
        threadParentId?.let { tp ->
            require(tp.isNotBlank() && tp != id) {
                "threadParentId must reference a different message than the message itself"
            }
        }
    }

    // ── Convenience accessors ────────────────────────────────────────────────

    /** `true` if this message has been edited since creation. */
    val isEdited: Boolean
        get() { val ea = this.editedAt; return ea != null && ea > createdAt }

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
