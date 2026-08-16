package com.scubbo.pl8calcul8.server

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.nio.file.Path

fun main() {
    val token = System.getenv("BACKUP_TOKEN")
        ?: error("BACKUP_TOKEN environment variable is required")
    val dataDir = Path.of(System.getenv("DATA_DIR") ?: "/data")
    val port = System.getenv("PORT")?.toInt() ?: 8080

    embeddedServer(Netty, port = port) {
        backupServer(BackupConfig(token = token, dataDir = dataDir))
    }.start(wait = true)
}
