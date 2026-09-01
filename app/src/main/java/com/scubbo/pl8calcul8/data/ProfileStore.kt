package com.scubbo.pl8calcul8.data

import android.content.Context
import androidx.core.content.edit
import com.scubbo.pl8calcul8.calc.Sex

/** The lifter's profile, used to scale strength standards. */
interface ProfileStore {
    /** Year of birth (age changes; birth year doesn't). 0 = unset. */
    var birthYear: Int
    var sex: Sex
}

class PrefsProfileStore(context: Context) : ProfileStore {
    private val prefs = context.getSharedPreferences("profile", Context.MODE_PRIVATE)

    override var birthYear: Int
        get() = prefs.getInt("birthYear", 0)
        set(value) = prefs.edit { putInt("birthYear", value) }

    override var sex: Sex
        get() = Sex.valueOf(prefs.getString("sex", Sex.MALE.name) ?: Sex.MALE.name)
        set(value) = prefs.edit { putString("sex", value.name) }
}
