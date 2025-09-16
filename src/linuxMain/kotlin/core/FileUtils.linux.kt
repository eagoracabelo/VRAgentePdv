package core

import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
actual class FileUtils {
    actual fun exists(path: String): Boolean {
        return access(path, F_OK) == 0
    }

    actual fun createDirectory(path: String): Boolean {
        return mkdir(path, 0b111111101u) == 0 || errno == EEXIST
    }

    actual fun deleteDirectory(path: String): Boolean {
        return rmdir(path) == 0
    }

    actual fun copyFile(source: String, destination: String): Boolean {
        return try {
            val sourceFile = fopen(source, "rb") ?: return false
            val destFile = fopen(destination, "wb") ?: run {
                fclose(sourceFile)
                return false
            }

            val buffer = ByteArray(4096)
            var shouldContinue = true
            while (shouldContinue) {
                memScoped {
                    val bytesRead = fread(buffer.refTo(0), 1u, buffer.size.convert(), sourceFile).toInt()
                    if (bytesRead <= 0) {
                        shouldContinue = false
                    } else {
                        fwrite(buffer.refTo(0), 1u, bytesRead.convert(), destFile)
                    }
                }
            }

            fclose(sourceFile)
            fclose(destFile)
            true
        } catch (_: Exception) {
            false
        }
    }

    actual fun writeTextFile(path: String, content: String): Boolean {
        return try {
            val file = fopen(path, "w") ?: return false
            fputs(content, file)
            fclose(file)
            true
        } catch (_: Exception) {
            false
        }
    }

    actual fun readTextFile(path: String): String? {
        return try {
            val file = fopen(path, "r") ?: return null
            val content = StringBuilder()
            val buffer = ByteArray(4096)

            var shouldContinue = true
            while (shouldContinue) {
                memScoped {
                    val bytesRead = fread(buffer.refTo(0), 1u, buffer.size.convert(), file).toInt()
                    if (bytesRead <= 0) {
                        shouldContinue = false
                    } else {
                        content.append(buffer.decodeToString(0, bytesRead))
                    }
                }
            }

            fclose(file)
            content.toString()
        } catch (_: Exception) {
            null
        }
    }

    actual fun makeExecutable(path: String): Boolean {
        return chmod(path, 0b111111101u) == 0
    }

    actual fun getFileSize(path: String): Long {
        return memScoped {
            val statBuf = alloc<stat>()
            if (stat(path, statBuf.ptr) == 0) {
                statBuf.st_size
            } else {
                -1L
            }
        }
    }

    actual fun calculateChecksum(path: String): String {
        // Simple checksum implementation using file size and name hash
        val size = getFileSize(path)
        val nameHash = path.hashCode()
        return (size + nameHash).toString(16)
    }
}