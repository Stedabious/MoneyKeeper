package com.moneykeeper.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.moneykeeper.app.presentation.screen.dashboard.DashboardScreen
import com.moneykeeper.app.presentation.screen.notificationlog.NotificationLogScreen
import com.moneykeeper.app.presentation.screen.patternlibrary.PatternLibraryScreen
import com.moneykeeper.app.presentation.screen.pending.PendingEventScreen
import com.moneykeeper.app.presentation.screen.notificationdetail.NotificationDetailScreen
import com.moneykeeper.app.presentation.screen.regexbuilder.RegexBuilderScreen
import com.moneykeeper.app.presentation.screen.transaction.AddTransactionScreen
import com.moneykeeper.app.presentation.screen.trash.TrashScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onAddClick = { navController.navigate(Screen.AddTransaction.route) },
                onPendingClick = { navController.navigate(Screen.PendingEvents.route) },
                onNotificationLogClick = { navController.navigate(Screen.NotificationLog.route) },
                onTrashClick = { navController.navigate(Screen.Trash.route) },
                onEditClick = { txId -> navController.navigate(Screen.EditTransaction.route(txId)) },
            )
        }
        composable(Screen.AddTransaction.route) {
            AddTransactionScreen(onDone = { navController.popBackStack() })
        }
        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.LongType }),
        ) {
            AddTransactionScreen(onDone = { navController.popBackStack() })
        }
        composable(Screen.PendingEvents.route) {
            PendingEventScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.NotificationLog.route) {
            NotificationLogScreen(
                onBack = { navController.popBackStack() },
                onRegexBuilder = { logId -> navController.navigate(Screen.RegexBuilder.route(logId)) },
                onPatternLibrary = { navController.navigate(Screen.PatternLibrary.route) },
                onDetail = { logId -> navController.navigate(Screen.NotificationDetail.route(logId)) },
            )
        }
        composable(Screen.Trash.route) {
            TrashScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.PatternLibrary.route) {
            PatternLibraryScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.RegexBuilder.route,
            arguments = listOf(navArgument("logId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val logId = backStackEntry.arguments?.getLong("logId") ?: 0L
            RegexBuilderScreen(logId = logId, onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.NotificationDetail.route,
            arguments = listOf(navArgument("logId") { type = NavType.LongType }),
        ) {
            NotificationDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
