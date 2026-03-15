package dev.chungjungsoo.truetime.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject

class NtpVerifier
    @Inject
    constructor() {
        suspend fun queryNtpTimeMillis(
            host: String = "time.google.com",
            timeoutMs: Int = 2_000,
        ): Long? =
            withContext(Dispatchers.IO) {
                runCatching {
                    val buffer = ByteArray(48)
                    buffer[INDEX_LEAP_VERSION_MODE] = CLIENT_MODE.toByte()
                    val address = InetAddress.getByName(host)
                    DatagramSocket().use { socket ->
                        socket.soTimeout = timeoutMs
                        val requestTimeMillis = System.currentTimeMillis()
                        writeTimestamp(buffer, INDEX_TRANSMIT_TIME, requestTimeMillis)
                        socket.send(DatagramPacket(buffer, buffer.size, address, 123))
                        val response = DatagramPacket(buffer, buffer.size)
                        socket.receive(response)
                        val responseTimeMillis = System.currentTimeMillis()

                        validateServerReply(buffer)

                        val receiveTimeMillis = readTimestamp(buffer, INDEX_RECEIVE_TIME)
                        val transmitTimeMillis = readTimestamp(buffer, INDEX_TRANSMIT_TIME)

                        responseTimeMillis +
                            calculateClockOffsetMillis(
                                requestTimeMillis = requestTimeMillis,
                                receiveTimeMillis = receiveTimeMillis,
                                transmitTimeMillis = transmitTimeMillis,
                                responseTimeMillis = responseTimeMillis,
                            )
                    }
                }.getOrNull()
            }

        internal fun calculateClockOffsetMillis(
            requestTimeMillis: Long,
            receiveTimeMillis: Long,
            transmitTimeMillis: Long,
            responseTimeMillis: Long,
        ): Long =
            ((receiveTimeMillis - requestTimeMillis) + (transmitTimeMillis - responseTimeMillis)) / 2L

        internal fun readTimestamp(
            buffer: ByteArray,
            offset: Int,
        ): Long {
            val seconds = read32(buffer, offset)
            val fraction = read32(buffer, offset + 4)
            return ((seconds - NTP_EPOCH_OFFSET_SECONDS) * MILLIS_PER_SECOND) +
                ((fraction * MILLIS_PER_SECOND) ushr 32)
        }

        internal fun writeTimestamp(
            buffer: ByteArray,
            offset: Int,
            timeMillis: Long,
        ) {
            val seconds = (timeMillis / MILLIS_PER_SECOND) + NTP_EPOCH_OFFSET_SECONDS
            val milliseconds = timeMillis % MILLIS_PER_SECOND
            val fraction = (milliseconds shl 32) / MILLIS_PER_SECOND

            write32(buffer, offset, seconds)
            write32(buffer, offset + 4, fraction)
        }

        private fun validateServerReply(buffer: ByteArray) {
            val leap = (buffer[INDEX_LEAP_VERSION_MODE].toInt() ushr 6) and 0x3
            val mode = buffer[INDEX_LEAP_VERSION_MODE].toInt() and 0x7
            val stratum = buffer[INDEX_STRATUM].toInt() and 0xff
            val transmitTimeMillis = readTimestamp(buffer, INDEX_TRANSMIT_TIME)

            require(leap != LEAP_NOT_IN_SYNC) { "NTP server is unsynchronized" }
            require(mode == MODE_SERVER || mode == MODE_BROADCAST) { "Unexpected NTP mode: $mode" }
            require(stratum in 1..15) { "Unexpected NTP stratum: $stratum" }
            require(transmitTimeMillis != 0L) { "Missing NTP transmit timestamp" }
        }

        private fun read32(
            buffer: ByteArray,
            offset: Int,
        ): Long =
            ((buffer[offset].toLong() and 0xff) shl 24) or
                ((buffer[offset + 1].toLong() and 0xff) shl 16) or
                ((buffer[offset + 2].toLong() and 0xff) shl 8) or
                (buffer[offset + 3].toLong() and 0xff)

        private fun write32(
            buffer: ByteArray,
            offset: Int,
            value: Long,
        ) {
            buffer[offset] = (value ushr 24).toByte()
            buffer[offset + 1] = (value ushr 16).toByte()
            buffer[offset + 2] = (value ushr 8).toByte()
            buffer[offset + 3] = value.toByte()
        }

        companion object {
            private const val INDEX_LEAP_VERSION_MODE = 0
            private const val INDEX_STRATUM = 1
            private const val INDEX_RECEIVE_TIME = 32
            private const val INDEX_TRANSMIT_TIME = 40
            private const val CLIENT_MODE = 0b00_100_011
            private const val MODE_SERVER = 4
            private const val MODE_BROADCAST = 5
            private const val LEAP_NOT_IN_SYNC = 3
            private const val MILLIS_PER_SECOND = 1_000L
            private const val NTP_EPOCH_OFFSET_SECONDS = 2_208_988_800L
        }
    }
