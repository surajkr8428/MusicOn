package com.example.musicon.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onSet: (Int, Int, Int) -> Unit
) {
    var hours by remember { mutableStateOf("0") }
    var minutes by remember { mutableStateOf("0") }
    var seconds by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Sleep Timer", fontWeight = FontWeight.Bold) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeInputUnit(value = hours, onValueChange = { hours = it }, label = "Hrs")
                Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                TimeInputUnit(value = minutes, onValueChange = { minutes = it }, label = "Min")
                Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                TimeInputUnit(value = seconds, onValueChange = { seconds = it }, label = "Sec")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = hours.toIntOrNull() ?: 0
                    val m = minutes.toIntOrNull() ?: 0
                    val s = seconds.toIntOrNull() ?: 0
                    onSet(h, m, s)
                }
            ) {
                Text("Set Timer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun TimeInputUnit(value: String, onValueChange: (String) -> Unit, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 2) onValueChange(it) },
            modifier = Modifier.width(64.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}
