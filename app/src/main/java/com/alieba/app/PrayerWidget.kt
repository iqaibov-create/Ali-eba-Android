package com.alieba.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class PrayerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        AzanScheduler.refreshTimes(context)
        val p = context.getSharedPreferences("prayer_settings", Context.MODE_PRIVATE)
        val names = listOf("Fəcr", "Günəş", "Zöhr", "Əsr", "Məğrib", "İşa")
        val text = names.joinToString("   ") { n -> "$n ${p.getString("${n}_time", "--:--")}" }
        ids.forEach { id ->
            val v = RemoteViews(context.packageName, R.layout.prayer_widget)
            v.setTextViewText(R.id.widget_times, text)
            manager.updateAppWidget(id, v)
        }
    }
}
