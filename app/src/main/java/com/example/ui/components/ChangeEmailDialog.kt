package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Change Email Dialog - All domains are REAL (no fake premium domains).
 * Custom username is sent to the real API.
 */
@Composable
fun ChangeEmailDialog(
    availableDomains: List<String>,
    onDismiss: () -> Unit,
    onCreate: (customUsername: String?, domain: String?) -> Unit,
    onShowAdReward: (RewardType) -> Unit = {}
) {
    var isCustom by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var selectedDomain by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(availableDomains) {
        if (availableDomains.isNotEmpty() && (selectedDomain.isEmpty() || selectedDomain !in availableDomains)) {
            selectedDomain = availableDomains.first()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.generate_email_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Selector: Random vs Custom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !isCustom,
                        onClick = { isCustom = false },
                        modifier = Modifier.testTag("radio_random")
                    )
                    Text(
                        text = stringResource(id = R.string.generate_random),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.clickable { isCustom = false }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = isCustom,
                        onClick = { isCustom = true },
                        modifier = Modifier.testTag("radio_custom")
                    )
                    Text(
                        text = stringResource(id = R.string.generate_custom),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.clickable { isCustom = true }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom username input
                if (isCustom) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(text = stringResource(id = R.string.username_hint)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input")
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Domain Selection Dropdown - All domains are real
                Text(
                    text = stringResource(id = R.string.domain_select),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dropdownExpanded = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedDomain.ifEmpty { "Loading domains..." },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    availableDomains.forEach { dom ->
                        DropdownMenuItem(
                            text = { Text(dom) },
                            onClick = {
                                selectedDomain = dom
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val customName = if (isCustom && username.isNotBlank()) username else null
                    onCreate(customName, selectedDomain.takeIf { it.isNotEmpty() })
                },
                modifier = Modifier.testTag("create_email_btn_confirm")
            ) {
                Text(text = stringResource(id = R.string.btn_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.btn_cancel))
            }
        }
    )
}
