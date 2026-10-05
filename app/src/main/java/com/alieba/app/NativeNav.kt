package com.alieba.app

import android.app.Activity
import android.view.View

object NativeNav {
    fun make(activity: Activity, section: String): View =
        AliebaBottomNav.make(activity, section)
}
