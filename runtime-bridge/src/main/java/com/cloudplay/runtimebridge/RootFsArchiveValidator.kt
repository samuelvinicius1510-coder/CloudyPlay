package com.cloudplay.runtimebridge

import com.github.luben.zstd.ZstdInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.InputStream

/**
 * Read-only validation for Zstandard-compressed TAR archives.
 *
 * This deliberately does not extract files. It checks paths, links, duplicate
 * names, entry count, and declared expanded size before an installer is added.
 */
object RootFsArchiveValidator {
    private const val MAX_ENTRIES = 250_000L
    private const val MAX_DECLARED_BYTES = 16L * 1024L * 1024L * 1024L

    data class Result(
        val valid: Boolean,
        val entryCount: Long,
        val declaredBytes: Long,
        val detail: String
    )

    fun inspect(input: InputStream): Result {
        var count = 0L
        var totalBytes = 0L
        val names = HashSet<String>()
        return try {
            ZstdInputStream(BufferedInputStream(input)).use { zstd ->
                TarArchiveInputStream(zstd).use { tar ->
                    while (true) {
                        val entry = tar.nextTarEntry ?: break
                        val name = normalize(entry.name)
                        if (!isSafeArchivePath(entry.name)) {
                            return Result(false, count, totalBytes, "Unsafe archive path: ${entry.name}")
                        }
                        if (!names.add(name)) {
                            return Result(false, count, totalBytes, "Duplicate archive path: ${entry.name}")
                        }
                        if (entry.isSymbolicLink && !isSafeLinkTarget(entry.name, entry.linkName)) {
                            return Result(false, count, totalBytes, "Unsafe symbolic link: ${entry.name} -> ${entry.linkName}")
                        }
                        if (entry.isLink && !entry.isSymbolicLink &&
                            !isSafeArchivePath(entry.linkName)) {
                            return Result(false, count, totalBytes, "Unsafe hard link: ${entry.name} -> ${entry.linkName}")
                        }

                        count++
                        if (count > MAX_ENTRIES) {
                            return Result(false, count, totalBytes, "Archive exceeds the entry-count limit.")
                        }
                        if (entry.size > 0) {
                            totalBytes += entry.size
                            if (totalBytes > MAX_DECLARED_BYTES) {
                                return Result(false, count, totalBytes, "Archive exceeds the expanded-size limit.")
                            }
                        }

                        if (entry.isFile) {
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (tar.read(buffer) != -1) { /* consume current entry */ }
                        }
                    }
                }
            }
            if (count == 0L) Result(false, 0L, 0L, "Archive contains no entries.")
            else Result(true, count, totalBytes, "Archive structure validated; no files were extracted.")
        } catch (e: Exception) {
            Result(false, count, totalBytes,
                "Archive could not be validated: ${e.javaClass.simpleName}: ${e.message ?: "unknown error"}")
        }
    }

    private fun normalize(path: String): String =
        path.replace("\\\\", "/").removeSuffix("/")

    private fun isSafeArchivePath(path: String): Boolean {
        if (path.isBlank() || path.startsWith("/") || path.startsWith("\\\\")) return false
        if (Regex("^[A-Za-z]:").containsMatchIn(path)) return false
        return normalize(path).split('/').none { it == ".." || it.isEmpty() }
    }

    private fun isSafeLinkTarget(entryPath: String, target: String): Boolean {
        if (target.isBlank() || target.startsWith("/") || target.startsWith("\\\\")) return false
        if (Regex("^[A-Za-z]:").containsMatchIn(target)) return false

        val segments = normalize(entryPath).split('/').dropLast(1).toMutableList()
        for (segment in normalize(target).split('/')) {
            when (segment) {
                "", "." -> Unit
                ".." -> if (segments.isEmpty()) return false else segments.removeAt(segments.lastIndex)
                else -> segments.add(segment)
            }
        }
        return true
    }
}
