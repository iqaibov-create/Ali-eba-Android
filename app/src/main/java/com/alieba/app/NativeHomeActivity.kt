package com.alieba.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class NativeHomeActivity : Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        openHabibHome()
    }

    override fun onNewIntent(i: Intent) {
        super.onNewIntent(i)
        setIntent(i)
        openHabibHome()
    }

    private fun openHabibHome() {
        val target = Intent(this, MainActivity::class.java).apply {
            action = intent?.action
            data = intent?.data
            intent?.extras?.let { putExtras(it) }
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(target)
        finish()
    }
}
