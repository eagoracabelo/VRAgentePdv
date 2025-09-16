package core

import kotlinx.coroutines.*

actual class DownloadUtils {

    actual suspend fun download(
        url: String,
        destination: String,
        onProgress: ((downloaded: Long, total: Long) -> Unit)?
    ): Boolean = withContext(Dispatchers.IO) {

        val processUtils = ProcessUtils()

        // Use PowerShell Invoke-WebRequest
        val psCommand = """
            Invoke-WebRequest -Uri '$url' -OutFile '$destination' -UseBasicParsing
        """.trimIndent()

        val result = processUtils.execute("powershell", listOf("-Command", psCommand))
        return@withContext result.isSuccess
    }

    actual suspend fun downloadWithChecksum(
        url: String,
        destination: String,
        expectedChecksum: String,
        onProgress: ((downloaded: Long, total: Long) -> Unit)?
    ): Boolean {

        val downloadSuccess = download(url, destination, onProgress)
        if (!downloadSuccess) return false

        if (expectedChecksum.isNotEmpty()) {
            val fileUtils = FileUtils()
            val actualChecksum = fileUtils.calculateChecksum(destination)
            return actualChecksum.equals(expectedChecksum, ignoreCase = true)
        }

        return true
    }
}