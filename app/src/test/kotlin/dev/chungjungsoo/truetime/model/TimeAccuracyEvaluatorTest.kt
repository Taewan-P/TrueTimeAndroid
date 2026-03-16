package dev.chungjungsoo.truetime.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeAccuracyEvaluatorTest {
    private val evaluator = TimeAccuracyEvaluator()

    @Test
    fun `keeps trusted time when drift is inside threshold`() {
        val result = evaluator.verify(trustedMillis = 1_000_000L, ntpMillis = 999_970L, maxAcceptedDriftMillis = 50L)

        assertEquals(1_000_000L, result.correctedEpochMillis)
        assertEquals(30L, result.driftMillis)
        assertFalse(result.usedManualNtpCorrection)
    }

    @Test
    fun `uses ntp time when drift exceeds threshold`() {
        val result = evaluator.verify(trustedMillis = 1_000_000L, ntpMillis = 999_900L, maxAcceptedDriftMillis = 50L)

        assertEquals(999_900L, result.correctedEpochMillis)
        assertEquals(100L, result.driftMillis)
        assertTrue(result.usedManualNtpCorrection)
    }

    @Test
    fun `falls back to trusted time when ntp unavailable`() {
        val result = evaluator.verify(trustedMillis = 1_000_000L, ntpMillis = null)

        assertEquals(1_000_000L, result.correctedEpochMillis)
        assertEquals(0L, result.driftMillis)
        assertFalse(result.usedManualNtpCorrection)
    }
}
