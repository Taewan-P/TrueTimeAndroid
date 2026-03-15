package dev.chungjungsoo.truetime.controller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.chungjungsoo.truetime.model.ClockSnapshot
import dev.chungjungsoo.truetime.model.TrustedClockModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MainController
    @Inject
    constructor(
        private val trustedClockModel: TrustedClockModel,
    ) : ViewModel() {
        data class UiState(
            val currentTime: String = "--:--:--",
            val lastUpdated: String = "Unknown",
            val offsetMillis: Long = 0L,
            val estimatedErrorMillis: Long = 0L,
            val driftMillis: Long = 0L,
            val corrected: Boolean = false,
            val clientReady: Boolean = false,
            val secondInMinute: Int = 0,
            val minuteProgress: Float = 0f,
        )

        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        private val liveTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        private var latestSnapshot: ClockSnapshot? = null
        private var tickerJob: Job? = null
        private var initialized = false

        private val _uiState = MutableStateFlow(UiState())
        val uiState = _uiState.asStateFlow()

        fun initialize() {
            if (initialized) return
            initialized = true
            viewModelScope.launch {
                runCatching { trustedClockModel.initialize() }
                    .onSuccess {
                        _uiState.update { it.copy(clientReady = true) }
                        updateSnapshot()
                    }
            }
        }

        fun startTicker() {
            if (tickerJob?.isActive == true) return
            tickerJob =
                viewModelScope.launch {
                    if (latestSnapshot == null) {
                        updateSnapshot()
                    }
                    while (isActive) {
                        val base = latestSnapshot
                        if (base != null) {
                            val adjustedMillis =
                                System.currentTimeMillis() +
                                    base.offsetMillis
                            val secondInMinute = ((adjustedMillis / 1000L) % 60L).toInt()
                            val minuteProgress = (adjustedMillis % 60_000L).toFloat() / 60_000f
                            _uiState.update {
                                it.copy(
                                    currentTime = formatLiveTime(adjustedMillis),
                                    secondInMinute = secondInMinute,
                                    minuteProgress = minuteProgress,
                                )
                            }
                        }
                        delay(TICKER_INTERVAL_MS)
                    }
                }
        }

        fun stopTicker() {
            tickerJob?.cancel()
            tickerJob = null
        }

        fun refresh() {
            viewModelScope.launch { updateSnapshot() }
        }

        private suspend fun updateSnapshot() {
            val snapshot = trustedClockModel.refreshSnapshot() ?: return
            latestSnapshot = snapshot
            _uiState.update {
                it.copy(
                    lastUpdated = formatDateTime(snapshot.epochMillis),
                    offsetMillis = snapshot.offsetMillis,
                    estimatedErrorMillis = snapshot.estimatedErrorMillis,
                    driftMillis = snapshot.driftMillis,
                    corrected = snapshot.corrected,
                )
            }
        }

        private fun formatDateTime(epochMillis: Long): String =
            dateTimeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

        private fun formatLiveTime(epochMillis: Long): String =
            liveTimeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

        override fun onCleared() {
            stopTicker()
            super.onCleared()
        }

        companion object {
            private const val TICKER_INTERVAL_MS = 33L
        }
    }
