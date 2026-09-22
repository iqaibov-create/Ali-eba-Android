package com.alieba.app
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class PrayerWidget:AppWidgetProvider(){
    override fun onUpdate(c:Context,m:AppWidgetManager,ids:IntArray){
        val t=PrayerClock.times(c)
        val text=PrayerClock.displayNames.zip(PrayerClock.keys).joinToString("    "){(n,k)->"$n ${t[k]?:"--:--"}"}
        ids.forEach{ id->val v=RemoteViews(c.packageName,R.layout.prayer_widget);v.setTextViewText(R.id.widget_times,text);m.updateAppWidget(id,v)}
        PrayerClock.fetchAndSchedule(c)
    }
}
