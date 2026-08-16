package com.scubbo.pl8calcul8.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onStartWorkout: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("pl8calcul8", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(48.dp))
        Button(onClick = onStartWorkout, modifier = Modifier.fillMaxWidth()) {
            Text("Start Workout", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) {
            Text("History")
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Settings")
        }
    }
}
