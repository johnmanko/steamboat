package com.johnmanko.app.steamboat.data.sms

import android.content.Context
import android.os.Build
import android.telephony.CarrierConfigManager
import android.telephony.SmsManager
import com.johnmanko.app.steamboat.domain.MessageAttachments

/** The carrier's MMS size cap, or 300 KB when the device does not publish one. */
class MmsBudget(private val context: Context) {
    fun maxBytes(): Int {
        val configured = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
                .carrierConfigValues
                .getInt(CarrierConfigManager.KEY_MMS_MAX_MESSAGE_SIZE_INT, 0)
        } else {
            0
        }
        return if (configured > 0) configured else MessageAttachments.DEFAULT_MAX_BYTES
    }
}
