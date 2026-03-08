package dev.chungjungsoo.truetime.model

data class TrustedSample(
    val trustedEpochMillis: Long,
    val estimatedErrorMillis: Long,
)

data class VerificationResult(
    val correctedEpochMillis: Long,
    val driftMillis: Long,
    val usedManualNtpCorrection: Boolean,
)

data class ClockSnapshot(
    val epochMillis: Long,
    val offsetMillis: Long,
    val estimatedErrorMillis: Long,
    val driftMillis: Long,
    val corrected: Boolean,
)
