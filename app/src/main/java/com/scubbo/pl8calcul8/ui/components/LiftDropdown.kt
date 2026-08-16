package com.scubbo.pl8calcul8.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.scubbo.pl8calcul8.data.Lift

/** Dropdown for choosing a lift, with an optional "New Lift…" action. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftDropdown(
    lifts: List<Lift>,
    selected: Lift?,
    onSelect: (Lift) -> Unit,
    onNewLift: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: "Select a lift",
            onValueChange = {},
            readOnly = true,
            label = { Text("Lift") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            lifts.forEach { lift ->
                DropdownMenuItem(
                    text = { Text(lift.name) },
                    onClick = {
                        onSelect(lift)
                        expanded = false
                    },
                )
            }
            if (onNewLift != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("New Lift…") },
                    onClick = {
                        expanded = false
                        onNewLift()
                    },
                )
            }
        }
    }
}
