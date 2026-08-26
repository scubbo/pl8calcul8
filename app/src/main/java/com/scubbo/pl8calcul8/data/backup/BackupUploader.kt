package com.scubbo.pl8calcul8.data.backup

import com.scubbo.pl8calcul8.data.LiftDao
import com.scubbo.pl8calcul8.data.WorkoutDao
import com.scubbo.pl8calcul8.ui.settings.BackupConfigStore

/** Uploads the whole database as a backup; used by Settings and after workouts. */
class BackupUploader(
    private val liftDao: LiftDao,
    private val workoutDao: WorkoutDao,
    private val configStore: BackupConfigStore,
    private val apiFactory: (url: String, token: String) -> BackupApi =
        { url, token -> KtorBackupApi(url, token) },
) {
    fun isConfigured(): Boolean =
        configStore.serverUrl.isNotBlank() && configStore.token.isNotBlank()

    /**
     * Backs up, returning a human-readable status - or null when no server
     * is configured. Never throws: backup failure must not break workflows
     * it is appended to (like finishing a workout).
     */
    suspend fun backup(): String? {
        if (!isConfigured()) return null
        val api = apiFactory(configStore.serverUrl, configStore.token)
        return try {
            val payload = buildPayload(
                lifts = liftDao.dump(),
                workouts = workoutDao.dumpWorkouts(),
                exercises = workoutDao.dumpExercises(),
            )
            api.upload(payload)
            "Backed up ${payload.workouts.size} workouts (${payload.exercises.size} exercises)."
        } catch (e: BackupException) {
            e.message
        } catch (e: Exception) {
            "Backup failed: ${e.message}"
        }
    }
}
