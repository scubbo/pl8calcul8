package com.scubbo.pl8calcul8.server

import com.scubbo.pl8calcul8.backup.BackupPayload
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.bearer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

data class BackupConfig(
    val token: String,
    val dataDir: Path,
)

private val json = Json { prettyPrint = true }

/** Timestamped-filename store: the lexically greatest file is the newest. */
class BackupStore(private val dataDir: Path) {
    init {
        dataDir.createDirectories()
    }

    fun save(payload: BackupPayload) {
        val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            .replace(":", "-")
        dataDir.resolve("backup-$timestamp.json")
            .writeText(json.encodeToString(BackupPayload.serializer(), payload))
    }

    fun latest(): BackupPayload? =
        dataDir.listDirectoryEntries("backup-*.json")
            .maxByOrNull { it.name }
            ?.let { json.decodeFromString(BackupPayload.serializer(), it.readText()) }
}

fun Application.backupServer(config: BackupConfig) {
    val store = BackupStore(config.dataDir)

    install(ContentNegotiation) {
        json()
    }
    install(StatusPages) {
        exception<kotlinx.serialization.SerializationException> { call, cause ->
            call.respondText(
                "Malformed payload: ${cause.message}",
                status = HttpStatusCode.BadRequest,
            )
        }
        exception<io.ktor.server.plugins.BadRequestException> { call, _ ->
            call.respondText("Malformed payload", status = HttpStatusCode.BadRequest)
        }
    }
    authentication {
        bearer("backup-token") {
            authenticate { credential ->
                if (credential.token == config.token) UserIdPrincipal("client") else null
            }
        }
    }

    routing {
        get("/healthz") {
            call.respondText("ok")
        }
        get("/") {
            val page = this::class.java.classLoader.getResource("web/index.html")!!.readText()
            call.respondText(page, ContentType.Text.Html)
        }
        authenticate("backup-token") {
            post("/backup") {
                val payload = call.receive<BackupPayload>()
                store.save(payload)
                call.respond(HttpStatusCode.Created)
            }
            get("/restore") {
                val latest = store.latest()
                if (latest == null) {
                    call.respondText("No backups yet", status = HttpStatusCode.NotFound)
                } else {
                    call.respond(latest)
                }
            }
            get("/history") {
                val latest = store.latest()
                if (latest == null) {
                    call.respondText("No backups yet", status = HttpStatusCode.NotFound)
                } else {
                    call.respond(buildHistory(latest))
                }
            }
        }
    }
}
