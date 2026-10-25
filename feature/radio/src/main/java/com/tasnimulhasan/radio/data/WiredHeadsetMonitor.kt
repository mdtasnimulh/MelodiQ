package com.tasnimulhasan.radio.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tells the app whether a wired headset / headphones are plugged in.
 *
 * Hardware constraint: the FM tuner chip uses the headphone cable as its antenna, so FM reception
 * needs wired headphones. Bluetooth headphones and the phone speaker are not antennas.
 *
 * The state comes from a standard [BroadcastReceiver] for [Intent.ACTION_HEADSET_PLUG] (extra
 * "state": 0 = unplugged, 1 = plugged). That broadcast is sticky, so registering also returns the
 * current state straight away. It can only be received by a receiver registered at runtime (not in
 * the manifest), so the receiver is registered only while somebody needs it: [acquire] / [release]
 * are reference counted and the receiver is removed when the last user lets go.
 */
@Singleton
class WiredHeadsetMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    private val _isPlugged = MutableStateFlow(queryPlugged())
    val isPlugged: StateFlow<Boolean> = _isPlugged.asStateFlow()

    private var users = 0

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
                _isPlugged.value = intent.getIntExtra(EXTRA_STATE, STATE_UNPLUGGED) == STATE_PLUGGED
            }
        }
    }

    @Synchronized
    fun acquire() {
        if (users++ == 0) {
            val sticky = ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(Intent.ACTION_HEADSET_PLUG),
                // System broadcasts are still delivered to a NOT_EXPORTED receiver.
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            _isPlugged.value = sticky?.let { it.getIntExtra(EXTRA_STATE, STATE_UNPLUGGED) == STATE_PLUGGED }
                ?: queryPlugged()
        }
    }

    @Synchronized
    fun release() {
        if (users > 0 && --users == 0) {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    /** Fallback for the very first value, before any broadcast has been seen. */
    private fun queryPlugged(): Boolean =
        audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        } ?: false

    private companion object {
        // Intent.ACTION_HEADSET_PLUG documents these extra names but has no constants for them.
        const val EXTRA_STATE = "state"
        const val STATE_UNPLUGGED = 0
        const val STATE_PLUGGED = 1
    }
}
