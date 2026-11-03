package com.example.data

import java.util.zip.CRC32
import android.util.Base64

class QrFountainEncoder(
    val payload: ByteArray,
    val sessionId: String,
    val key: ByteArray,
    val iv: ByteArray,
    val chunkSize: Int = optimalChunkSize(payload.size)
) {
    companion object {
        fun optimalChunkSize(payloadBytes: Int): Int = when {
            payloadBytes <= 1200 -> minOf(payloadBytes.coerceAtLeast(1), 400)
            payloadBytes <= 4000 -> 480
            else -> 550
        }
    }

    val k = ((payload.size + chunkSize - 1) / chunkSize).coerceAtLeast(1)
    val chunks = Array(k) { i ->
        val start = i * chunkSize
        val end = minOf(start + chunkSize, payload.size)
        if (start < end) payload.copyOfRange(start, end) else ByteArray(0)
    }
    
    fun getInitPacket(): ByteArray {
        val keyB64 = Base64.encodeToString(key, Base64.NO_WRAP)
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val str = "S1|$sessionId|V2|$k|${payload.size}|GCM|SYSFOUNTAIN|$keyB64|$ivB64"
        return str.toByteArray(Charsets.ISO_8859_1)
    }
    
    fun getSystematicPacket(index: Int): ByteArray {
        val chunk = chunks[index]
        val header = "P|$sessionId|0|$index|".toByteArray(Charsets.ISO_8859_1)
        
        val crc = CRC32().apply { update(chunk) }.value
        val crcStr = "|${crc.toString(16)}"
        
        val result = ByteArray(header.size + chunk.size + crcStr.toByteArray(Charsets.ISO_8859_1).size)
        System.arraycopy(header, 0, result, 0, header.size)
        System.arraycopy(chunk, 0, result, header.size, chunk.size)
        System.arraycopy(crcStr.toByteArray(Charsets.ISO_8859_1), 0, result, header.size + chunk.size, crcStr.toByteArray(Charsets.ISO_8859_1).size)
        return result
    }
    
    fun getRepairPacket(packetIndex: Int): ByteArray {
        val random = java.util.Random(packetIndex.toLong())
        val p = random.nextInt(100)
        val degree = when {
            p < 10 -> 1
            p < 50 -> 2
            p < 75 -> 3
            p < 90 -> 4
            else -> 5 + random.nextInt(5)
        }.coerceAtMost(k)
        
        val indices = mutableSetOf<Int>()
        while (indices.size < degree) {
            indices.add(random.nextInt(k))
        }
        
        val mixed = ByteArray(chunkSize)
        for (idx in indices) {
            val c = chunks[idx]
            for (i in c.indices) {
                mixed[i] = (mixed[i].toInt() xor c[i].toInt()).toByte()
            }
        }
        
        val indicesStr = indices.joinToString(",")
        val header = "R|$sessionId|1|$degree|[$indicesStr]|".toByteArray(Charsets.ISO_8859_1)
        val crc = CRC32().apply { update(mixed) }.value
        val crcStr = "|${crc.toString(16)}"
        
        val result = ByteArray(header.size + mixed.size + crcStr.toByteArray(Charsets.ISO_8859_1).size)
        System.arraycopy(header, 0, result, 0, header.size)
        System.arraycopy(mixed, 0, result, header.size, mixed.size)
        System.arraycopy(crcStr.toByteArray(Charsets.ISO_8859_1), 0, result, header.size + mixed.size, crcStr.toByteArray(Charsets.ISO_8859_1).size)
        return result
    }
}
