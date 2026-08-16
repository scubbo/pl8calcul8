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
                backupServer(BackupConfig(token = TOKEN, dataDir = dataDir))
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
}
