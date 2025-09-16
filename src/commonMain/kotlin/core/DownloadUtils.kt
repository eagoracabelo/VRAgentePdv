package core

expect class DownloadUtils() {

    suspend fun download(
        url: String,
        destination: String,
        onProgress: ((downloaded: Long, total: Long) -> Unit)? = null
    ): Boolean

    suspend fun downloadWithChecksum(
        url: String,
        destination: String,
        expectedChecksum: String,
        onProgress: ((downloaded: Long, total: Long) -> Unit)? = null
    ): Boolean
}