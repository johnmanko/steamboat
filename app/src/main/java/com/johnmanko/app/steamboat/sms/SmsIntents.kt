package com.johnmanko.app.steamboat.sms

import android.content.Intent
import android.net.Uri

object SmsIntents {
    fun recipients(intent: Intent?): List<String> {
        val data = intent?.data ?: return emptyList()
        val scheme = data.scheme ?: return emptyList()
        if (scheme != "sms" && scheme != "smsto" && scheme != "mms" && scheme != "mmsto") {
            return emptyList()
        }
        return data.schemeSpecificPart
            ?.substringBefore('?')
            .orEmpty()
            .split(';', ',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun recipient(intent: Intent?): String? = recipients(intent).firstOrNull()

    fun body(intent: Intent?): String? {
        if (intent == null) return null
        val fromQuery = intent.data?.getQueryParameter("body")
        val extra = intent.getStringExtra("sms_body") ?: intent.getStringExtra(Intent.EXTRA_TEXT)
        return (fromQuery ?: extra)?.takeIf { it.isNotBlank() }
    }
}
