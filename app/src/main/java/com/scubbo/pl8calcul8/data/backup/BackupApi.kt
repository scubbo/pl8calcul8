package com.scubbo.pl8calcul8.data.backup

import com.scubbo.pl8calcul8.backup.BackupPayload
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

interface BackupApi {
    /** Uploads a snapshot. Throws [BackupException] on failure. */
    suspend fun upload(payload: BackupPayload)

    /** Returns the newest snapshot, or null if none exists. */
    suspend fun download(): BackupPayload?
}

class KtorBackupApi(
    baseUrl: String,
    private val token: String,
    engine: HttpClientEngine = OkHttp.create(),
) : BackupApi {
    private val baseUrl = baseUrl.trimEnd('/')
    private val client = HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) {
            json()
        }
    }

    override suspend fun upload(payload: BackupPayload) {
        val response = client.post("$baseUrl/backup") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        if (!response.status.isSuccess()) {
            throw BackupException("Backup failed: server said ${response.status.value}")
        }
    }

    override suspend fun download(): BackupPayload? {
        val response = client.get("$baseUrl/restore") {
            bearerAuth(token)
        }
        return when {
            response.status == HttpStatusCode.NotFound -> null
            response.status.isSuccess() -> response.body()
            else -> throw BackupException("Restore failed: server said ${response.status.value}")
        }
    }

    private fun HttpStatusCode.isSuccess(): Boolean = value in 200..299
}
