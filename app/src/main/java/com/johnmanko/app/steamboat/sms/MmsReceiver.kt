package com.johnmanko.app.steamboat.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Required so Steamboat can be the default SMS app. Multimedia messages are not composed here.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}
