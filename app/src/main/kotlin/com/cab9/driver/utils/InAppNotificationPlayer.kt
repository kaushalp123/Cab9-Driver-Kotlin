package com.cab9.driver.utils

import android.app.Activity
import android.media.MediaPlayer
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.cab9.driver.R
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class InAppNotificationPlayer @Inject constructor(private val activityRef: Activity) :
    DefaultLifecycleObserver {

    private var mediaPlayer: MediaPlayer? = null

    init {
        (activityRef as AppCompatActivity).lifecycle.addObserver(this)
    }

    fun play(@RawRes toneResId: Int = R.raw.tone_app_alert) {
        if (mediaPlayer != null) release()
        mediaPlayer = MediaPlayer.create(activityRef.applicationContext, toneResId)
        mediaPlayer?.start()
    }

    private fun release() {
        if (mediaPlayer?.isPlaying == true) mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy(owner: LifecycleOwner) {
        release()
        super.onDestroy(owner)
    }

}