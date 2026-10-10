package com.cloudplay.runtimebridge

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import com.github.luben.zstd.ZstdInputStream
import java.io.BufferedInputStream
import java.io.InputStream

/**
 * Reads a Zstandard-compressed TAR archive without writing files.
 *
 * This is a validation step only; it does not install RootFS content. It rejects
 * paths that could escape the destination root before a future extractor is added.
 */
object RootFsArchiveValidator {
    data class Result(
        val valid: Boolean,
        val entryCount: Long,
        val detail: String
    )

    fun inspect(input: InputStream): Result {
        var count = 0L
        return try {
            ZstdInputStream(BufferedInputStream(input)).use { zstd ->
                TarArchiveInputStream(zstd).use { tar ->
                    while (true) {
                        val entry = tar.nextTarEntry ?: break
                        if (!isSafeArchivePath(entry.name)) {
                            return Result(false, count, "Unsafe archive path: ${entry.name}")
                        }
                        if (entry.isSymbolicLink && !isSafeLinkTarget(entry.name, entry.linkName)) {
                            return Result(false, count, "Unsafe symbolic link: ${entry.name} -> ${entry.linkName}")
                        }
                        if (entry.isLink && !entry.isSymbolicLink &&
                            !isSafeArchivePath(entry.linkName)) {
                            return Result(false, count, "Unsafe hard link: ${entry.name} -> ${entry.linkName}")
                        }
                        count++
                        if (entry.isFile) {
                            // Consume the entry so the TAR stream can advance correctly.
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (tar.read(buffer) != -1) { /* validate stream */ }
                        }
                    }
                }
            }
            if (count == 0L) Result(false, 0L, "Archive contains no entries.")
            else Result(true, count, "Archive structure validated; no files were extracted.")
        } catch (e: Exception) {
            Result(false, count, "Archive could not be validated: ${e.javaClass.simpleName}: ${e.message ?: "unknown error"}")
        }
    }

    private fun isSafeArchivePath(path: String): Boolean {
        if (path.isBlank() || path.startsWith("/") || path.startsWith("\\\\")) return false
        if (Regex("^[A-Za-z]:").containsMatchIn(path)) return false
        val normalized = path.replace('\\\\', '/')
        return normalized.split('/').none { it == ".." }
    }

    private fun isSafeLinkTarget(entryPath: String, target: String): Boolean {
        if (target.isBlank() || target.startsWith("\\\\")) return false
        if (Regex("^[A-Za-z]:").containsMatchIn(target)) return false

        val normalizedTarget = target.replace('\\\\', '/')
        val segments = if (normalizedTarget.startsWith("/")) {
            mutableListOf()
        } else {
            entryPath.replace('\\\\', '/').split('/').dropLast(1).toMutableList()
        }

        for (segment in normalizedTarget.split('/')) {
            when (segment) {
                "", "." -> Unit
                ".." -> if (segments.isEmpty()) return false else segments.removeAt(segments.lastIndex)
                else -> segments.add(segment)
            }
        }
        return true
    }
}
