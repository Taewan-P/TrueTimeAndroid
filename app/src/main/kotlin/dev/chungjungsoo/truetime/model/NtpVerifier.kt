package dev.chungjungsoo.truetime.model

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject

class NtpVerifier @Inject constructor() {

    fun queryNtpTimeMillis(host: String = "time.google.com", timeoutMs: Int = 2_000): Long? {
        return runCatching {
            val buffer = ByteArray(48)
            buffer[0] = 0b00_100_011
            val address = InetAddress.getByName(host)
            DatagramSocket().use { socket ->
                socket.soTimeout = timeoutMs
                socket.send(DatagramPacket(buffer, buffer.size, address, 123))
                val response = DatagramPacket(buffer, buffer.size)
                socket.receive(response)
            }

            val seconds = ((buffer[40].toLong() and 0xff) shl 24) or
                ((buffer[41].toLong() and 0xff) shl 16) or
                ((buffer[42].toLong() and 0xff) shl 8) or
                (buffer[43].toLong() and 0xff)
            val fraction = ((buffer[44].toLong() and 0xff) shl 24) or
                ((buffer[45].toLong() and 0xff) shl 16) or
                ((buffer[46].toLong() and 0xff) shl 8) or
                (buffer[47].toLong() and 0xff)

            val ntpEpochOffset = 2_208_988_800L
            ((seconds - ntpEpochOffset) * 1_000L) + ((fraction * 1_000L) ushr 32)
        }.getOrNull()
    }
}
