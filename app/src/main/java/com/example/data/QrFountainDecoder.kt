package com.example.data

import android.util.Base64
import java.util.zip.CRC32

class QrFountainDecoder {
    var k: Int = 0
    var totalSize: Int = 0
    var sessionId: String = ""
    var key: ByteArray? = null
    var iv: ByteArray? = null
    var isInitialized = false

    val receivedChunks = mutableMapOf<Int, ByteArray>()
    val repairPackets = mutableListOf<RepairPacket>()

    class RepairPacket(var indices: MutableSet<Int>, val data: ByteArray)

    fun processPacket(packet: ByteArray): Boolean {
        val packetStr = String(packet, Charsets.ISO_8859_1)
        if (packetStr.startsWith("S1|")) {
            val parts = packetStr.split("|")
            if (!isInitialized) {
                sessionId = parts[1]
                k = parts[3].toInt()
                totalSize = parts[4].toInt()
                key = Base64.decode(parts[7], Base64.NO_WRAP)
                iv = Base64.decode(parts[8], Base64.NO_WRAP)
                isInitialized = true
            }
            return true
        }

        val lastPipeIndex = packet.indexOfLast { it == '|'.code.toByte() }
        if (lastPipeIndex == -1) return false

        val crcStr = String(packet, lastPipeIndex + 1, packet.size - lastPipeIndex - 1, Charsets.ISO_8859_1)
        val payloadEnd = lastPipeIndex

        val firstPipes = mutableListOf<Int>()
        for (i in 0 until payloadEnd) {
            if (packet[i] == '|'.code.toByte()) firstPipes.add(i)
        }

        if (packet[0] == 'P'.code.toByte()) {
            if (firstPipes.size < 4) return false
            val headerEnd = firstPipes[3]
            val headerStr = String(packet, 0, headerEnd + 1, Charsets.ISO_8859_1)
            val parts = headerStr.split("|")
            val pSessionId = parts[1]
            if (isInitialized && pSessionId != sessionId) return false

            val index = parts[3].toInt()
            val chunk = packet.copyOfRange(headerEnd + 1, payloadEnd)

            val crc = CRC32().apply { update(chunk) }.value
            if (crc.toString(16) != crcStr) return false

            receivedChunks[index] = chunk

        } else if (packet[0] == 'R'.code.toByte()) {
            if (firstPipes.size < 5) return false
            val headerEnd = firstPipes[4]
            val headerStr = String(packet, 0, headerEnd + 1, Charsets.ISO_8859_1)
            val parts = headerStr.split("|")
            val pSessionId = parts[1]
            if (isInitialized && pSessionId != sessionId) return false

            val degree = parts[3].toInt()
            val indicesStr = parts[4].removeSurrounding("[", "]")
            val indices = indicesStr.split(",").map { it.toInt() }.toMutableSet()

            val mixed = packet.copyOfRange(headerEnd + 1, payloadEnd)

            val crc = CRC32().apply { update(mixed) }.value
            if (crc.toString(16) != crcStr) return false

            repairPackets.add(RepairPacket(indices, mixed))
        }
        return true
    }

    fun tryDecode(): ByteArray? {
        if (!isInitialized || k == 0) return null
        if (receivedChunks.size == k) {
            return buildPayload()
        }

        var progress = true
        while (progress && receivedChunks.size < k) {
            progress = false
            val iterator = repairPackets.iterator()
            while (iterator.hasNext()) {
                val packet = iterator.next()
                val knownIndices = packet.indices.filter { receivedChunks.containsKey(it) }

                for (idx in knownIndices) {
                    val knownData = receivedChunks[idx]!!
                    for (i in packet.data.indices) {
                        if (i < knownData.size) {
                            packet.data[i] = (packet.data[i].toInt() xor knownData[i].toInt()).toByte()
                        }
                    }
                    packet.indices.remove(idx)
                }

                if (packet.indices.isEmpty()) {
                    iterator.remove()
                } else if (packet.indices.size == 1) {
                    val newIndex = packet.indices.first()
                    receivedChunks[newIndex] = packet.data.copyOf()
                    iterator.remove()
                    progress = true
                }
            }
        }

        if (receivedChunks.size == k) {
            return buildPayload()
        }
        return null
    }

    private fun buildPayload(): ByteArray {
        val result = ByteArray(totalSize)
        var offset = 0
        for (i in 0 until k) {
            val chunk = receivedChunks[i]!!
            val length = minOf(chunk.size, totalSize - offset)
            System.arraycopy(chunk, 0, result, offset, length)
            offset += length
        }
        return result
    }
}
