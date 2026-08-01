package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.Arrangement
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Change Email Dialog - All domains are REAL (no fake premium domains).
 * Custom username is sent to the real API.
 *
 * Uses a scrollable radio button list instead of DropdownMenu,
 * because DropdownMenu inside AlertDialog has z-index/popup issues in Material3.
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

                // Domain Selection - Scrollable radio button list
                // (Replaces DropdownMenu which doesn't work inside AlertDialog)
                Text(
                    text = stringResource(id = R.string.domain_select),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (availableDomains.isEmpty()) {
                    Text(
                        text = "Loading domains...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(availableDomains) { dom ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = (dom == selectedDomain),
                                        onClick = { selectedDomain = dom },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (dom == selectedDomain),
                                    onClick = null // handled by selectable
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = dom,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
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
