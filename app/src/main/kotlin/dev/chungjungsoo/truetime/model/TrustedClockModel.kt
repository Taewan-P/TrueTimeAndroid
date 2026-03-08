package dev.chungjungsoo.truetime.model

import com.google.android.gms.time.TrustedTimeClient
import dev.chungjungsoo.truetime.data.TrustedTimeClientAccessor
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TrustedClockModel @Inject constructor(
    private val trustedTimeClientAccessor: TrustedTimeClientAccessor,
    private val ntpVerifier: NtpVerifier,
    private val timeAccuracyEvaluator: TimeAccuracyEvaluator,
) {
    private var trustedTimeClient: TrustedTimeClient? = null

    suspend fun initialize() {
        if (trustedTimeClient == null) {
            trustedTimeClient = trustedTimeClientAccessor.createClient().await()
        }
    }

    fun refreshSnapshot(nowMillis: Long = System.currentTimeMillis()): ClockSnapshot? {
        val currentInstant = trustedTimeClient?.latestTimeSignal?.computeCurrentInstant() ?: return null
        val trustedMillis = currentInstant.instantMillis
        val error = currentInstant.estimatedErrorMillis
        val trustedSample = TrustedSample(trustedEpochMillis = trustedMillis, estimatedErrorMillis = error)
        val verification = verifyAgainstNtp(trustedSample)
        val correctedMillis = verification.correctedEpochMillis

        return ClockSnapshot(
            epochMillis = correctedMillis,
            offsetMillis = correctedMillis - nowMillis,
            estimatedErrorMillis = error,
            driftMillis = verification.driftMillis,
            corrected = verification.usedManualNtpCorrection,
        )
    }

    private fun verifyAgainstNtp(sample: TrustedSample, maxAcceptedDriftMillis: Long = 50L): VerificationResult {
        val ntpMillis = ntpVerifier.queryNtpTimeMillis() ?: return VerificationResult(
            correctedEpochMillis = sample.trustedEpochMillis,
            driftMillis = 0L,
            usedManualNtpCorrection = false,
        )

        return timeAccuracyEvaluator.verify(
            trustedMillis = sample.trustedEpochMillis,
            ntpMillis = ntpMillis,
            maxAcceptedDriftMillis = maxAcceptedDriftMillis,
        )
    }
}
