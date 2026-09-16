package com.alieba.app
import android.content.Context
object AzanPrefs {
 fun sound(c:Context)=c.getSharedPreferences("azan_settings",0).getString("sound","shia")?:"shia"
 fun setSound(c:Context,v:String)=c.getSharedPreferences("azan_settings",0).edit().putString("sound",v).apply()
 fun res(c:Context)=when(sound(c)){"beautiful1"->R.raw.azan_beautiful_1;"beautiful2"->R.raw.azan_beautiful_2;else->R.raw.azan_shia}
}