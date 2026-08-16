package com.scubbo.pl8calcul8.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scubbo.pl8calcul8.data.AppDatabase
import com.scubbo.pl8calcul8.data.ExerciseHistoryEntry
import com.scubbo.pl8calcul8.ui.components.LiftDropdown
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TABLE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private fun tableDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(TABLE_DATE_FORMAT)

private fun rpeLabel(rpe: Double): String =
    if (rpe % 1.0 == 0.0) rpe.toInt().toString() else rpe.toString()

private fun weightLabel(weightLb: Double): String =
    if (weightLb % 1.0 == 0.0) weightLb.toInt().toString() else weightLb.toString()

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val vm: HistoryViewModel = viewModel { HistoryViewModel(db.liftDao(), db.workoutDao()) }
    val lifts by vm.lifts.collectAsState(initial = emptyList())
    val selected by vm.selectedLift.collectAsState()
    val entries by vm.entries.collectAsState()
    val oneRepMaxPoints by vm.oneRepMaxPoints.collectAsState()
    val weightPoints by vm.weightPoints.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("History", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        LiftDropdown(
            lifts = lifts,
            selected = selected,
            onSelect = { lift -> scope.launch { vm.selectLift(lift) } },
        )
        Spacer(Modifier.height(16.dp))
        if (selected == null) {
            Text(
                "Choose a lift to see its history.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn {
                item {
                    Text("Estimated 1RM", style = MaterialTheme.typography.titleMedium)
                    com.scubbo.pl8calcul8.ui.components.LineChart(points = oneRepMaxPoints)
                    Spacer(Modifier.height(16.dp))
                    Text("Weight lifted", style = MaterialTheme.typography.titleMedium)
                    com.scubbo.pl8calcul8.ui.components.LineChart(
                        points = weightPoints,
                        lineColor = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Past exercises", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                }
                if (entries.isEmpty()) {
                    item {
                        Text(
                            "No recorded exercises for this lift yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(entries) { entry ->
                    HistoryRow(entry)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: ExerciseHistoryEntry) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                tableDate(entry.date),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${entry.assignedReps}@${rpeLabel(entry.assignedRpe)} × ${entry.sets}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${weightLabel(entry.weightLb)} lb @ ${rpeLabel(entry.rpe)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }
        entry.notes?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
