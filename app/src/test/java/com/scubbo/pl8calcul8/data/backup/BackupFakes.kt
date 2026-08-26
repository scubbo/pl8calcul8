package com.scubbo.pl8calcul8.data.backup

import com.scubbo.pl8calcul8.backup.BackupPayload
import com.scubbo.pl8calcul8.ui.settings.BackupConfigStore

class FakeBackupApi : BackupApi {
    var uploaded: BackupPayload? = null
    var stored: BackupPayload? = null
    var failWith: Exception? = null

    override suspend fun upload(payload: BackupPayload) {
        failWith?.let { throw it }
        uploaded = payload
    }

    override suspend fun download(): BackupPayload? {
        failWith?.let { throw it }
        return stored
    }
}

class FakeConfigStore : BackupConfigStore {
    override var serverUrl: String = ""
    override var token: String = ""
}
