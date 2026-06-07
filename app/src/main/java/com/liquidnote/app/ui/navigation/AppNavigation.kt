package com.liquidnote.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.liquidnote.app.ui.screens.HomeScreen
import com.liquidnote.app.ui.screens.NoteEditorScreen
import com.liquidnote.app.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val EDITOR = "editor"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNoteClick = { noteId ->
                    navController.navigate("${Routes.EDITOR}?noteId=$noteId")
                },
                onNewNote = {
                    navController.navigate(Routes.EDITOR)
                },
                onSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }
        composable(
            route = "${Routes.EDITOR}?noteId={noteId}",
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            NoteEditorScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
