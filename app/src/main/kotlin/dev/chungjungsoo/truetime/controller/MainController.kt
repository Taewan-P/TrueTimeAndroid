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
            val currentTime: String = "--",
            val lastUpdated: String = "Unknown",
            val offsetMillis: Long = 0L,
            val estimatedErrorMillis: Long = 0L,
            val driftMillis: Long = 0L,
            val corrected: Boolean = false,
            val clientReady: Boolean = false,
        )

        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        private var latestSnapshot: ClockSnapshot? = null
        private var tickerJob: Job? = null

        private val _uiState = MutableStateFlow(UiState())
        val uiState = _uiState.asStateFlow()

        fun initialize() {
            viewModelScope.launch {
                runCatching { trustedClockModel.initialize() }
                    .onSuccess {
                        _uiState.update { it.copy(clientReady = true) }
                        refresh()
                        startTicker()
                    }
            }
        }

        fun refresh() {
            val snapshot = trustedClockModel.refreshSnapshot() ?: return
            latestSnapshot = snapshot
            _uiState.update {
                it.copy(
                    lastUpdated = formatTime(snapshot.epochMillis),
                    offsetMillis = snapshot.offsetMillis,
                    estimatedErrorMillis = snapshot.estimatedErrorMillis,
                    driftMillis = snapshot.driftMillis,
                    corrected = snapshot.corrected,
                )
            }
        }

        private fun startTicker() {
            tickerJob?.cancel()
            tickerJob =
                viewModelScope.launch {
                    while (true) {
                        val base = latestSnapshot
                        if (base != null) {
                            val adjusted =
                                System.currentTimeMillis() +
                                    base.offsetMillis -
                                    base.estimatedErrorMillis
                            _uiState.update { it.copy(currentTime = formatTime(adjusted)) }
                        }
                        delay(100L)
                    }
                }
        }

        private fun formatTime(epochMillis: Long): String =
            formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
    }
