package dev.chungjungsoo.truetime.model

import android.os.SystemClock
import com.google.android.gms.time.TrustedTimeClient
import dev.chungjungsoo.truetime.data.TrustedTimeClientAccessor
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class TrustedClockModel
@Inject
constructor(
    private val trustedTimeClientAccessor: TrustedTimeClientAccessor,
    private val ntpVerifier: NtpVerifier,
    private val timeAccuracyEvaluator: TimeAccuracyEvaluator
) {
    private var trustedTimeClient: TrustedTimeClient? = null

    suspend fun initialize() {
        if (trustedTimeClient == null) {
            trustedTimeClient = trustedTimeClientAccessor.createClient().await()
        }
    }

    suspend fun refreshSnapshot(): ClockSnapshot? {
        val sampleElapsedRealtime = SystemClock.elapsedRealtime()
        val currentInstant = trustedTimeClient?.latestTimeSignal?.computeCurrentInstant() ?: return null
        val trustedMillis = currentInstant.instantMillis
        val error = currentInstant.estimatedErrorMillis ?: 0L
        val trustedSample = TrustedSample(trustedEpochMillis = trustedMillis, estimatedErrorMillis = error)
        val ntpServer = NtpVerifier.DEFAULT_HOST
        val verification =
            verifyAgainstNtp(
                sample = trustedSample,
                sampleElapsedRealtime = sampleElapsedRealtime
            )
        val correctedMillis = verification.correctedEpochMillis
        val nowMillis = System.currentTimeMillis()

        return ClockSnapshot(
            epochMillis = correctedMillis,
            offsetMillis = correctedMillis - nowMillis,
            estimatedErrorMillis = error,
            driftMillis = verification.driftMillis,
            corrected = verification.usedManualNtpCorrection,
            ntpServer = ntpServer
        )
    }

    private suspend fun verifyAgainstNtp(
        sample: TrustedSample,
        sampleElapsedRealtime: Long,
        maxAcceptedDriftMillis: Long = 50L
    ): VerificationResult {
        val ntpMillis =
            ntpVerifier.queryNtpTimeMillis() ?: return VerificationResult(
                correctedEpochMillis = sample.trustedEpochMillis,
                driftMillis = 0L,
                usedManualNtpCorrection = false
            )
        val adjustedTrustedMillis =
            sample.trustedEpochMillis +
                (SystemClock.elapsedRealtime() - sampleElapsedRealtime)

        return timeAccuracyEvaluator.verify(
            trustedMillis = adjustedTrustedMillis,
            ntpMillis = ntpMillis,
            maxAcceptedDriftMillis = maxAcceptedDriftMillis
        )
    }
}
