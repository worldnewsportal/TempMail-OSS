package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.db.MessageEntity
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest

sealed class NavTab(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val labelRes: Int) {
    object Inbox : NavTab("inbox", Icons.Default.Inbox, R.string.nav_inbox)
    object Emails : NavTab("emails", Icons.Default.Email, R.string.nav_manage)
    object Search : NavTab("search", Icons.Default.Search, R.string.nav_search)
    object Settings : NavTab("settings", Icons.Default.Settings, R.string.nav_settings)
    object About : NavTab("about", Icons.Default.Info, R.string.nav_about)
}

/**
 * Represents the current compose email state (new, reply, or forward).
 */
data class ComposeState(
    val isComposing: Boolean = false,
    val replyTo: String? = null,
    val replyToSubject: String? = null,
    val replyToBody: String? = null,
    val forwardFrom: String? = null,
    val forwardSubject: String? = null,
    val forwardBody: String? = null
)

@Composable
fun MainAppContainerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf<NavTab>(NavTab.Inbox) }
    var currentDetailMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var composeState by remember { mutableStateOf(ComposeState()) }

    val context = LocalContext.current

    // Observe Toast notifications
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Compose email overlay
    if (composeState.isComposing) {
        ComposeEmailScreen(
            viewModel = viewModel,
            onSent = {
                composeState = ComposeState()
            },
            onDiscard = {
                composeState = ComposeState()
            },
            replyTo = composeState.replyTo,
            replyToSubject = composeState.replyToSubject,
            replyToBody = composeState.replyToBody,
            forwardFrom = composeState.forwardFrom,
            forwardSubject = composeState.forwardSubject,
            forwardBody = composeState.forwardBody
        )
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 600.dp

        if (isExpanded) {
            // Adaptive Tablet Layout: Side Navigation Rail + Split Panes
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.testTag("tablet_nav_rail")
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    listOf(NavTab.Inbox, NavTab.Emails, NavTab.Search, NavTab.Settings, NavTab.About).forEach { tab ->
                        NavigationRailItem(
                            selected = activeTab == tab,
                            onClick = {
                                activeTab = tab
                                if (tab != NavTab.Inbox) {
                                    currentDetailMessage = null
                                }
                            },
                            icon = { Icon(imageVector = tab.icon, contentDescription = stringResource(id = tab.labelRes)) },
                            label = { Text(text = stringResource(id = tab.labelRes)) },
                            modifier = Modifier.testTag("rail_item_${tab.route}")
                        )
                    }
                }

                // Split Pane Layout
                Row(modifier = Modifier.weight(1.5f)) {
                    Box(modifier = Modifier.weight(1f)) {
                        when (activeTab) {
                            NavTab.Inbox -> InboxScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { currentDetailMessage = it },
                                onCompose = { composeState = ComposeState(isComposing = true) }
                            )
                            NavTab.Emails -> ManageEmailsScreen(viewModel = viewModel)
                            NavTab.Search -> SearchScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { currentDetailMessage = it }
                            )
                            NavTab.Settings -> SettingsScreen(viewModel = viewModel)
                            NavTab.About -> AboutScreen()
                        }
                    }

                    // Detail Pane
                    if (activeTab == NavTab.Inbox || activeTab == NavTab.Search) {
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            if (currentDetailMessage != null) {
                                EmailDetailsScreen(
                                    viewModel = viewModel,
                                    onBack = { currentDetailMessage = null },
                                    onReply = { message ->
                                        composeState = ComposeState(
                                            isComposing = true,
                                            replyTo = message.senderEmail,
                                            replyToSubject = message.subject,
                                            replyToBody = message.textBody
                                        )
                                    },
                                    onForward = { message ->
                                        composeState = ComposeState(
                                            isComposing = true,
                                            forwardFrom = "${message.senderName} <${message.senderEmail}>",
                                            forwardSubject = message.subject,
                                            forwardBody = message.textBody
                                        )
                                    }
                                )
                            } else {
                                // Default details placeholder view with custom generated empty_inbox illustration!
                                DetailsPlaceholderView()
                            }
                        }
                    }
                }
            }
        } else {
            // Mobile Stack Layout: Bottom Navigation Bar
            Scaffold(
                floatingActionButton = {
                    if (activeTab == NavTab.Inbox || activeTab == NavTab.Emails) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                composeState = ComposeState(isComposing = true)
                            },
                            icon = { Icon(imageVector = Icons.Default.Edit, contentDescription = "Compose") },
                            text = { Text("Compose") },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("mobile_bottom_nav")
                    ) {
                        listOf(NavTab.Inbox, NavTab.Emails, NavTab.Search, NavTab.Settings, NavTab.About).forEach { tab ->
                            NavigationBarItem(
                                selected = activeTab == tab,
                                onClick = {
                                    activeTab = tab
                                    currentDetailMessage = null
                                },
                                icon = { Icon(imageVector = tab.icon, contentDescription = stringResource(id = tab.labelRes)) },
                                label = { Text(text = stringResource(id = tab.labelRes)) },
                                modifier = Modifier.testTag("nav_item_${tab.route}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (currentDetailMessage != null) {
                        EmailDetailsScreen(
                            viewModel = viewModel,
                            onBack = { currentDetailMessage = null },
                            onReply = { message ->
                                composeState = ComposeState(
                                    isComposing = true,
                                    replyTo = message.senderEmail,
                                    replyToSubject = message.subject,
                                    replyToBody = message.textBody
                                )
                            },
                            onForward = { message ->
                                composeState = ComposeState(
                                    isComposing = true,
                                    forwardFrom = "${message.senderName} <${message.senderEmail}>",
                                    forwardSubject = message.subject,
                                    forwardBody = message.textBody
                                )
                            }
                        )
                    } else {
                        when (activeTab) {
                            NavTab.Inbox -> InboxScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { currentDetailMessage = it },
                                onCompose = { composeState = ComposeState(isComposing = true) }
                            )
                            NavTab.Emails -> ManageEmailsScreen(viewModel = viewModel)
                            NavTab.Search -> SearchScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { currentDetailMessage = it }
                            )
                            NavTab.Settings -> SettingsScreen(viewModel = viewModel)
                            NavTab.About -> AboutScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailsPlaceholderView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Render beautiful floating custom generated empty inbox illustration
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(32.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_inbox_1785511616492),
                contentDescription = "Empty Inbox Illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Secure Temporary Inbox",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Select an email from the left list to read its contents and download attachments.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
