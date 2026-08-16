package com.scubbo.pl8calcul8.ui.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scubbo.pl8calcul8.data.AppDatabase
import com.scubbo.pl8calcul8.data.Lift
import com.scubbo.pl8calcul8.ui.components.NewLiftDialog
import com.scubbo.pl8calcul8.ui.components.NumberSpinner
import kotlinx.coroutines.launch

private val INCREMENT_OPTIONS: List<Double> =
    generateSequence(0.0) { it + 2.5 }.takeWhile { it <= 25.0 }.toList()

private fun incrementLabel(incrementLb: Double): String =
    if (incrementLb % 1.0 == 0.0) incrementLb.toInt().toString() else incrementLb.toString()

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val vm: SettingsViewModel = viewModel { SettingsViewModel(db.liftDao()) }
    val lifts by vm.lifts.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Lift?>(null) }
    var showNewLift by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("Lifts", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(lifts) { lift ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editing = lift }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(lift.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "+${incrementLabel(lift.incrementLb)} lb",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { showNewLift = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Add lift")
        }
        Spacer(Modifier.height(24.dp))
        Text("Backup", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Requires the backup server — coming soon.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {}, enabled = false, modifier = Modifier.weight(1f)) {
                Text("Back up now")
            }
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.weight(1f)) {
                Text("Restore")
            }
        }
    }

    editing?.let { lift ->
        EditIncrementDialog(
            lift = lift,
            onSave = { increment ->
                scope.launch {
                    vm.setIncrement(lift, increment)
                    editing = null
                }
            },
            onDismiss = { editing = null },
        )
    }

    if (showNewLift) {
        NewLiftDialog(
            onCreate = { name ->
                scope.launch {
                    vm.addLift(name)
                    showNewLift = false
                }
            },
            onDismiss = { showNewLift = false },
        )
    }
}

@Composable
private fun EditIncrementDialog(
    lift: Lift,
    onSave: (incrementLb: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var increment by remember {
        mutableStateOf(
            INCREMENT_OPTIONS.minByOrNull { kotlin.math.abs(it - lift.incrementLb) } ?: 5.0
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(lift.name) },
        text = {
            NumberSpinner(
                label = "Increment (lb)",
                options = INCREMENT_OPTIONS,
                selected = increment,
                display = ::incrementLabel,
                onSelect = { increment = it },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(increment) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
