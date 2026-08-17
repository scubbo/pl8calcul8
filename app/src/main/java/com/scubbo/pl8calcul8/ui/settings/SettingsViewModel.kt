package com.scubbo.pl8calcul8.ui.settings

import androidx.lifecycle.ViewModel
import com.scubbo.pl8calcul8.data.Lift
import com.scubbo.pl8calcul8.data.LiftDao
import com.scubbo.pl8calcul8.data.WorkoutDao
import com.scubbo.pl8calcul8.data.backup.BackupApi
import com.scubbo.pl8calcul8.data.backup.BackupException
import com.scubbo.pl8calcul8.data.backup.KtorBackupApi
import com.scubbo.pl8calcul8.data.backup.buildPayload
import com.scubbo.pl8calcul8.data.backup.toExercises
import com.scubbo.pl8calcul8.data.backup.toLifts
import com.scubbo.pl8calcul8.data.backup.toWorkouts
import com.scubbo.pl8calcul8.data.createLift
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where the backup server's URL and token are remembered. */
interface BackupConfigStore {
    var serverUrl: String
    var token: String
}

class SettingsViewModel(
    private val liftDao: LiftDao,
    private val workoutDao: WorkoutDao,
    private val configStore: BackupConfigStore,
    private val apiFactory: (url: String, token: String) -> BackupApi =
        { url, token -> KtorBackupApi(url, token) },
    /** Wraps restore's wipe+insert; production passes Room's withTransaction. */
    private val runInTransaction: suspend (suspend () -> Unit) -> Unit = { it() },
) : ViewModel() {

    val lifts: Flow<List<Lift>> = liftDao.all()

    private val _serverUrl = MutableStateFlow(configStore.serverUrl)
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _token = MutableStateFlow(configStore.token)
    val token: StateFlow<String> = _token.asStateFlow()

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    suspend fun addLift(name: String): Lift = liftDao.createLift(name)

    suspend fun setIncrement(lift: Lift, incrementLb: Double) {
        liftDao.updateIncrement(lift.id, incrementLb)
    }

    fun saveConfig(serverUrl: String, token: String) {
        configStore.serverUrl = serverUrl.trim()
        configStore.token = token.trim()
        _serverUrl.value = configStore.serverUrl
        _token.value = configStore.token
    }

    fun isConfigured(): Boolean =
        configStore.serverUrl.isNotBlank() && configStore.token.isNotBlank()

    suspend fun backup() {
        val api = apiFactory(configStore.serverUrl, configStore.token)
        try {
            val payload = buildPayload(
                lifts = liftDao.dump(),
                workouts = workoutDao.dumpWorkouts(),
                exercises = workoutDao.dumpExercises(),
            )
            api.upload(payload)
            _backupStatus.value =
                "Backed up ${payload.workouts.size} workouts (${payload.exercises.size} exercises)."
        } catch (e: BackupException) {
            _backupStatus.value = e.message
        } catch (e: Exception) {
            _backupStatus.value = "Backup failed: ${e.message}"
        }
    }

    suspend fun restore() {
        val api = apiFactory(configStore.serverUrl, configStore.token)
        try {
            val payload = api.download()
            if (payload == null) {
                _backupStatus.value = "No backup found on the server."
                return
            }
            runInTransaction {
                workoutDao.deleteAllExercises()
                workoutDao.deleteAllWorkouts()
                liftDao.deleteAll()
                payload.toLifts().forEach { liftDao.insert(it) }
                payload.toWorkouts().forEach { workoutDao.insert(it) }
                payload.toExercises().forEach { workoutDao.insert(it) }
            }
            _backupStatus.value =
                "Restored ${payload.workouts.size} workouts (${payload.exercises.size} exercises)."
        } catch (e: BackupException) {
            _backupStatus.value = e.message
        } catch (e: Exception) {
            _backupStatus.value = "Restore failed: ${e.message}"
        }
    }
}
