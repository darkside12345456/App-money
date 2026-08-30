package com.despesas.gestor.util.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.despesas.gestor.GestorApp
import com.despesas.gestor.util.Dates
import com.despesas.gestor.util.Money
import java.util.concurrent.TimeUnit

/** Agenda (ou cancela) o lembrete diário de contas por pagar. */
object BillReminderScheduler {
    private const val WORK_NAME = "bill_reminder_daily"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<BillReminderWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

/**
 * Verifica se há contas do mês por pagar e, em caso afirmativo, mostra uma
 * notificação. Todo o processamento é local.
 */
class BillReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? GestorApp ?: return Result.success()
        val monthKey = Dates.currentMonthKey()
        val unpaid = app.container.repository.unpaidBills(monthKey)
        if (unpaid.isNotEmpty()) {
            val total = unpaid.sumOf { it.amount }
            notify(
                title = "Contas por pagar",
                text = "${unpaid.size} conta(s) deste mês por pagar · ${Money.format(total)}"
            )
        }
        return Result.success()
    }

    private fun notify(title: String, text: String) {
        val ctx = applicationContext
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lembretes de contas",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        // notify() falha em silêncio se faltar a permissão POST_NOTIFICATIONS.
        NotificationManagerCompat.from(ctx).notify(NOTIF_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "bills"
        private const val NOTIF_ID = 1001
    }
}
