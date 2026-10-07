package com.alieba.app

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView

class AliebaApp : Application(), Application.ActivityLifecycleCallbacks {

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    private fun dp(a: Activity, v: Int) =
        (v * a.resources.displayMetrics.density).toInt()

    override fun onActivityResumed(activity: Activity) {
        if (activity is MainActivity || activity is NativeAiActivity) return

        val content = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        if (content.findViewWithTag<View>("alieba_global_ai") != null) return

        val button = FrameLayout(activity).apply {
            tag = "alieba_global_ai"
            elevation = dp(activity, 18).toFloat()
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xffff8b60.toInt(), 0xffff6f50.toInt())
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(activity, 4), Color.WHITE)
            }
            setOnClickListener {
                activity.startActivity(Intent(activity, NativeAiActivity::class.java))
            }
        }

        button.addView(
            ImageView(activity).apply {
                setImageResource(R.drawable.ic_alieba_ai)
                setColorFilter(Color.WHITE)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12))
            },
            FrameLayout.LayoutParams(-1, -1)
        )

        content.addView(
            button,
            FrameLayout.LayoutParams(
                dp(activity, 58),
                dp(activity, 58),
                Gravity.END or Gravity.BOTTOM
            ).apply {
                rightMargin = dp(activity, 14)
                bottomMargin = dp(activity, 104)
            }
        )
    }

    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
