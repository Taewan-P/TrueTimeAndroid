package dev.chungjungsoo.truetime

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
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
        observeClientReady()
        observeTime()
        observeDetails()

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

    private fun observeTime() {
        lifecycleScope.launch {
            while (true) {
                val currentTime = LocalDateTime
                    .ofInstant(
                        Instant.ofEpochMilli(adjustedTimeStamp),
                        ZoneId.systemDefault()
                    )
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd\nHH:mm:ss.SSS"))
                binding.tvTime.text = currentTime
                delay(10L)
            }
        }
    }

    private fun observeClientReady() {
        lifecycleScope.launch {
            mainViewModel.clientReady.collect { ready ->
                if (ready) {
                    refreshTime()
                }
            }
        }
    }

    private fun observeDetails() {
        lifecycleScope.launch {
            mainViewModel.lastUpdatedTime.collect { time ->
                if (time == null) {
                    binding.tvLastUpdatedTime.text =
                        getString(R.string.last_updated_time, getString(R.string.unknown))
                } else {
                    val lastUpdatedTime =
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault())
                            .format(
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            )
                    binding.tvLastUpdatedTime.text =
                        getString(R.string.last_updated_time, lastUpdatedTime)
                }
            }
        }

        lifecycleScope.launch {
            mainViewModel.timeOffSet.collect { offset ->
                binding.tvOffset.text = getString(R.string.time_offset, offset.toString())
            }
        }

        lifecycleScope.launch {
            mainViewModel.latestEstimateError.collect { estimatedError ->
                binding.tvEstimatedError.text =
                    getString(R.string.estimated_error, estimatedError.toString())
            }
        }

    }

    private fun updateTimeOffset(offset: Long?): Boolean {
        offset?.let {
            Log.d("TimeOffset", it.toString())
            mainViewModel.setTimeOffSet(it)
            return true
        }

        Log.d("TimeOffset", "Offset is null")
        return false
    }

    private fun updateEstimatedError(estimatedError: Long?): Boolean {
        estimatedError?.let {
            Log.d("TimeEstimatedError", it.toString())
            mainViewModel.setLatestEstimateError(it)
            return true
        }

        Log.d("TimeEstimatedError", "Estimated error is null")
        return false
    }

    private fun refreshTime() {
        val time = trustedTimeClient!!.latestTimeSignal?.computeCurrentInstant()
        Log.d(
            "TimeInMilliSeconds",
            "Trusted: ${time?.instantMillis}\n System: ${System.currentTimeMillis()}"
        )
        val offsetResult = updateTimeOffset(time?.instantMillis?.minus(System.currentTimeMillis()))
        val estimatedErrorResult = updateEstimatedError(time?.estimatedErrorMillis)
        if (offsetResult && estimatedErrorResult) {
            mainViewModel.setLastUpdatedTime(adjustedTimeStamp)
        }
    }
}