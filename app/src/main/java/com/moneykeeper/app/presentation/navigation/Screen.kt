package com.moneykeeper.app.presentation.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object AddTransaction : Screen("add_transaction")
    object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun route(transactionId: Long) = "edit_transaction/$transactionId"
    }
    object PendingEvents : Screen("pending_events")
    object NotificationLog : Screen("notification_log")
    object Trash : Screen("trash")
    object PatternLibrary : Screen("pattern_library")
    object RegexBuilder : Screen("regex_builder/{logId}") {
        fun route(logId: Long) = "regex_builder/$logId"
    }
    object NotificationDetail : Screen("notification_detail/{logId}") {
        fun route(logId: Long) = "notification_detail/$logId"
    }
}
