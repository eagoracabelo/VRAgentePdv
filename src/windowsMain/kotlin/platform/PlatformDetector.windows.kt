package platform

import core.Platform
import core.ProcessUtils
import kotlinx.coroutines.runBlocking

actual class PlatformDetector {
    actual fun detect(): Platform {
        val processUtils = ProcessUtils()

        return runBlocking {
            val osName = getOsName(processUtils)
            val osVersion = getOsVersion(processUtils)
            val architecture = getArchitecture(processUtils)
            val hostname = getHostname(processUtils)
            val isAdmin = checkAdminPrivileges(processUtils)

            Platform(
                name = osName,
                version = osVersion,
                architecture = architecture,
                hostname = hostname,
                isRoot = isAdmin
            )
        }
    }

    private suspend fun getOsName(processUtils: ProcessUtils): String {
        val result = processUtils.execute(
            "wmic",
            listOf("os", "get", "Caption", "/value")
        )

        return if (result.isSuccess) {
            val lines = result.stdout.split("\n")
            val captionLine = lines.find { it.startsWith("Caption=") }
            captionLine?.substringAfter("Caption=")?.trim() ?: "Windows"
        } else {
            "Windows"
        }
    }

    private suspend fun getOsVersion(processUtils: ProcessUtils): String {
        val result = processUtils.execute(
            "wmic",
            listOf("os", "get", "Version", "/value")
        )

        return if (result.isSuccess) {
            val lines = result.stdout.split("\n")
            val versionLine = lines.find { it.startsWith("Version=") }
            versionLine?.substringAfter("Version=")?.trim() ?: "unknown"
        } else {
            "unknown"
        }
    }

    private suspend fun getArchitecture(processUtils: ProcessUtils): String {
        val result = processUtils.execute(
            "wmic",
            listOf("os", "get", "OSArchitecture", "/value")
        )

        return if (result.isSuccess) {
            val lines = result.stdout.split("\n")
            val archLine = lines.find { it.startsWith("OSArchitecture=") }
            archLine?.substringAfter("OSArchitecture=")?.trim() ?: "x64"
        } else {
            "x64"
        }
    }

    private suspend fun getHostname(processUtils: ProcessUtils): String {
        val result = processUtils.execute("hostname")
        return if (result.isSuccess) {
            result.stdout.trim()
        } else {
            "unknown-host"
        }
    }

    private suspend fun checkAdminPrivileges(processUtils: ProcessUtils): Boolean {
        val result = processUtils.execute(
            "powershell",
            listOf("-Command", "([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)")
        )

        return if (result.isSuccess) {
            result.stdout.trim().equals("True", ignoreCase = true)
        } else {
            false
        }
    }
}