package core

import kotlinx.cinterop.*
import platform.windows.*

@OptIn(ExperimentalForeignApi::class)
actual class FileUtils {
    actual fun exists(path: String): Boolean {
        val attributes = GetFileAttributesA(path)
        return attributes != INVALID_FILE_ATTRIBUTES
    }

    actual fun createDirectory(path: String): Boolean {
        return CreateDirectoryA(path, null) != 0 || GetLastError() == ERROR_ALREADY_EXISTS.toUInt()
    }

    actual fun deleteDirectory(path: String): Boolean {
        return RemoveDirectoryA(path) != 0
    }

    actual fun copyFile(source: String, destination: String): Boolean {
        return CopyFileA(source, destination, 0) != 0
    }

    actual fun writeTextFile(path: String, content: String): Boolean {
        val bytes = content.encodeToByteArray()
        val handle = CreateFileA(
            path,
            GENERIC_WRITE.convert(),
            0u,
            null,
            CREATE_ALWAYS.convert(),
            FILE_ATTRIBUTE_NORMAL.convert(),
            null
        )

        if (handle == INVALID_HANDLE_VALUE) return false

        return try {
            memScoped {
                val written = alloc<DWORDVar>()
                val result = WriteFile(handle, bytes.refTo(0).getPointer(this), bytes.size.convert(), written.ptr, null)
                result != 0
            }
        } finally {
            CloseHandle(handle)
        }
    }

    actual fun readTextFile(path: String): String? {
        val handle = CreateFileA(
            path,
            GENERIC_READ,
            FILE_SHARE_READ.convert(),
            null,
            OPEN_EXISTING.convert(),
            FILE_ATTRIBUTE_NORMAL.convert(),
            null
        )

        if (handle == INVALID_HANDLE_VALUE) return null

        return try {
            memScoped {
                val fileSize = GetFileSize(handle, null)
                if (fileSize == INVALID_FILE_SIZE) return@memScoped null

                val size = fileSize.toInt()
                val buffer = ByteArray(size)
                val read = alloc<DWORDVar>()

                if (ReadFile(handle, buffer.refTo(0).getPointer(this), size.convert(), read.ptr, null) != 0) {
                    buffer.decodeToString()
                } else {
                    null
                }
            }
        } finally {
            CloseHandle(handle)
        }
    }

    actual fun makeExecutable(path: String): Boolean {
        // On Windows, executable permissions are handled by file extension
        return true
    }

    actual fun getFileSize(path: String): Long {
        val handle = CreateFileA(
            path,
            GENERIC_READ,
            FILE_SHARE_READ.convert(),
            null,
            OPEN_EXISTING.convert(),
            FILE_ATTRIBUTE_NORMAL.convert(),
            null
        )

        if (handle == INVALID_HANDLE_VALUE) return -1L

        return try {
            val fileSize = GetFileSize(handle, null)
            if (fileSize != INVALID_FILE_SIZE) fileSize.toLong() else -1L
        } finally {
            CloseHandle(handle)
        }
    }

    actual fun calculateChecksum(path: String): String {
        // Simple checksum implementation using file size and name hash
        val size = getFileSize(path)
        val nameHash = path.hashCode()
        return (size + nameHash).toString(16)
    }
}