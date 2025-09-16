package core

import kotlinx.coroutines.*

actual class DownloadUtils {

    actual suspend fun download(
        url: String,
        destination: String,
        onProgress: ((downloaded: Long, total: Long) -> Unit)?
    ): Boolean = withContext(Dispatchers.IO) {

        val processUtils = ProcessUtils()

        // Try wget first, then curl as fallback
        val wgetResult = processUtils.execute("wget", listOf(
            "--progress=bar",
            "--output-document=$destination",
            url
        ))

        if (wgetResult.isSuccess) {
            return@withContext true
        }

        // Fallback to curl
        val curlResult = processUtils.execute("curl", listOf(
            "-L",
            "-o", destination,
            url
        ))

        return@withContext curlResult.isSuccess
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