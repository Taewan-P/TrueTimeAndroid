package dev.chungjungsoo.truetime

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.time.Ticks
import com.google.android.gms.time.TrustedTimeClient
import dagger.hilt.android.AndroidEntryPoint
import dev.chungjungsoo.truetime.data.TrustedTimeClientAccessor
import dev.chungjungsoo.truetime.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var trustedTimeClientAccessor: TrustedTimeClientAccessor
    private var trustedTimeClient: TrustedTimeClient? = null
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private val adjustedTimeStamp: Long
        get() = System.currentTimeMillis() + mainViewModel.timeOffSet.value - mainViewModel.latestEstimateError.value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setWindowInsetListener()
        initializeTimeClient()

        lifecycleScope.launch {
            mainViewModel.clientReady.collect { ready ->
                if (ready) {
                    refreshTime()
                }
            }
        }

        lifecycleScope.launch {
            while (true) {
                val currentTime = LocalDateTime
                    .ofInstant(
                        Instant.ofEpochMilli(adjustedTimeStamp),
                        ZoneId.systemDefault()
                    )
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
                binding.tvTime.text = currentTime
                delay(10L)
            }
        }

        binding.btnRefresh.setOnClickListener { refreshTime() }
    }

    private fun initializeTimeClient() {
        lifecycleScope.launch {
            try {
                trustedTimeClient = trustedTimeClientAccessor.createClient().await()
                Log.d("TimeClient", "Initialized time client")
                mainViewModel.setTimeClientReady()
            } catch (e: Exception) {
                Log.e("TimeClient", "Error initializing time client, $e")
            }
        }
    }

    private fun setWindowInsetListener() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun updateTimeOffset(offset: Long?) {
        if (offset == null) {
            Log.d("TimeOffset", "Offset is null")
        } else {
            Log.d("TimeOffset", offset.toString())
            mainViewModel.setTimeOffSet(offset)
        }
    }

    private fun updateEstimatedError(estimatedError: Long?) {
        if (estimatedError == null) {
            Log.d("TimeEstimatedError", "Estimated error is null")
        } else {
            Log.d("TimeEstimatedError", estimatedError.toString())
            mainViewModel.setLatestEstimateError(estimatedError)
        }
    }

    private fun updateLatestTick(tick: Ticks?) {
        if (tick == null) {
            Log.d("TimeTick", "Tick is null")
            return
        }

        Log.d("TimeTick", tick.toString())
        val previousTick = mainViewModel.previousTick.value
        if (tick == previousTick) {
            Log.d("TimeTick", "Tick is the same as previous tick")
        } else {
            val duration = previousTick?.durationUntil(tick)
            Log.d("TimeTick", "Duration: ${duration?.seconds}")

            mainViewModel.setPreviousTick(tick)
        }
    }

    private fun refreshTime() {
        val time = trustedTimeClient!!.latestTimeSignal?.computeCurrentInstant()
        Log.d(
            "TimeInMilliSeconds",
            "Trusted: ${time?.instantMillis}\n System: ${System.currentTimeMillis()}"
        )
        updateTimeOffset(time?.instantMillis?.minus(System.currentTimeMillis()))
        updateEstimatedError(time?.estimatedErrorMillis)
        updateLatestTick(time?.ticks)
    }

}