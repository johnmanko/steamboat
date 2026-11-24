package com.johnmanko.app.steamboat.data.sms

import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.ims.ImsManager
import android.telephony.ims.RegistrationManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Reads whether the carrier IMS stack currently has RCS registered.
 * Android does not give an app an API to send or receive RCS messages, so a
 * registered state only decides that text would prefer RCS and then falls
 * back to SMS. Pictures and video always use MMS.
 */
class RcsAvailability(private val context: Context) {
    fun isRegistered(): Boolean = runCatching {
        val ims = context.getSystemService(ImsManager::class.java) ?: return false
        val subscriptionId = SubscriptionManager.getDefaultSubscriptionId()
        if (subscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) return false
        val rcs = ims.getImsRcsManager(subscriptionId)
        val state = CompletableFuture<Int>()
        rcs.getRegistrationState(context.mainExecutor) { state.complete(it) }
        state.get(1, TimeUnit.SECONDS) == RegistrationManager.REGISTRATION_STATE_REGISTERED
    }.getOrDefault(false)
}
