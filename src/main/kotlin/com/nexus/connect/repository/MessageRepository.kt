package com.nexus.connect.repository

import com.nexus.connect.model.v1.Message
import jakarta.persistence.EntityManager
import java.time.Instant

/**
 * JPA-based repository for persisting and querying [Message] entities.
 *
 * The EntityManager is provided via Ktor's `jakarta.persistence` plugin,
 * which registers it as a call-scoped attribute that can be resolved through
 * the application pipeline or dependency injection.
 */
class MessageRepository(
    private val entityManager: EntityManager,
) {

    /**
     * Persist a new message and flush to ensure the ID is assigned immediately.
     */
    fun save(message: Message): Message {
        entityManager.persist(message)
        entityManager.flush()
        return message
    }

    /**
     * Find a message by its ULID identifier. Returns `null` if not found or soft-deleted.
     */
    fun findById(id: String): Message? =
        entityManager.find(Message::class.java, id)?.takeUnless { it.isDeleted }

    /**
     * Find a message by ID including soft-deleted records.
     */
    fun findWithDeletedById(id: String): Message? =
        entityManager.find(Message::class.java, id)

    /**
     * Retrieve messages sent by a specific user, newest first, respecting soft-delete.
     * Pagination is handled client-side via `limit`/`offset`.
     */
    fun findBySender(senderId: String, limit: Int = 50, offset: Int = 0): List<Message> {
        val query = entityManager.createQuery(
            "SELECT m FROM Message m WHERE m.senderId = :senderId AND m.isDeleted = false ORDER BY m.createdAt DESC",
            Message::class.java,
        )
        query.setParameter("senderId", senderId)
        query.setMaxResults(limit)
        query.setFirstResult(offset)
        return query.resultList
    }

    /**
     * Retrieve thread replies for a given parent message ID.
     */
    fun findThreadReplies(threadParentId: String, limit: Int = 50): List<Message> {
        val query = entityManager.createQuery(
            "SELECT m FROM Message m WHERE m.threadParentId = :parentId AND m.isDeleted = false ORDER BY m.createdAt ASC",
            Message::class.java,
        )
        query.setParameter("parentId", threadParentId)
        query.setMaxResults(limit)
        return query.resultList
    }

    /**
     * Soft-delete a message by setting isDeleted = true.
     */
    fun softDelete(id: String): Boolean {
        val message = entityManager.find(Message::class.java, id) ?: return false
        if (message.isDeleted) return false
        val detached = entityManager.detach(message) // ensure managed
        entityManager.merge(Message(
            version = message.version,
            id = message.id,
            senderId = message.senderId,
            createdAt = message.createdAt,
            editedAt = message.editedAt,
            body = message.body,
            threadParentId = message.threadParentId,
            isDeleted = true,
            attachments = message.attachments,
            reactions = message.reactions,
        ))
        entityManager.flush()
        return true
    }

    /**
     * Edit the body of an existing message.
     */
    fun editBody(id: String, newBody: String): Message? {
        val message = entityManager.find(Message::class.java, id) ?: return null
        if (message.isDeleted) return null
        return entityManager.merge(
            Message(
                version = message.version + 1,
                id = message.id,
                senderId = message.senderId,
                createdAt = message.createdAt,
                editedAt = Instant.now(),
                body = newBody,
                threadParentId = message.threadParentId,
                isDeleted = message.isDeleted,
                attachments = message.attachments,
                reactions = message.reactions,
            ),
        )
    }

    /**
     * Count of non-deleted messages (useful for pagination metadata).
     */
    fun countActive(): Long {
        val query = entityManager.createQuery("SELECT COUNT(m) FROM Message m WHERE m.isDeleted = false", Long::class.java)
        return query.singleResult
    }

    /**
     * Count of active messages for a specific sender.
     */
    fun countBySender(senderId: String): Long {
        val query = entityManager.createQuery(
            "SELECT COUNT(m) FROM Message m WHERE m.senderId = :senderId AND m.isDeleted = false",
            Long::class.java,
        )
        query.setParameter("senderId", senderId)
        return query.singleResult
    }

    private fun <T : Any> EntityManager.detach(entity: T): T {
        if (entityManager.contains(entity)) entityManager.detach(entity)
        return entity
    }
}
