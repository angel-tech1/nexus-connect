package com.nexus.connect

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(Netty, host = "0.0.0.0", port = 8080) {
        applicationModule()
    }.start(wait = true)
}

fun Application.applicationModule() {
    install(ContentNegotiation) {}

    routing {
        get("/health") {
            call.respondText("Nexus Connect is up!", contentType = ContentType.Text.Plain)
        }
    }
}
