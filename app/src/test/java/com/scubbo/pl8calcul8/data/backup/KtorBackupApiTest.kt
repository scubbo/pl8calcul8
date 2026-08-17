package com.scubbo.pl8calcul8.data.backup

import com.scubbo.pl8calcul8.backup.BackupPayload
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

private val EMPTY_PAYLOAD = BackupPayload(lifts = emptyList(), workouts = emptyList(), exercises = emptyList())
private const val PAYLOAD_JSON = """{"version":1,"lifts":[],"workouts":[],"exercises":[]}"""

class KtorBackupApiTest {

    @Test
    fun `upload posts the payload with bearer auth`() = runTest {
        var method: HttpMethod? = null
        var path: String? = null
        var auth: String? = null
        val engine = MockEngine { request ->
            method = request.method
            path = request.url.encodedPath
            auth = request.headers[HttpHeaders.Authorization]
            respond("", HttpStatusCode.Created)
        }

        KtorBackupApi("https://example.test", "sekrit", engine).upload(EMPTY_PAYLOAD)

        assertEquals(HttpMethod.Post, method)
        assertEquals("/backup", path)
        assertEquals("Bearer sekrit", auth)
    }

    @Test
    fun `upload failure throws with status`() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.Unauthorized) }
        val api = KtorBackupApi("https://example.test", "wrong", engine)

        val thrown = assertThrows(BackupException::class.java) {
            kotlinx.coroutines.runBlocking { api.upload(EMPTY_PAYLOAD) }
        }
        assertEquals(true, thrown.message!!.contains("401"))
    }

    @Test
    fun `download returns the payload`() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(PAYLOAD_JSON),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val payload = KtorBackupApi("https://example.test", "sekrit", engine).download()

        assertEquals(EMPTY_PAYLOAD, payload)
    }

    @Test
    fun `download returns null when no backup exists`() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.NotFound) }

        assertNull(KtorBackupApi("https://example.test", "sekrit", engine).download())
    }

    @Test
    fun `download failure throws with status`() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val api = KtorBackupApi("https://example.test", "sekrit", engine)

        val thrown = assertThrows(BackupException::class.java) {
            kotlinx.coroutines.runBlocking { api.download() }
        }
        assertEquals(true, thrown.message!!.contains("500"))
    }
}
