package com.moneykeeper.app.data.notification

import com.moneykeeper.app.data.repository.NotificationLogRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects duplicate financial notifications within a short time window.
 *
 * Duplicate criteria (checked in order):
 *  1. Same parsed amount within [windowMs] milliseconds
 *  2. Distinguishes same-app vs cross-app duplicates (both are suppressed)
 *
 * A duplicate log is still inserted to the DB (for visibility) but with
 * ParseStatus.DUPLICATE — no PendingEvent is created.
 */
@Singleton
class DuplicateDetector @Inject constructor(
    private val repository: NotificationLogRepository,
) {
    data class DuplicateResult(
        val isDuplicate: Boolean,
        val reason: String? = null,
        val matchedAppLabel: String? = null,
        val matchedPackage: String? = null,
    )

    /**
     * Checks whether a newly-parsed notification is a duplicate of one already recorded
     * within [windowMs] milliseconds.
     *
     * @param amount        The extracted amount from the new notification.
     * @param packageName   The package of the new notification.
     * @param windowMs      Time window (default: 5 000 ms).
     */
    suspend fun check(
        amount: Double,
        packageName: String,
        windowMs: Long = WINDOW_MS,
    ): DuplicateResult {
        val since = System.currentTimeMillis() - windowMs
        val recent = repository.findRecentWithAmount(amount, since)

        if (recent.isEmpty()) return DuplicateResult(isDuplicate = false)

        // Prefer same-app match first; fall back to cross-app
        val sameApp  = recent.firstOrNull { it.packageName == packageName }
        val crossApp = recent.firstOrNull { it.packageName != packageName }

        return when {
            sameApp != null -> DuplicateResult(
                isDuplicate = true,
                reason = "同 App 重複通知，${windowMs / 1000}s 內已有相同金額 NT\$${"%.0f".format(amount)}",
                matchedAppLabel = sameApp.appLabel,
                matchedPackage  = sameApp.packageName,
            )
            crossApp != null -> DuplicateResult(
                isDuplicate = true,
                reason = "跨 App 重複 (${crossApp.appLabel})，${windowMs / 1000}s 內相同金額 NT\$${"%.0f".format(amount)}",
                matchedAppLabel = crossApp.appLabel,
                matchedPackage  = crossApp.packageName,
            )
            else -> DuplicateResult(isDuplicate = false)
        }
    }

    companion object {
        const val WINDOW_MS = 5_000L
    }
}
