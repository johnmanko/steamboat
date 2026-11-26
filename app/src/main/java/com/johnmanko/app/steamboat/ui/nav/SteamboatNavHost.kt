package com.johnmanko.app.steamboat.ui.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.johnmanko.app.steamboat.di.AppGraph
import com.johnmanko.app.steamboat.ui.contacts.ContactsMode
import com.johnmanko.app.steamboat.ui.contacts.ContactsRoute
import com.johnmanko.app.steamboat.ui.contacts.contactsViewModel
import com.johnmanko.app.steamboat.ui.inbox.InboxRoute
import com.johnmanko.app.steamboat.ui.inbox.inboxViewModel
import com.johnmanko.app.steamboat.ui.replies.QuickRepliesRoute
import com.johnmanko.app.steamboat.ui.settings.AboutRoute
import com.johnmanko.app.steamboat.ui.settings.LocationRoute
import com.johnmanko.app.steamboat.ui.settings.NotificationSettingsRoute
import com.johnmanko.app.steamboat.ui.settings.SettingsRoute
import com.johnmanko.app.steamboat.ui.themes.ThemesRoute
import com.johnmanko.app.steamboat.ui.thread.ThreadRoute
import com.johnmanko.app.steamboat.ui.thread.threadViewModel
import kotlinx.coroutines.launch

@Composable
fun SteamboatNavHost(graph: AppGraph) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val requested by graph.navigator.conversationId.collectAsStateWithLifecycle()
    LaunchedEffect(requested) {
        val id = requested ?: return@LaunchedEffect
        nav.navigate("thread/$id") {
            launchSingleTop = true
        }
        graph.navigator.consume()
    }
    NavHost(
        navController = nav,
        startDestination = "inbox",
        enterTransition = { slideInHorizontally(tween(BACK_MS)) { width -> width } },
        exitTransition = { slideOutHorizontally(tween(BACK_MS)) { width -> -width / 4 } },
        popEnterTransition = { slideInHorizontally(tween(BACK_MS)) { width -> -width / 4 } + fadeIn(tween(BACK_MS)) },
        popExitTransition = { backExit() },
        predictivePopEnterTransition = { _ ->
            slideInHorizontally(tween(BACK_MS)) { width -> -width / 4 } + fadeIn(tween(BACK_MS))
        },
        predictivePopExitTransition = { _ -> backExit() },
    ) {
        composable("inbox") {
            InboxRoute(
                viewModel = inboxViewModel(graph.repository),
                notificationSettings = graph.notificationSettings,
                onOpenThread = { id -> nav.navigate("thread/$id") },
                onOpenMessage = { conversationId, messageId ->
                    nav.navigate("thread/$conversationId?messageId=$messageId")
                },
                onNewMessage = { nav.navigate("contacts/new") },
                onOpenSettings = { nav.navigate("settings") },
            )
        }
        composable("settings") {
            SettingsRoute(
                onBack = { nav.popBackStack() },
                onOpenQuickReplies = { nav.navigate("quick-replies") },
                onOpenLocation = { nav.navigate("location") },
                onOpenNotifications = { nav.navigate("notifications") },
                onOpenThemes = { nav.navigate("themes") },
                onOpenAbout = { nav.navigate("about") },
            )
        }
        composable("about") {
            AboutRoute(onBack = { nav.popBackStack() })
        }
        composable("notifications") {
            NotificationSettingsRoute(
                preferences = graph.notificationSettings,
                notifications = graph.notifications,
                onBack = { nav.popBackStack() },
            )
        }
        composable("location") {
            LocationRoute(
                preferences = graph.location,
                onBack = { nav.popBackStack() },
            )
        }
        composable("quick-replies") {
            QuickRepliesRoute(
                quickReplies = graph.quickReplies,
                onBack = { nav.popBackStack() },
            )
        }
        composable("themes") {
            ThemesRoute(
                themes = graph.themes,
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            route = "thread/{conversationId}?messageId={messageId}&compose={compose}",
            arguments = listOf(
                navArgument("conversationId") { type = NavType.LongType },
                navArgument("messageId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("compose") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            val id = entry.arguments?.getLong("conversationId") ?: return@composable
            val messageId = entry.arguments?.getLong("messageId") ?: -1L
            val compose = entry.arguments?.getBoolean("compose") ?: false
            ThreadRoute(
                conversationId = id,
                focusMessageId = messageId,
                requestComposerFocus = compose,
                viewModel = threadViewModel(id, graph.repository, graph.navigator),
                quickReplies = graph.quickReplies,
                location = graph.location,
                notificationSettings = graph.notificationSettings,
                notifications = graph.notifications,
                onBack = { nav.popBackStack() },
                onLinkContact = { conversationId -> nav.navigate("contacts/link/$conversationId") },
                onAddPerson = { nav.navigate("contacts/add/$id") },
                onForward = { text, media ->
                    graph.navigator.stageForward(text, media)
                    nav.navigate("contacts/forward")
                },
            )
        }
        composable("contacts/forward") {
            val draft = remember { graph.navigator.takeForward() }
            ContactsRoute(
                mode = ContactsMode.Forward,
                conversationId = null,
                viewModel = contactsViewModel(graph.repository),
                onBack = { nav.popBackStack() },
                onOpenThread = { opened ->
                    scope.launch {
                        val copies = graph.repository.copyAttachments(draft.media)
                        graph.navigator.stageDraft(opened, draft.copy(media = copies))
                        nav.navigate("thread/$opened?compose=true") {
                            popUpTo("contacts/forward") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onLinked = {},
            )
        }
        composable("contacts/new") {
            ContactsRoute(
                mode = ContactsMode.NewMessage,
                conversationId = null,
                viewModel = contactsViewModel(graph.repository),
                onBack = { nav.popBackStack() },
                onOpenThread = { id ->
                    nav.navigate("thread/$id?compose=true") { popUpTo("inbox") }
                },
                onLinked = {},
            )
        }
        composable(
            route = "contacts/link/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("conversationId") ?: return@composable
            ContactsRoute(
                mode = ContactsMode.Link,
                conversationId = id,
                viewModel = contactsViewModel(graph.repository),
                onBack = { nav.popBackStack() },
                onOpenThread = {},
                onLinked = { nav.popBackStack() },
            )
        }
        composable(
            route = "contacts/add/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("conversationId") ?: return@composable
            ContactsRoute(
                mode = ContactsMode.Add,
                conversationId = id,
                viewModel = contactsViewModel(graph.repository),
                onBack = { nav.popBackStack() },
                onOpenThread = { opened ->
                    if (opened == id) {
                        nav.popBackStack()
                    } else {
                        nav.navigate("thread/$opened") { popUpTo("inbox") }
                    }
                },
                onLinked = {},
            )
        }
    }
}

private const val BACK_MS = 220

/** Slides the closed screen fully away. The default predictive pop only scales to 70%. */
private fun backExit() =
    slideOutHorizontally(tween(BACK_MS)) { width -> width } + fadeOut(tween(BACK_MS))
