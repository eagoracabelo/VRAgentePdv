package version

import core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SaltVersion(
    val version: String,
    val releaseDate: String,
    val downloads: Map<String, SaltDownload>
)

@Serializable
data class SaltDownload(
    val url: String,
    val checksum: String,
    val size: Long
)

class SaltMinionVersionManager {
    private val downloadUtils = DownloadUtils()
    private val fileUtils = FileUtils()
    private val logger = Logger

    companion object {
        private const val VERSIONS_URL = "https://repo.saltproject.io/releases.json"
        private const val DEFAULT_VERSION = "3006.4"
    }

    suspend fun getAvailableVersions(): List<String> = withContext(Dispatchers.Default) {
        try {
            logger.info("Fetching available Salt versions...")

            val tempFile = "versions.json"
            if (downloadUtils.download(VERSIONS_URL, tempFile)) {
                val content = fileUtils.readTextFile(tempFile)
                if (content != null) {
                    val versions = Json.decodeFromString<List<SaltVersion>>(content)
                    return@withContext versions.map { it.version }.sortedDescending()
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to fetch versions from remote: ${e.message}")
        }

        // Fallback to hardcoded versions
        return@withContext listOf(DEFAULT_VERSION, "3006.3", "3006.2", "3006.1", "3005.4")
    }

    suspend fun download(version: String, platform: Platform): String = withContext(Dispatchers.Default) {
        val actualVersion = if (version == "latest") {
            getAvailableVersions().first()
        } else {
            version
        }

        logger.info("Downloading Salt-Minion version $actualVersion for ${platform.name}")

        val downloadInfo = getDownloadInfo(actualVersion, platform)
        val filename = getFilename(actualVersion, platform)
        val destination = "downloads/$filename"

        // Create downloads directory
        fileUtils.createDirectory("downloads")

        // Download with progress
        val success = downloadUtils.downloadWithChecksum(
            url = downloadInfo.url,
            destination = destination,
            expectedChecksum = downloadInfo.checksum
        ) { downloaded, total ->
            val percent = if (total > 0) (downloaded * 100 / total) else 0
            print("\rProgress: $percent% ($downloaded/$total bytes)")
        }

        println() // New line after progress

        if (!success) {
            throw Exception("Failed to download Salt-Minion version $actualVersion")
        }

        logger.info("Download completed: $destination")
        return@withContext destination
    }

    private suspend fun getDownloadInfo(version: String, platform: Platform): SaltDownload {
        // This would normally query the Salt repository
        // For now, we'll construct URLs based on known patterns

        val baseUrl = "https://packages.broadcom.com/artifactory/saltproject-generic"
        val filename = getFilename(version, platform)

        return when (platform.name.lowercase()) {
            "windows" -> SaltDownload(
                url = "$baseUrl/windows/$version/$filename",
                checksum = "", // Would be fetched from repo
                size = 50_000_000
            )
            else -> { // Linux
                val distro = detectLinuxDistro(platform)
                SaltDownload(
                    url = "$baseUrl/py3/$distro/$version/salt-minion_$version.deb",
                    checksum = "", // Would be fetched from repo
                    size = 30_000_000
                )
            }
        }
    }

    private fun getFilename(version: String, platform: Platform): String {
        return when (platform.name.lowercase()) {
            "windows" -> "Salt-Minion-$version-Py3-AMD64-Setup.exe"
            else -> "salt-minion_$version.deb" // or .rpm depending on distro
        }
    }

    private fun detectLinuxDistro(platform: Platform): String {
        return when {
            platform.name.contains("ubuntu", ignoreCase = true) -> "ubuntu/20.04"
            platform.name.contains("debian", ignoreCase = true) -> "debian/10"
            platform.name.contains("centos", ignoreCase = true) -> "centos/8"
            platform.name.contains("rhel", ignoreCase = true) -> "rhel/8"
            else -> "ubuntu/20.04" // Default fallback
        }
    }

    fun validateChecksum(filePath: String, expectedChecksum: String): Boolean {
        if (expectedChecksum.isEmpty()) return true // Skip if no checksum provided

        try {
            val actualChecksum = fileUtils.calculateChecksum(filePath)
            val isValid = actualChecksum.equals(expectedChecksum, ignoreCase = true)

            if (isValid) {
                logger.info("Checksum validation passed")
            } else {
                logger.error("Checksum validation failed. Expected: $expectedChecksum, Got: $actualChecksum")
            }

            return isValid
        } catch (e: Exception) {
            logger.error("Failed to calculate checksum: ${e.message}")
            return false
        }
    }
}