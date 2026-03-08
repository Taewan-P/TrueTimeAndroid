package dev.chungjungsoo.truetime.model

import javax.inject.Inject
import kotlin.math.abs

class TimeAccuracyEvaluator @Inject constructor() {
    fun verify(trustedMillis: Long, ntpMillis: Long?, maxAcceptedDriftMillis: Long = 50L): VerificationResult {
        if (ntpMillis == null) {
            return VerificationResult(
                correctedEpochMillis = trustedMillis,
                driftMillis = 0L,
                usedManualNtpCorrection = false,
            )
        }

        val drift = trustedMillis - ntpMillis
        val useCorrection = abs(drift) > maxAcceptedDriftMillis
        return VerificationResult(
            correctedEpochMillis = if (useCorrection) ntpMillis else trustedMillis,
            driftMillis = drift,
            usedManualNtpCorrection = useCorrection,
        )
    }
}
