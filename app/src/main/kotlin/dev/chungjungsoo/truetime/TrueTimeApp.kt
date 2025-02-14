package dev.chungjungsoo.truetime

import android.app.Application
import android.util.Log
import com.google.android.gms.time.TrustedTimeClient
import dagger.hilt.android.HiltAndroidApp
import dev.chungjungsoo.truetime.data.TrustedTimeClientAccessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltAndroidApp
class TrueTimeApp : Application() {

    @Inject
    lateinit var trustedTimeClientAccessor: TrustedTimeClientAccessor

    var trustedTimeClient: TrustedTimeClient? = null
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val task = trustedTimeClientAccessor.createClient()

        applicationScope.launch {
            try {
                trustedTimeClient = task.await()
                Log.d("TimeClient", "Initialized time client")
            } catch (e: Exception) {
                Log.e("TimeClient", "Error initializing time client, $e")
            }
        }
    }
}