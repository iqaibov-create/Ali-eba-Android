package com.alieba.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class PrayerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val p = context.getSharedPreferences("prayer_times", Context.MODE_PRIVATE)
        val names = listOf("Fəcr" to "05:19", "Günəş" to "06:42", "Zöhr" to "12:55", "Əsr" to "16:19", "Məğrib" to "19:26", "İşa" to "00:14")
        val text = names.joinToString("   ") { (n,d) -> "$n ${p.getString(n,d)}" }
        ids.forEach { id ->
            val v=RemoteViews(context.packageName,R.layout.prayer_widget)
            v.setTextViewText(R.id.widget_times,text)
            manager.updateAppWidget(id,v)
        }
    }
}
