package com.johnmanko.app.steamboat.data.sms

object MessageTransport {
    const val RCS = "rcs"
    const val MMS = "mms"
    const val SMS = "sms"

    fun choose(rcsRegistered: Boolean, hasMedia: Boolean, recipients: Int = 1): String = when {
        hasMedia || recipients > 1 -> MMS
        rcsRegistered -> RCS
        else -> SMS
    }
}
