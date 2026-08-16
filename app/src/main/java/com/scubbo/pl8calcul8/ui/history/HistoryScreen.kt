package com.scubbo.pl8calcul8.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scubbo.pl8calcul8.data.AppDatabase
import com.scubbo.pl8calcul8.data.ExerciseHistoryEntry
import com.scubbo.pl8calcul8.ui.components.ChartSeries
import com.scubbo.pl8calcul8.ui.components.LiftDropdown
import com.scubbo.pl8calcul8.ui.components.LineChart
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

/** Distinguishable line colors, assigned to lifts in selection order. */
private val SERIES_COLORS = listOf(
    Color(0xFF6750A4), // purple
    Color(0xFFB3261E), // red
    Color(0xFF386641), // green
    Color(0xFF00639B), // blue
    Color(0xFFB4451F), // orange
    Color(0xFF7D5260), // mauve
)

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val vm: HistoryViewModel = viewModel { HistoryViewModel(db.liftDao(), db.workoutDao()) }
    var compareMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("History", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        ModeSelector(compareMode, onChange = { compareMode = it })
        Spacer(Modifier.height(16.dp))
        if (compareMode) {
            CompareLifts(vm)
        } else {
            SingleLift(vm)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(compareMode: Boolean, onChange: (Boolean) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = !compareMode,
            onClick = { onChange(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) { Text("Single lift") }
        SegmentedButton(
            selected = compareMode,
            onClick = { onChange(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) { Text("Compare lifts") }
    }
}

@Composable
private fun SingleLift(vm: HistoryViewModel) {
    val lifts by vm.lifts.collectAsState(initial = emptyList())
    val selected by vm.selectedLift.collectAsState()
    val entries by vm.entries.collectAsState()
    val oneRepMaxPoints by vm.oneRepMaxPoints.collectAsState()
    val tonnagePoints by vm.tonnagePoints.collectAsState()
    val scope = rememberCoroutineScope()

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
        return
    }
    LazyColumn {
        item {
            Text("Estimated 1RM", style = MaterialTheme.typography.titleMedium)
            LineChart(points = oneRepMaxPoints)
            Spacer(Modifier.height(16.dp))
            Text("Total weight lifted", style = MaterialTheme.typography.titleMedium)
            LineChart(
                points = tonnagePoints,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompareLifts(vm: HistoryViewModel) {
    val lifts by vm.lifts.collectAsState(initial = emptyList())
    val selectedIds by vm.multiSelectedIds.collectAsState()
    val metric by vm.metric.collectAsState()
    val series by vm.multiSeries.collectAsState()
    val scope = rememberCoroutineScope()

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        lifts.forEach { lift ->
            FilterChip(
                selected = lift.id in selectedIds,
                onClick = { scope.launch { vm.toggleLift(lift) } },
                label = { Text(lift.name) },
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    MetricSelector(metric, onChange = vm::setMetric)
    Spacer(Modifier.height(16.dp))
    if (series.isEmpty()) {
        Text(
            "Choose lifts to compare.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    LineChart(
        series = series.mapIndexed { i, s ->
            ChartSeries(
                label = s.lift.name,
                points = s.points,
                color = SERIES_COLORS[i % SERIES_COLORS.size],
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricSelector(metric: HistoryMetric, onChange: (HistoryMetric) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = metric == HistoryMetric.E1RM,
            onClick = { onChange(HistoryMetric.E1RM) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) { Text("Est. 1RM") }
        SegmentedButton(
            selected = metric == HistoryMetric.TONNAGE,
            onClick = { onChange(HistoryMetric.TONNAGE) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) { Text("Total weight") }
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
