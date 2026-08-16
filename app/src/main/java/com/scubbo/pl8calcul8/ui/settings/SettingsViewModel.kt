package com.scubbo.pl8calcul8.ui.settings

import androidx.lifecycle.ViewModel
import com.scubbo.pl8calcul8.data.Lift
import com.scubbo.pl8calcul8.data.LiftDao
import com.scubbo.pl8calcul8.data.createLift
import kotlinx.coroutines.flow.Flow

class SettingsViewModel(
    private val liftDao: LiftDao,
) : ViewModel() {

    val lifts: Flow<List<Lift>> = liftDao.all()

    suspend fun addLift(name: String): Lift = liftDao.createLift(name)

    suspend fun setIncrement(lift: Lift, incrementLb: Double) {
        liftDao.updateIncrement(lift.id, incrementLb)
    }
}
