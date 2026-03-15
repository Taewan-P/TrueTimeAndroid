package dev.chungjungsoo.truetime.model

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

    suspend fun refreshSnapshot(nowMillis: Long = System.currentTimeMillis()): ClockSnapshot? {
        val currentInstant = trustedTimeClient?.latestTimeSignal?.computeCurrentInstant() ?: return null
        val trustedMillis = currentInstant.instantMillis
        val error = currentInstant.estimatedErrorMillis ?: 0L
        val trustedSample = TrustedSample(trustedEpochMillis = trustedMillis, estimatedErrorMillis = error)
        val ntpServer = NtpVerifier.DEFAULT_HOST
        val verification = verifyAgainstNtp(trustedSample)
        val correctedMillis = verification.correctedEpochMillis

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
        maxAcceptedDriftMillis: Long = 50L
    ): VerificationResult {
        val ntpMillis =
            ntpVerifier.queryNtpTimeMillis() ?: return VerificationResult(
                correctedEpochMillis = sample.trustedEpochMillis,
                driftMillis = 0L,
                usedManualNtpCorrection = false
            )

        return timeAccuracyEvaluator.verify(
            trustedMillis = sample.trustedEpochMillis,
            ntpMillis = ntpMillis,
            maxAcceptedDriftMillis = maxAcceptedDriftMillis
        )
    }
}
