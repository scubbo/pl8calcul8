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

enum class HistoryMetric { E1RM, TONNAGE }

/** One lift's chart line in multi-lift mode. */
data class LiftSeries(val lift: Lift, val points: List<ChartPoint>)

private fun ExerciseHistoryEntry.oneRepMax(): Double =
    RpeCalculator.estimateOneRepMax(weightLb = weightLb, reps = assignedReps, rpe = rpe)

/** Total pounds moved: weight x reps x sets. */
private fun ExerciseHistoryEntry.tonnage(): Double =
    weightLb * assignedReps * sets

class HistoryViewModel(
    liftDao: LiftDao,
    private val workoutDao: WorkoutDao,
) : ViewModel() {

    val lifts: Flow<List<Lift>> = liftDao.all()

    // Single-lift mode

    private val _selectedLift = MutableStateFlow<Lift?>(null)
    val selectedLift: StateFlow<Lift?> = _selectedLift.asStateFlow()

    private val _entries = MutableStateFlow<List<ExerciseHistoryEntry>>(emptyList())
    /** Newest first, as displayed in the table. */
    val entries: StateFlow<List<ExerciseHistoryEntry>> = _entries.asStateFlow()

    private val _oneRepMaxPoints = MutableStateFlow<List<ChartPoint>>(emptyList())
    /** Date-ascending estimated 1RM, for charting. */
    val oneRepMaxPoints: StateFlow<List<ChartPoint>> = _oneRepMaxPoints.asStateFlow()

    private val _tonnagePoints = MutableStateFlow<List<ChartPoint>>(emptyList())
    /** Date-ascending tonnage (weight x reps x sets), for charting. */
    val tonnagePoints: StateFlow<List<ChartPoint>> = _tonnagePoints.asStateFlow()

    suspend fun selectLift(lift: Lift) {
        _selectedLift.value = lift
        val history = workoutDao.historyForLift(lift.id)
        _entries.value = history
        val ascending = history.asReversed()
        _oneRepMaxPoints.value = ascending.map { it.date to it.oneRepMax() }
        _tonnagePoints.value = ascending.map { it.date to it.tonnage() }
    }

    // Multi-lift mode

    private val _metric = MutableStateFlow(HistoryMetric.E1RM)
    val metric: StateFlow<HistoryMetric> = _metric.asStateFlow()

    private val _multiSelectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val multiSelectedIds: StateFlow<Set<Long>> = _multiSelectedIds.asStateFlow()

    private val _multiSeries = MutableStateFlow<List<LiftSeries>>(emptyList())
    /** One series per selected lift, in selection order. */
    val multiSeries: StateFlow<List<LiftSeries>> = _multiSeries.asStateFlow()

    private val multiHistories = LinkedHashMap<Long, Pair<Lift, List<ExerciseHistoryEntry>>>()

    suspend fun toggleLift(lift: Lift) {
        if (multiHistories.remove(lift.id) == null) {
            multiHistories[lift.id] = lift to workoutDao.historyForLift(lift.id)
        }
        _multiSelectedIds.value = multiHistories.keys.toSet()
        rebuildMultiSeries()
    }

    fun setMetric(metric: HistoryMetric) {
        _metric.value = metric
        rebuildMultiSeries()
    }

    private fun rebuildMultiSeries() {
        _multiSeries.value = multiHistories.values.map { (lift, history) ->
            LiftSeries(
                lift = lift,
                points = history.asReversed().map { entry ->
                    entry.date to when (_metric.value) {
                        HistoryMetric.E1RM -> entry.oneRepMax()
                        HistoryMetric.TONNAGE -> entry.tonnage()
                    }
                },
            )
        }
    }
}
