package com.nexus.connect

import com.nexus.connect.repository.MessageRepository
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.util.AttributeKey
import jakarta.persistence.EntityManagerFactory
import kotlinx.coroutines.Dispatchers

/** Key for storing the EntityManagerFactory in Application's attributes. */
val JPA_KEYS = AttributeKey<EntityManagerFactory>("nexus-connect.emf")

fun main() {
    embeddedServer(Netty, host = "0.0.0.0", port = 8080) {
        applicationModule()
    }.start(wait = true)
}

/**
 * Resolves a [MessageRepository] from the current call scope by extracting the
 * EntityManagerFactory registered in Application attributes during startup, then creates an
 * EntityManager scoped to this request. The EntityManager is closed after
 * the response completes.
 */
fun ApplicationCall.messageRepository(): MessageRepository {
    val emf: EntityManagerFactory = application.attributes.getOrNull(JPA_KEYS)
        ?: error("EntityManagerFactory not available — did JPA init run?")

    val em = emf.createEntityManager()

    // Close the EntityManager after the response is fully sent.
    var closed = false
    val sendPipe = this@messageRepository.application.sendPipeline
    sendPipe.intercept(ApplicationSendPipeline.Engine) {
        proceed()
        if (!closed) {
            closed = true
            try { em.close() } catch (_: Exception) { }
        }
    }

    return MessageRepository(em)
}

fun Application.applicationModule() {
    // ── Content negotiation (JSON serialization for API responses) ───────────
    install(ContentNegotiation) {
        json()
    }

    // ── JPA / Hibernate + PostgreSQL via direct initialization ───────────────
    val db = environment.config.config("ktor.database")
    val jpaUnitName = environment.config.property("ktor.jpa.persistence_unit_name").getString()

    // Override JDBC properties from application.conf (overrides persistence.xml defaults)
    val overrideProps = hashMapOf(
        "jakarta.persistence.jdbc.url" to db.property("url").getString(),
        "jakarta.persistence.jdbc.user" to db.property("user").getString(),
        "jakarta.persistence.jdbc.password" to db.property("password").getString(),
        // Keep schema generation from persistence.xml but allow override
        "hibernate.hbm2ddl.auto" to environment.config.property("ktor.jpa.ddl_auto").getString(),
    )

    val emf: EntityManagerFactory = jakarta.persistence.Persistence
        .createEntityManagerFactory(jpaUnitName, overrideProps)

    attributes.put(JPA_KEYS, emf)

    // Register JVM shutdown hook for graceful cleanup
    Runtime.getRuntime().addShutdownHook(Thread {
        try { emf.close() } catch (_: Exception) { }
    })

    routing {
        get("/health") {
            call.respondText("Nexus Connect is up!", contentType = ContentType.Text.Plain)
        }

        // Example endpoint demonstrating repository usage
        get("/messages/count") {
            val repo = call.messageRepository()
            val count = kotlinx.coroutines.withContext(Dispatchers.IO) { repo.countActive() }
            call.respond(mapOf("activeMessages" to count))
        }
    }
}
