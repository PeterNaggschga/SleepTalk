package com.peternaggschga.sleeptalk.domain.soundfiles

import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

abstract class Codec(private val rootDirectory: File) {
    abstract val fileExtension: String

    fun savePcmToFile(pcm: FloatArray, fileName: String) {
        if (!rootDirectory.exists()) {
            rootDirectory.mkdirs()
        }

        val file = File(rootDirectory, fileName + fileExtension)
        file.createNewFile()

        val outStream = DataOutputStream(
            BufferedOutputStream(
                FileOutputStream(
                    file
                )
            )
        )
        writeAudioData(pcm, outStream)
        outStream.flush()
        outStream.close()
    }

    abstract fun writeAudioData(pcm: FloatArray, outputStream: DataOutputStream)
}
