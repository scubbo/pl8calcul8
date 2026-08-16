package com.scubbo.pl8calcul8.ui.components

import android.widget.NumberPicker
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A vertical spinner for picking from a fixed list of values, backed by the
 * classic Android NumberPicker widget (Compose has no built-in equivalent).
 */
@Composable
fun <T> NumberSpinner(
    label: String,
    options: List<T>,
    selected: T,
    display: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = remember(options) { options.map(display).toTypedArray() }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        AndroidView(
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = 0
                    displayedValues = labels
                    maxValue = labels.size - 1
                    wrapSelectorWheel = false
                    setOnValueChangedListener { _, _, newIndex -> onSelect(options[newIndex]) }
                }
            },
            update = { picker ->
                val index = options.indexOf(selected)
                if (index >= 0 && picker.value != index) picker.value = index
            },
        )
    }
}
