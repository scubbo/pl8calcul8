package com.scubbo.pl8calcul8.data.backup

import android.content.Context
import androidx.core.content.edit
import com.scubbo.pl8calcul8.ui.settings.BackupConfigStore

class PrefsBackupConfigStore(context: Context) : BackupConfigStore {
    private val prefs = context.getSharedPreferences("backup", Context.MODE_PRIVATE)

    override var serverUrl: String
        get() = prefs.getString("serverUrl", "") ?: ""
        set(value) = prefs.edit { putString("serverUrl", value) }

    override var token: String
        get() = prefs.getString("token", "") ?: ""
        set(value) = prefs.edit { putString("token", value) }
}
