package com.scubbo.pl8calcul8.ui.history

import androidx.lifecycle.ViewModel
import com.scubbo.pl8calcul8.calc.RpeCalculator
import com.scubbo.pl8calcul8.data.ExerciseHistoryEntry
import com.scubbo.pl8calcul8.data.Lift
import com.scubbo.pl8calcul8.data.LiftDao
import com.scubbo.pl8calcul8.data.WorkoutDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A (date, value) chart point. */
typealias ChartPoint = Pair<Long, Double>

class HistoryViewModel(
    liftDao: LiftDao,
    private val workoutDao: WorkoutDao,
) : ViewModel() {

    val lifts: Flow<List<Lift>> = liftDao.all()

    private val _selectedLift = MutableStateFlow<Lift?>(null)
    val selectedLift: StateFlow<Lift?> = _selectedLift.asStateFlow()

    private val _entries = MutableStateFlow<List<ExerciseHistoryEntry>>(emptyList())
    /** Newest first, as displayed in the table. */
    val entries: StateFlow<List<ExerciseHistoryEntry>> = _entries.asStateFlow()

    private val _oneRepMaxPoints = MutableStateFlow<List<ChartPoint>>(emptyList())
    /** Date-ascending estimated 1RM, for charting. */
    val oneRepMaxPoints: StateFlow<List<ChartPoint>> = _oneRepMaxPoints.asStateFlow()

    private val _weightPoints = MutableStateFlow<List<ChartPoint>>(emptyList())
    /** Date-ascending actual weight, for charting. */
    val weightPoints: StateFlow<List<ChartPoint>> = _weightPoints.asStateFlow()

    suspend fun selectLift(lift: Lift) {
        _selectedLift.value = lift
        val history = workoutDao.historyForLift(lift.id)
        _entries.value = history
        val ascending = history.asReversed()
        _oneRepMaxPoints.value = ascending.map { entry ->
            entry.date to RpeCalculator.estimateOneRepMax(
                weightLb = entry.weightLb,
                reps = entry.assignedReps,
                rpe = entry.rpe,
            )
        }
        _weightPoints.value = ascending.map { it.date to it.weightLb }
    }
}
