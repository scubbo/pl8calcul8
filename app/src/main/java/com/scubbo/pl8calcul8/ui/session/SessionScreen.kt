package com.scubbo.pl8calcul8.ui.session

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scubbo.pl8calcul8.calc.RpeChart
import com.scubbo.pl8calcul8.data.AppDatabase
import com.scubbo.pl8calcul8.data.Lift
import kotlinx.coroutines.launch

private val RPE_OPTIONS: List<Double> =
    generateSequence(RpeChart.MIN_RPE) { it + 0.5 }.takeWhile { it <= RpeChart.MAX_RPE }.toList()
private val REP_OPTIONS: List<Int> = (RpeChart.MIN_REPS..RpeChart.MAX_REPS).toList()
private val SET_OPTIONS: List<Int> = (1..10).toList()

/** Formats RPE without a trailing .0: "7", "7.5". */
private fun rpeLabel(rpe: Double): String =
    if (rpe % 1.0 == 0.0) rpe.toInt().toString() else rpe.toString()

private fun weightLabel(weightLb: Double): String =
    if (weightLb % 1.0 == 0.0) weightLb.toInt().toString() else weightLb.toString()

@Composable
fun SessionScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val vm: SessionViewModel = viewModel { SessionViewModel(db.liftDao(), db.workoutDao()) }
    val planned by vm.planned.collectAsState()
    val lifts by vm.lifts.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Workout", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
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
                        vm.recordResult(index, weight, rpe, notes)
                        expandedIndex = null
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

    if (showAddDialog) {
        AddExerciseDialog(
            lifts = lifts,
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
    var weightText by remember { mutableStateOf(initialWeight?.let(::weightLabel) ?: "") }
    var rpe by remember { mutableStateOf(initialRpe) }
    var notes by remember { mutableStateOf(initialNotes) }
    val weight = weightText.toDoubleOrNull()

    OutlinedTextField(
        value = weightText,
        onValueChange = { weightText = it },
        label = { Text("Weight (lb)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    OptionDropdown(
        label = "RPE",
        options = RPE_OPTIONS,
        selected = rpe,
        display = ::rpeLabel,
        onSelect = { rpe = it },
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Notes (optional)") },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { onRecord(weight!!, rpe, notes) },
        enabled = weight != null && weight > 0,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Save result")
    }
}

@Composable
private fun AddExerciseDialog(
    lifts: List<Lift>,
    onAdd: (lift: Lift, reps: Int, rpe: Double, sets: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var lift by remember { mutableStateOf<Lift?>(null) }
    var reps by remember { mutableStateOf(5) }
    var rpe by remember { mutableStateOf(8.0) }
    var sets by remember { mutableStateOf(3) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add exercise") },
        text = {
            Column {
                OptionDropdown(
                    label = "Lift",
                    options = lifts,
                    selected = lift,
                    display = { it?.name ?: "Select a lift" },
                    onSelect = { lift = it },
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        OptionDropdown(
                            label = "Reps",
                            options = REP_OPTIONS,
                            selected = reps,
                            display = Int::toString,
                            onSelect = { reps = it },
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        OptionDropdown(
                            label = "RPE",
                            options = RPE_OPTIONS,
                            selected = rpe,
                            display = ::rpeLabel,
                            onSelect = { rpe = it },
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        OptionDropdown(
                            label = "Sets",
                            options = SET_OPTIONS,
                            selected = sets,
                            display = Int::toString,
                            onSelect = { sets = it },
                        )
                    }
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
}

@Suppress("UNCHECKED_CAST")
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun <T> OptionDropdown(
    label: String,
    options: List<T>,
    selected: T,
    display: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = display(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(display(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
