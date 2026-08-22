package com.scubbo.pl8calcul8.server

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.moveTo
import kotlin.io.path.name

fun main() {
    val dataDir = Path.of(System.getenv("DATA_DIR") ?: "/data")
    val port = System.getenv("PORT")?.toInt() ?: 8080

    // BACKUP_TOKENS=name:token,name:token is the multi-track config;
    // BACKUP_TOKEN=<token> (the original single-user form) still works and
    // maps to a track named "default".
    val tokensSpec = System.getenv("BACKUP_TOKENS")
    val singleToken = System.getenv("BACKUP_TOKEN")
    val tracks = when {
        tokensSpec != null -> parseTracks(tokensSpec)
        singleToken != null -> mapOf(singleToken to "default")
        else -> error("BACKUP_TOKENS (name:token,...) or BACKUP_TOKEN is required")
    }

    migrateLooseBackups(dataDir, tracks.values.first())

    embeddedServer(Netty, port = port) {
        backupServer(BackupConfig(tracks = tracks, dataDir = dataDir))
    }.start(wait = true)
}

/**
 * Backups written before tracks existed live directly in the data dir.
 * Moves them into the first configured track's subdirectory so history is
 * preserved across the upgrade. (With one configured track there is no
 * ambiguity; with several, the first entry is the original user's.)
 */
private fun migrateLooseBackups(dataDir: Path, firstTrack: String) {
    dataDir.createDirectories()
    val loose = dataDir.listDirectoryEntries("backup-*.json")
    if (loose.isEmpty()) return
    val target = dataDir.resolve(firstTrack).createDirectories()
    loose.forEach { it.moveTo(target.resolve(it.name)) }
    println("Migrated ${loose.size} pre-track backups into track '$firstTrack'")
}
