package com.routina.bite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.routina.bite.R

/** 一杯與一瓶。大部分時候按這兩顆就夠了，其餘走「自訂」 */
private const val CUP_ML = 250
private const val BOTTLE_ML = 500

/**
 * 今天喝了多少水。
 *
 * 刻意做成「按一下就加一杯」而不是開表單填數字——喝水是一天要記很多次的事，
 * 每次都要打字就不會有人記。按錯就按減回去。
 */
@Composable
fun WaterCard(
    ml: Int,
    target: Int,
    onAdd: (Int) -> Unit
) {
    var customOpen by remember { mutableStateOf(false) }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = stringResource(R.string.water_title),
                    style = MaterialTheme.typography.titleSmall.zh(),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.water_amount, ml, target),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            LinearProgressIndicator(
                progress = { progressOf(ml.toDouble(), target.toDouble()) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onAdd(CUP_ML) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.water_add, CUP_ML))
                }
                OutlinedButton(onClick = { onAdd(BOTTLE_ML) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.water_add, BOTTLE_ML))
                }
                OutlinedButton(
                    onClick = { onAdd(-CUP_ML) },
                    enabled = ml > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.water_undo, CUP_ML))
                }
                OutlinedButton(onClick = { customOpen = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.water_custom))
                }
            }
        }
    }

    if (customOpen) {
        CustomWaterDialog(
            onAdd = { onAdd(it); customOpen = false },
            onDismiss = { customOpen = false }
        )
    }
}

@Composable
private fun CustomWaterDialog(onAdd: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val amount = text.toDoubleOrNull()?.toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.water_custom_title)) },
        text = {
            NumberField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(R.string.water_field),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = amount != null && amount > 0,
                onClick = { amount?.let(onAdd) }
            ) { Text(stringResource(R.string.action_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
