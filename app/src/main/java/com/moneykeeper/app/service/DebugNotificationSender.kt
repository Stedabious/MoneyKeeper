package com.moneykeeper.app.service

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.moneykeeper.app.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugNotificationSender @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun sendTestNotification(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "▶ SEND_FAIL permission not granted")
                return false
            }
        }

        val template = TEMPLATES.random()
        val amount = (template.minAmount..template.maxAmount).random()
        val body = template.body(amount)

        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(template.title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(DEBUG_NOTIFICATION_ID, notification)
        Log.d(TAG, "▶ SENT type=${template.type} amount=$amount title='${template.title}'")
        return true
    }

    private data class NotifTemplate(
        val type: String,
        val title: String,
        val minAmount: Int,
        val maxAmount: Int,
        val body: (Int) -> String,
    )

    companion object {
        private const val TAG = "DebugNotifSender"
        const val CHANNEL_ID = "moneykeeper_debug_test"
        private const val DEBUG_NOTIFICATION_ID = 9001

        private val EXPENSE_MERCHANTS = listOf(
            "全家便利商店", "7-ELEVEN", "麥當勞", "星巴克", "誠品書店",
            "全聯福利中心", "好市多", "IKEA", "蝦皮購物",
        )

        private val TEMPLATES = listOf(
            // ── 支出 ──────────────────────────────────────────────────────
            NotifTemplate("EXPENSE", "國泰世華 CUBE", 50, 3999) { amt ->
                val merchant = EXPENSE_MERCHANTS.random()
                "消費金額：NT$$amt\n消費地點：$merchant"
            },
            NotifTemplate("EXPENSE", "中國信託 信用卡", 100, 5999) { amt ->
                val merchant = EXPENSE_MERCHANTS.random()
                "您的信用卡消費 NT$$amt 元，消費地點：$merchant"
            },
            NotifTemplate("EXPENSE", "LINE Pay", 30, 1999) { amt ->
                "付款成功！NT$$amt 元已從 LINE Pay 扣款"
            },
            NotifTemplate("EXPENSE", "玉山銀行", 200, 8999) { amt ->
                "刷卡消費 NT$$amt 元，扣款成功"
            },

            // ── 收入 ──────────────────────────────────────────────────────
            NotifTemplate("INCOME", "台灣銀行", 30000, 65000) { amt ->
                "薪資入帳 NT$$amt 元，帳戶餘額已更新"
            },
            NotifTemplate("INCOME", "中國信託 帳戶", 500, 5000) { amt ->
                "退款入帳：NT$$amt 元已存入您的帳戶"
            },
            NotifTemplate("INCOME", "元大證券", 1000, 50000) { amt ->
                "股利入帳通知：配息 NT$$amt 元已匯入指定帳戶"
            },
            NotifTemplate("INCOME", "台新銀行", 100, 800) { amt ->
                "現金回饋 NT$$amt 元已存入您的帳戶"
            },
            NotifTemplate("INCOME", "玉山銀行", 50, 300) { amt ->
                "利息入帳 NT$$amt 元"
            },
        )
    }
}
