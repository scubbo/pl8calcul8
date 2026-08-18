package com.scubbo.pl8calcul8.ui.session

import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scubbo.pl8calcul8.calc.RpeChart
import com.scubbo.pl8calcul8.data.AppDatabase
import com.scubbo.pl8calcul8.data.Lift
import com.scubbo.pl8calcul8.ui.components.LiftDropdown
import com.scubbo.pl8calcul8.ui.components.NewLiftDialog
import com.scubbo.pl8calcul8.ui.components.NumberSpinner
import kotlinx.coroutines.launch

private val ASSIGNED_RPE_OPTIONS: List<Double> = (6..10).map { it.toDouble() }
private val RECORDED_RPE_OPTIONS: List<Double> =
    generateSequence(6.5) { it + 0.5 }.takeWhile { it <= RpeChart.MAX_RPE }.toList()
private val REP_OPTIONS: List<Int> = (RpeChart.MIN_REPS..RpeChart.MAX_REPS).toList()
private val SET_OPTIONS: List<Int> = (1..10).toList()
private val WEIGHT_OPTIONS: List<Double> =
    generateSequence(5.0) { it + 5.0 }.takeWhile { it <= 995.0 }.toList()
private const val DEFAULT_BAR_WEIGHT = 45.0

/** Formats RPE without a trailing .0: "7", "7.5". */
private fun rpeLabel(rpe: Double): String =
    if (rpe % 1.0 == 0.0) rpe.toInt().toString() else rpe.toString()

private fun weightLabel(weightLb: Double): String =
    if (weightLb % 1.0 == 0.0) weightLb.toInt().toString() else weightLb.toString()

private fun sessionDateLabel(epochMillis: Long): String =
    java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d"))

/**
 * DatePicker reports UTC midnight of the chosen calendar day; anchor it to
 * local noon so the date can't shift when rendered in the local timezone.
 */
private fun pickedDateToLocalMillis(utcMidnightMillis: Long): Long =
    java.time.Instant.ofEpochMilli(utcMidnightMillis)
        .atZone(java.time.ZoneOffset.UTC).toLocalDate()
        .atTime(12, 0).atZone(java.time.ZoneId.systemDefault())
        .toInstant().toEpochMilli()

