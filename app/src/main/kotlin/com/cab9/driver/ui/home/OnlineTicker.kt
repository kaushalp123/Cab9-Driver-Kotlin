package com.cab9.driver.ui.home

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.Driver
import com.cab9.driver.data.models.MobileState
import com.cab9.driver.di.user.UserComponentManager
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import timber.log.Timber
import javax.inject.Inject

@ActivityScoped
class OnlineTicker @Inject constructor(
    private val activityRef: Activity,
    private val userComponentManager: UserComponentManager
) : DefaultLifecycleObserver {

    private val externalScope: CoroutineScope
        get() = (activityRef as AppCompatActivity).lifecycleScope

    // Backing property to avoid flow emissions from other classes
    private val _timerFlow = MutableSharedFlow<String>(replay = 0)
    val timerFlow: SharedFlow<String> = _timerFlow

    private var driverStatus: Driver.Status? = null
    private var timerJob: Job? = null

    init {
        (activityRef as AppCompatActivity).lifecycle.addObserver(this)
        externalScope.launch {
            userComponentManager.cab9Repo.mobileStateAsFlow.collectLatest {
                if (it is Outcome.Success) {
                    if (driverStatus == it.data.driverStatus) return@collectLatest
                    driverStatus = it.data.driverStatus
                    if (driverStatus == Driver.Status.OFFLINE) stop() else start(it.data)
                }
            }
        }
    }

    private fun start(newState: MobileState) {
        if (timerJob == null)
            newState.currentShift?.startTime?.let { startTime ->
                Timber.d("Online shift timer started...".uppercase())
                timerJob = externalScope.launch {
                    while (true) {
                        _timerFlow.emit(formatElapsedTime(startTime))
                        delay(1000)
                    }
                }
            }
    }

    private fun formatElapsedTime(startTime: Instant): String {
        var duration = Duration.between(startTime, Instant.now())
        if (duration.isNegative) duration = duration.negated()
        return "%02d:%02d:%02d".format(
            duration.toHours(),
            duration.toMinutes() % 60,
            duration.toSecondsPart()
        )
    }

    private fun stop() {
        timerJob?.cancel()
        timerJob = null
        Timber.d("Online shift timer stopped!".uppercase())
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stop()
        super.onDestroy(owner)
    }

}