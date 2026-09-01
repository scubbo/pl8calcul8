package com.scubbo.pl8calcul8.ui.strength

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scubbo.pl8calcul8.calc.Sex
import com.scubbo.pl8calcul8.data.AppDatabase
import com.scubbo.pl8calcul8.data.PrefsProfileStore
import kotlinx.coroutines.launch

private fun weightLabel(weightLb: Double): String =
    if (weightLb % 1.0 == 0.0) weightLb.toInt().toString() else weightLb.toString()

@Composable
fun StrengthScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val vm: StrengthViewModel = viewModel {
        StrengthViewModel(
            liftDao = db.liftDao(),
            workoutDao = db.workoutDao(),
            bodyweightDao = db.bodyweightDao(),
            profileStore = PrefsProfileStore(context.applicationContext),
        )
    }
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()
    var showLogWeight by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Strength", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        if (!state.profileComplete) {
            ProfileSetup(
                initialBirthYear = state.birthYear,
                bodyweightLb = state.bodyweightLb,
                onSave = { birthYear, sex, weight ->
                    scope.launch {
                        vm.saveProfile(birthYear, sex)
                        weight?.let { vm.logBodyweight(it) }
                    }
                },
            )
            return
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Bodyweight: ${weightLabel(state.bodyweightLb!!)} lb",
                style = MaterialTheme.typography.bodyLarge,
            )
            TextButton(onClick = { showLogWeight = true }) { Text("Log weight") }
        }
        Spacer(Modifier.height(8.dp))
        if (state.scores.isEmpty()) {
            Text(
                "No designated lifts with history yet. In Settings, tap a lift " +
                    "to assign its scoring category (e.g. Squat → Back Squat).",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.scores) { entry -> ScoreCard(entry) }
            }
        }
    }

    if (showLogWeight) {
        LogWeightDialog(
            current = state.bodyweightLb,
            onLog = { weight ->
                scope.launch {
                    vm.logBodyweight(weight)
                    showLogWeight = false
                }
            },
            onDismiss = { showLogWeight = false },
        )
    }
}

@Composable
private fun ScoreCard(entry: LiftStrength) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(entry.liftName, style = MaterialTheme.typography.titleMedium)
                Text(
                    entry.score.level.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "e1RM ${weightLabel(Math.round(entry.oneRepMaxLb).toDouble())} lb",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { entry.score.progressToNext.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSetup(
    initialBirthYear: Int,
    bodyweightLb: Double?,
    onSave: (birthYear: Int, sex: Sex, weightLb: Double?) -> Unit,
) {
    var birthYearText by remember {
        mutableStateOf(if (initialBirthYear == 0) "" else initialBirthYear.toString())
    }
    var sex by remember { mutableStateOf(Sex.MALE) }
    var weightText by remember { mutableStateOf(bodyweightLb?.let(::weightLabel) ?: "") }
    val birthYear = birthYearText.toIntOrNull()
    val weight = weightText.toDoubleOrNull()

    Text(
        "Strength standards scale with bodyweight, age, and sex. " +
            "Set up your profile to see your levels.",
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = birthYearText,
        onValueChange = { birthYearText = it },
        label = { Text("Birth year") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = sex == Sex.MALE,
            onClick = { sex = Sex.MALE },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) { Text("Male") }
        SegmentedButton(
            selected = sex == Sex.FEMALE,
            onClick = { sex = Sex.FEMALE },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) { Text("Female") }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = weightText,
        onValueChange = { weightText = it },
        label = { Text("Bodyweight (lb)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { onSave(birthYear!!, sex, weight) },
        enabled = birthYear in 1900..2100 && (bodyweightLb != null || weight != null),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Save profile")
    }
}

@Composable
private fun LogWeightDialog(
    current: Double?,
    onLog: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var weightText by remember { mutableStateOf(current?.let(::weightLabel) ?: "") }
    val weight = weightText.toDoubleOrNull()
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log bodyweight") },
        text = {
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text("Bodyweight (lb)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onLog(weight!!) },
                enabled = weight != null && weight > 0,
            ) { Text("Log") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
