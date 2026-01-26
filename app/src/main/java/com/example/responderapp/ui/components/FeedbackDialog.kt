package com.example.responderapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.responderapp.data.model.FeedbackType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    onSubmit: (FeedbackType, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(FeedbackType.SUGGESTION) }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Feedback") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("What would you like to share?", style = MaterialTheme.typography.bodyMedium)
                
                // Feedback Type Selection
                Column(Modifier.selectableGroup()) {
                    FeedbackType.values().forEach { type ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .selectable(
                                    selected = (type == selectedType),
                                    onClick = { selectedType = type },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (type == selectedType),
                                onClick = null // null recommended for accessibility with selectable
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Your message") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Tell us more...") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedType, message) },
                enabled = message.isNotBlank()
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
