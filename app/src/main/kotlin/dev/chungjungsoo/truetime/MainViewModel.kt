package dev.chungjungsoo.truetime

import androidx.lifecycle.ViewModel
import com.google.android.gms.time.Ticks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel() {

    sealed class MainEvent {
        data object TimeClientReady : MainEvent()
        data object TimeClientError : MainEvent()
    }

    private val _clientReady = MutableStateFlow(false)
    val clientReady = _clientReady.asStateFlow()

    private val _timeOffSet = MutableStateFlow(0L)
    val timeOffSet = _timeOffSet.asStateFlow()

    private val _latestEstimateError = MutableStateFlow(0L)
    val latestEstimateError = _latestEstimateError.asStateFlow()

    private val _previousTick = MutableStateFlow<Ticks?>(null)
    val previousTick = _previousTick.asStateFlow()

    fun setTimeClientReady() = _clientReady.update { true }

    fun setTimeOffSet(offset: Long) = _timeOffSet.update { offset }

    fun setLatestEstimateError(error: Long) = _latestEstimateError.update { error }

    fun setPreviousTick(tick: Ticks) = _previousTick.update { tick }
}