package dev.chungjungsoo.truetime.model

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NtpVerifierTest {
    private val verifier = NtpVerifier()

    @Test
    fun `calculates sntp clock offset from four timestamps`() {
        val offset =
            verifier.calculateClockOffsetMillis(
                requestTimeMillis = 1_000L,
                receiveTimeMillis = 1_120L,
                transmitTimeMillis = 1_130L,
                responseTimeMillis = 1_060L
            )

        assertEquals(95L, offset)
    }

    @Test
    fun `round trips ntp timestamp encoding`() {
        val buffer = ByteArray(48)
        val expectedMillis = 1_710_000_123_456L

        verifier.writeTimestamp(buffer, 40, expectedMillis)

        val decodedMillis = verifier.readTimestamp(buffer, 40)

        assertTrue(abs(decodedMillis - expectedMillis) <= 1L)
    }

    @Test
    fun `detects missing transmit timestamp from raw packet fields`() {
        val buffer = ByteArray(48)

        assertFalse(verifier.hasTimestamp(buffer, 40))

        verifier.writeTimestamp(buffer, 40, 1_710_000_123_456L)

        assertTrue(verifier.hasTimestamp(buffer, 40))
    }
}
