package com.peternaggschga.sleeptalk.domain.monitoring

import java.io.DataOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

class WavCodec(rootDirectory: File) : Codec(rootDirectory) {
    override val fileExtension: String = ".wav"

    override fun writeAudioData(pcm: FloatArray, outputStream: DataOutputStream) {
        val fileBuffer =
            ByteBuffer.allocate(44 + pcm.size * Short.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        writeHeader(fileBuffer, pcm.size)
        writePcmData(fileBuffer, pcm)

        for (index in 0 until fileBuffer.position()) {
            outputStream.writeByte(fileBuffer.get(index).toInt())
        }
    }

    private fun writeHeader(buffer: ByteBuffer, nrOfSamples: Int) {
        buffer.apply {
            // numbers are little endian, strings big endian
            // since buffer assumes little endian, strings must be reversed
            put("RIFF".toByteArray()) // RIFF 4CC
            putInt(36) // size of the rest of this header chunk
            put("WAVE".toByteArray()) // format
            put("fmt ".toByteArray()) // id of 1st sub-chunk
            putInt(16) // Size of sub-chunk
            putShort(1) // audio format (PCM = 1)
            putShort(1) // number of channels
            putInt(AudioRecordFactory.SAMPLE_RATE) // sampling rate
            putInt(AudioRecordFactory.SAMPLE_RATE * Short.SIZE_BYTES) // size of data
            putShort(Short.SIZE_BYTES.toShort()) // block alignment
            putShort(Short.SIZE_BITS.toShort()) // size of one sample
            put("data".toByteArray()) // id of data chunk
            putInt(nrOfSamples * Short.SIZE_BYTES) // size of data chunk
        }
    }

    private fun writePcmData(buffer: ByteBuffer, pcm: FloatArray) {
        pcm.map { fl -> floatToShort(fl) }.forEach { sh -> buffer.putShort(sh) }
    }

    private fun floatToShort(float: Float): Short {
        var result = (float.toDouble() * Short.MAX_VALUE).toInt()
        result = min(result, Short.MAX_VALUE.toInt())
        result = max(result, Short.MIN_VALUE.toInt())
        return result.toShort()
    }
}
