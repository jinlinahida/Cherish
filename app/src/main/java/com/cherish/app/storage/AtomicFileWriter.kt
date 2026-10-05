package com.cherish.app.storage

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Low-level atomic file I/O abstraction.
 * Decouples the storage serialization logic from the underlying Android platform runtime
 * to allow deterministic JVM testing while using android.util.AtomicFile on Android devices.
 */
interface AtomicFileWriter {
    val file: File
    fun exists(): Boolean
    fun readFully(): ByteArray
    fun writeBytes(bytes: ByteArray)
    fun delete()
}

/**
 * Production implementation using Android's [android.util.AtomicFile].
 */
class AndroidAtomicFileWriter(override val file: File) : AtomicFileWriter {
    private val atomicFile = android.util.AtomicFile(file)

    override fun exists(): Boolean = file.exists()

    override fun readFully(): ByteArray {
        return atomicFile.readFully()
    }

    override fun writeBytes(bytes: ByteArray) {
        val stream = atomicFile.startWrite()
        try {
            stream.write(bytes)
            atomicFile.finishWrite(stream)
        } catch (e: Throwable) {
            atomicFile.failWrite(stream)
            throw IOException("Failed to write to atomic file: ${file.path}", e)
        }
    }

    override fun delete() {
        atomicFile.delete()
    }
}

/**
 * Pure JVM implementation mirroring android.util.AtomicFile semantics using a .bak file.
 * Used for fast, reliable JVM unit tests without Android mock stubs.
 */
class JvmAtomicFileWriter(override val file: File) : AtomicFileWriter {
    private val backupFile = File(file.path + ".bak")

    override fun exists(): Boolean = file.exists() || backupFile.exists()

    override fun readFully(): ByteArray {
        if (backupFile.exists()) {
            file.delete()
            backupFile.renameTo(file)
        }
        if (!file.exists()) {
            throw FileNotFoundException("File not found: ${file.path}")
        }
        return file.readBytes()
    }

    override fun writeBytes(bytes: ByteArray) {
        file.parentFile?.mkdirs()
        if (file.exists()) {
            if (!backupFile.exists()) {
                if (!file.renameTo(backupFile)) {
                    throw IOException("Failed to create backup file: ${backupFile.path}")
                }
            } else {
                file.delete()
            }
        }

        try {
            file.writeBytes(bytes)
            backupFile.delete()
        } catch (e: Throwable) {
            if (file.exists()) {
                file.delete()
            }
            if (backupFile.exists()) {
                backupFile.renameTo(file)
            }
            throw IOException("Failed to write bytes to ${file.path}", e)
        }
    }

    override fun delete() {
        file.delete()
        backupFile.delete()
    }
}