/** Snaps a weight to the nearest spinner option. */
private fun nearestWeightOption(weightLb: Double): Double =
    WEIGHT_OPTIONS.minByOrNull { kotlin.math.abs(it - weightLb) } ?: DEFAULT_BAR_WEIGHT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val vm: SessionViewModel = viewModel {
        SessionViewModel(db.liftDao(), db.workoutDao(), db.draftDao())
    }
    val planned by vm.planned.collectAsState()
    val lifts by vm.lifts.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val sessionDate by vm.sessionDate.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var expandedIndex by remember { mutableStateOf<Int?>(null) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadDraft() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Workout", style = MaterialTheme.typography.headlineMedium)
            if (planned.isNotEmpty()) {
                TextButton(onClick = { showDiscardConfirm = true }) {
                    Text("Discard")
                }
            }
        }
        // Tappable date: recording a past workout just means changing this.
        TextButton(onClick = { showDatePicker = true }) {
            Text(sessionDateLabel(sessionDate))
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(planned) { index, exercise ->
                ExerciseCard(
                    exercise = exercise,
                    expanded = expandedIndex == index,
                    onToggle = { expandedIndex = if (expandedIndex == index) null else index },
                    onRecord = { weight, rpe, notes ->
                        scope.launch {
                            vm.recordResult(index, weight, rpe, notes)
                            expandedIndex = null
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Add exercise")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { scope.launch { vm.finishSession(); onFinished() } },
            enabled = planned.any { it.result != null },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Finish workout")
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = sessionDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { picked ->
                            scope.launch { vm.setSessionDate(pickedDateToLocalMillis(picked)) }
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("Set date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard this workout?") },
            text = { Text("Planned exercises and any recorded results will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirm = false
                        scope.launch { vm.discardSession() }
                    },
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showAddDialog) {
        AddExerciseDialog(
            lifts = lifts,
            onCreateLift = { name -> vm.addLift(name) },
            onAdd = { lift, reps, rpe, sets ->
                scope.launch { vm.addExercise(lift, reps, rpe, sets) }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun ExerciseCard(
    exercise: PlannedExercise,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRecord: (weightLb: Double, rpe: Double, notes: String?) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "${exercise.lift.name}  ${exercise.reps}@${rpeLabel(exercise.rpe)} × ${exercise.sets}",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            val result = exercise.result
            if (result != null) {
                Text(
                    "Done: ${weightLabel(result.weightLb)} lb @ RPE ${rpeLabel(result.rpe)}" +
                        (result.notes?.let { " — $it" } ?: ""),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text(
                    exercise.advisedWeightLb?.let { "Advised: ${weightLabel(it)} lb" }
                        ?: "No history — choose a starting weight",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (expanded) {
                RecordResultForm(
                    initialWeight = exercise.result?.weightLb ?: exercise.advisedWeightLb,
                    // Default the recorded RPE to the assignment's RPE
                    initialRpe = exercise.result?.rpe ?: exercise.rpe,
                    initialNotes = exercise.result?.notes.orEmpty(),
                    onRecord = onRecord,
                )
            } else {
                TextButton(onClick = onToggle) {
                    Text(if (result == null) "Record result" else "Edit result")
                }
            }
        }
    }
}

@Composable
private fun RecordResultForm(
    initialWeight: Double?,
    initialRpe: Double,
    initialNotes: String,
    onRecord: (weightLb: Double, rpe: Double, notes: String?) -> Unit,
) {
    var weight by remember {
        mutableStateOf(nearestWeightOption(initialWeight ?: DEFAULT_BAR_WEIGHT))
    }
    var rpe by remember {
        // The assignment can be RPE 6 but recording starts at 6.5
        mutableStateOf(initialRpe.coerceIn(RECORDED_RPE_OPTIONS.first(), RECORDED_RPE_OPTIONS.last()))
    }
    var notes by remember { mutableStateOf(initialNotes) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumberSpinner(
            label = "Weight (lb)",
            options = WEIGHT_OPTIONS,
            selected = weight,
            display = ::weightLabel,
            onSelect = { weight = it },
            modifier = Modifier.weight(1f),
        )
        NumberSpinner(
            label = "RPE",
            options = RECORDED_RPE_OPTIONS,
            selected = rpe,
            display = ::rpeLabel,
            onSelect = { rpe = it },
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Notes (optional)") },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { onRecord(weight, rpe, notes) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Save result")
    }
}

@Composable
private fun AddExerciseDialog(
    lifts: List<Lift>,
    onCreateLift: suspend (name: String) -> Lift,
    onAdd: (lift: Lift, reps: Int, rpe: Double, sets: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var lift by remember { mutableStateOf<Lift?>(null) }
    var sets by remember { mutableStateOf(3) }
    var reps by remember { mutableStateOf(5) }
    var rpe by remember { mutableStateOf(8.0) }
    var showNewLiftDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add exercise") },
        text = {
            Column {
                LiftDropdown(
                    lifts = lifts,
                    selected = lift,
                    onSelect = { lift = it },
                    onNewLift = { showNewLiftDialog = true },
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberSpinner(
                        label = "Sets",
                        options = SET_OPTIONS,
                        selected = sets,
                        display = Int::toString,
                        onSelect = { sets = it },
                        modifier = Modifier.weight(1f),
                    )
                    NumberSpinner(
                        label = "Reps",
                        options = REP_OPTIONS,
                        selected = reps,
                        display = Int::toString,
                        onSelect = { reps = it },
                        modifier = Modifier.weight(1f),
                    )
                    NumberSpinner(
                        label = "RPE",
                        options = ASSIGNED_RPE_OPTIONS,
                        selected = rpe,
                        display = ::rpeLabel,
                        onSelect = { rpe = it },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(lift!!, reps, rpe, sets) },
                enabled = lift != null,
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )

    if (showNewLiftDialog) {
        NewLiftDialog(
            onCreate = { name ->
                scope.launch {
                    lift = onCreateLift(name)
                    showNewLiftDialog = false
                }
            },
            onDismiss = { showNewLiftDialog = false },
        )
    }
}


