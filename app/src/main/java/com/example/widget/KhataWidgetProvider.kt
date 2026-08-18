package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.KhataDatabase
import com.example.data.model.TransactionType
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class KhataWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_daily_khata)

        // PendingIntent to open MainActivity on click
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        val now = System.currentTimeMillis()
        views.setTextViewText(R.id.widget_date, DateUtils.formatShortDate(now))

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = KhataDatabase.getDatabase(context)
                val startOfDay = DateUtils.getStartOfDay(now)
                val endOfDay = DateUtils.getEndOfDay(now)
                val transactions = db.transactionDao().getTransactionsByDateRange(startOfDay, endOfDay).firstOrNull() ?: emptyList()

                var earning = 0.0
                var expense = 0.0
                var loanGiven = 0.0
                var loanTaken = 0.0

                for (tx in transactions) {
                    when (tx.type) {
                        TransactionType.EARNING -> earning += tx.amount
                        TransactionType.EXPENSE -> expense += tx.amount
                        TransactionType.LOAN_GIVEN -> loanGiven += tx.amount
                        TransactionType.LOAN_TAKEN -> loanTaken += tx.amount
                    }
                }

                val netSavings = earning - expense - loanGiven + loanTaken

                views.setTextViewText(R.id.widget_earnings, CurrencyUtils.format(earning))
                views.setTextViewText(R.id.widget_expenses, CurrencyUtils.format(expense))
                views.setTextViewText(R.id.widget_net_savings, CurrencyUtils.format(netSavings))

                val savingsColor = if (netSavings >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#EF5350")
                views.setTextColor(R.id.widget_net_savings, savingsColor)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, KhataWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, KhataWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
