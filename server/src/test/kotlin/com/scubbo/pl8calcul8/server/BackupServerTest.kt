package com.scubbo.pl8calcul8.server

import com.scubbo.pl8calcul8.backup.BackupExercise
import com.scubbo.pl8calcul8.backup.BackupLift
import com.scubbo.pl8calcul8.backup.BackupPayload
import com.scubbo.pl8calcul8.backup.BackupWorkout
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

private const val TOKEN = "test-secret"
private const val OTHER_TOKEN = "other-secret"

private val SAMPLE = BackupPayload(
    lifts = listOf(BackupLift(id = 1, name = "Bench Press", incrementLb = 5.0)),
    workouts = listOf(BackupWorkout(id = 1, date = 1_000L)),
    exercises = listOf(
        BackupExercise(
            id = 1, workoutId = 1, liftId = 1,
            assignedReps = 5, assignedRpe = 8.0, sets = 3,
            weightLb = 185.0, rpe = 8.0, notes = "felt good",
        )
    ),
)

class BackupServerTest {

    private fun withServer(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) {
        val dataDir = Files.createTempDirectory("pl8-backup-test")
        testApplication {
            application {
                backupServer(
                    BackupConfig(
                        tracks = mapOf(TOKEN to "tester", OTHER_TOKEN to "other"),
                        dataDir = dataDir,
                    )
                )
            }
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json()
                }
            }
            block(client)
        }
    }

    @Test
    fun `tracks are isolated by token`() = withServer { client ->
        client.post("/backup") {
            bearerAuth(TOKEN)
            contentType(ContentType.Application.Json)
            setBody(SAMPLE)
        }

        // The other track sees no backup...
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/restore") { bearerAuth(OTHER_TOKEN) }.status,
        )

        // ...until it uploads its own, which doesn't clobber the first
        val otherPayload = SAMPLE.copy(
            lifts = listOf(BackupLift(id = 1, name = "Squat", incrementLb = 10.0)),
        )
        client.post("/backup") {
            bearerAuth(OTHER_TOKEN)
            contentType(ContentType.Application.Json)
            setBody(otherPayload)
        }

        val mine: BackupPayload = client.get("/restore") { bearerAuth(TOKEN) }.body()
        val theirs: BackupPayload = client.get("/restore") { bearerAuth(OTHER_TOKEN) }.body()
        assertEquals("Bench Press", mine.lifts.single().name)
        assertEquals("Squat", theirs.lifts.single().name)
    }

    @Test
    fun `config parses BACKUP_TOKENS format`() {
        val tracks = parseTracks("jack:abc123,anna:xyz789")
        assertEquals(mapOf("abc123" to "jack", "xyz789" to "anna"), tracks)
    }

    @Test
    fun `config rejects malformed BACKUP_TOKENS`() {
        kotlin.test.assertFailsWith<IllegalArgumentException> { parseTracks("no-colon-here") }
        kotlin.test.assertFailsWith<IllegalArgumentException> { parseTracks("dup:a,dup:b") }
        kotlin.test.assertFailsWith<IllegalArgumentException> { parseTracks("a:same,b:same") }
        kotlin.test.assertFailsWith<IllegalArgumentException> { parseTracks("bad/name:tok") }
    }

    @Test
    fun `health endpoint needs no auth`() = withServer { client ->
        val response = client.get("/healthz")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `backup without token is rejected`() = withServer { client ->
        val response = client.post("/backup") {
            contentType(ContentType.Application.Json)
            setBody(SAMPLE)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `backup with wrong token is rejected`() = withServer { client ->
        val response = client.post("/backup") {
            bearerAuth("wrong")
            contentType(ContentType.Application.Json)
            setBody(SAMPLE)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `restore without a backup is 404`() = withServer { client ->
        val response = client.get("/restore") { bearerAuth(TOKEN) }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `backup then restore round-trips the payload`() = withServer { client ->
        val backupResponse = client.post("/backup") {
            bearerAuth(TOKEN)
            contentType(ContentType.Application.Json)
            setBody(SAMPLE)
        }
        assertEquals(HttpStatusCode.Created, backupResponse.status)

        val restored: BackupPayload = client.get("/restore") { bearerAuth(TOKEN) }.body()
        assertEquals(SAMPLE, restored)
    }

    @Test
    fun `restore returns the most recent backup`() = withServer { client ->
        val older = SAMPLE
        val newer = SAMPLE.copy(
            workouts = SAMPLE.workouts + BackupWorkout(id = 2, date = 2_000L),
        )
        client.post("/backup") {
            bearerAuth(TOKEN)
            contentType(ContentType.Application.Json)
            setBody(older)
        }
        client.post("/backup") {
            bearerAuth(TOKEN)
            contentType(ContentType.Application.Json)
            setBody(newer)
        }

        val restored: BackupPayload = client.get("/restore") { bearerAuth(TOKEN) }.body()
        assertEquals(newer, restored)
    }

    @Test
    fun `malformed backup payload is a client error`() = withServer { client ->
        val response = client.post("/backup") {
            bearerAuth(TOKEN)
            contentType(ContentType.Application.Json)
            setBody("{\"not\": \"a payload\"}")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        response.bodyAsText() // must not throw
    }

    @Test
    fun `web page is served without auth`() = withServer { client ->
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(true, response.bodyAsText().contains("pl8calcul8"))
    }

    @Test
    fun `history requires auth`() = withServer { client ->
        assertEquals(HttpStatusCode.Unauthorized, client.get("/history").status)
    }

    @Test
    fun `history computes per-lift series from the latest backup`() = withServer { client ->
        client.post("/backup") {
            bearerAuth(TOKEN)
            contentType(ContentType.Application.Json)
            setBody(SAMPLE)
        }

        val history: HistoryResponse = client.get("/history") { bearerAuth(TOKEN) }.body()

        val lift = history.lifts.single()
        assertEquals("Bench Press", lift.name)
        val entry = lift.entries.single()
        assertEquals(1_000L, entry.date)
        assertEquals(185.0, entry.weightLb, 1e-9)
        // 185 x5 @8 -> 185 / 0.811
        assertEquals(228.11, entry.oneRepMax!!, 0.01)
        // 185 * 5 * 3
        assertEquals(2775.0, entry.tonnage, 1e-9)
        assertEquals("felt good", entry.notes)
    }

    @Test
    fun `history without a backup is 404`() = withServer { client ->
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/history") { bearerAuth(TOKEN) }.status,
        )
    }
}
