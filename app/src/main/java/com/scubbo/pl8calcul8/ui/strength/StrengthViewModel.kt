package com.scubbo.pl8calcul8.ui.strength

import androidx.lifecycle.ViewModel
import com.scubbo.pl8calcul8.calc.RpeCalculator
import com.scubbo.pl8calcul8.calc.ScoringCategory
import com.scubbo.pl8calcul8.calc.StrengthScore
import com.scubbo.pl8calcul8.calc.StrengthStandards
import com.scubbo.pl8calcul8.data.BodyweightDao
import com.scubbo.pl8calcul8.data.BodyweightEntry
import com.scubbo.pl8calcul8.data.LiftDao
import com.scubbo.pl8calcul8.data.ProfileStore
import com.scubbo.pl8calcul8.data.WorkoutDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** One designated lift's strength assessment. */
data class LiftStrength(
    val liftName: String,
    val category: ScoringCategory,
    val oneRepMaxLb: Double,
    val score: StrengthScore,
)

data class StrengthState(
    val profileComplete: Boolean = false,
    val bodyweightLb: Double? = null,
    val birthYear: Int = 0,
    val scores: List<LiftStrength> = emptyList(),
)

class StrengthViewModel(
    private val liftDao: LiftDao,
    private val workoutDao: WorkoutDao,
    private val bodyweightDao: BodyweightDao,
    private val profileStore: ProfileStore,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val _state = MutableStateFlow(StrengthState())
    val state: StateFlow<StrengthState> = _state.asStateFlow()

    suspend fun refresh() {
        val bodyweight = bodyweightDao.latest()
        val birthYear = profileStore.birthYear
        if (bodyweight == null || birthYear == 0) {
            _state.value = StrengthState(
                profileComplete = false,
                bodyweightLb = bodyweight?.weightLb,
                birthYear = birthYear,
            )
            return
        }
        val age = today().year - birthYear
        val scores = liftDao.dump()
            .filter { it.scoringCategory != null }
            .mapNotNull { lift ->
                val category = runCatching {
                    ScoringCategory.valueOf(lift.scoringCategory!!)
                }.getOrNull() ?: return@mapNotNull null
                val best = workoutDao.historyForLift(lift.id)
                    .mapNotNull { entry ->
                        runCatching {
                            RpeCalculator.estimateOneRepMax(
                                weightLb = entry.weightLb,
                                reps = entry.assignedReps,
                                rpe = entry.rpe,
                            )
                        }.getOrNull()
                    }
                    .maxOrNull() ?: return@mapNotNull null
                LiftStrength(
                    liftName = lift.name,
                    category = category,
                    oneRepMaxLb = best,
                    score = StrengthStandards.score(
                        oneRepMaxLb = best,
                        category = category,
                        bodyweightLb = bodyweight.weightLb,
                        age = age,
                        sex = profileStore.sex,
                    ),
                )
            }
            .sortedBy { it.category.ordinal }
        _state.value = StrengthState(
            profileComplete = true,
            bodyweightLb = bodyweight.weightLb,
            birthYear = birthYear,
            scores = scores,
        )
    }

    suspend fun logBodyweight(weightLb: Double) {
        bodyweightDao.insert(BodyweightEntry(date = System.currentTimeMillis(), weightLb = weightLb))
        refresh()
    }

    suspend fun saveProfile(birthYear: Int, sex: com.scubbo.pl8calcul8.calc.Sex) {
        profileStore.birthYear = birthYear
        profileStore.sex = sex
        refresh()
    }

    suspend fun bodyweightHistory(): List<BodyweightEntry> = bodyweightDao.all().first()
}
