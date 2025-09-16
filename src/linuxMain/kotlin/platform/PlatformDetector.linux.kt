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
            val isRoot = checkRootPrivileges(processUtils)

            Platform(
                name = osName,
                version = osVersion,
                architecture = architecture,
                hostname = hostname,
                isRoot = isRoot
            )
        }
    }

    private suspend fun getOsName(processUtils: ProcessUtils): String {
        // Try to get from /etc/os-release
        val osReleaseResult = processUtils.execute("cat", listOf("/etc/os-release"))
        if (osReleaseResult.isSuccess) {
            val lines = osReleaseResult.stdout.split("\n")
            for (line in lines) {
                if (line.startsWith("NAME=")) {
                    return line.split("=")[1].trim('"')
                }
            }
        }

        // Fallback to uname
        val unameResult = processUtils.execute("uname", listOf("-s"))
        return if (unameResult.isSuccess) {
            unameResult.stdout.trim()
        } else {
            "Linux"
        }
    }

    private suspend fun getOsVersion(processUtils: ProcessUtils): String {
        // Try to get from /etc/os-release
        val osReleaseResult = processUtils.execute("cat", listOf("/etc/os-release"))
        if (osReleaseResult.isSuccess) {
            val lines = osReleaseResult.stdout.split("\n")
            for (line in lines) {
                if (line.startsWith("VERSION_ID=")) {
                    return line.split("=")[1].trim('"')
                }
            }
        }

        // Fallback to uname
        val unameResult = processUtils.execute("uname", listOf("-r"))
        return if (unameResult.isSuccess) {
            unameResult.stdout.trim()
        } else {
            "unknown"
        }
    }

    private suspend fun getArchitecture(processUtils: ProcessUtils): String {
        val unameResult = processUtils.execute("uname", listOf("-m"))
        return if (unameResult.isSuccess) {
            unameResult.stdout.trim()
        } else {
            "x86_64"
        }
    }

    private suspend fun getHostname(processUtils: ProcessUtils): String {
        val hostnameResult = processUtils.execute("hostname")
        return if (hostnameResult.isSuccess) {
            hostnameResult.stdout.trim()
        } else {
            "unknown-host"
        }
    }

    private suspend fun checkRootPrivileges(processUtils: ProcessUtils): Boolean {
        val idResult = processUtils.execute("id", listOf("-u"))
        return if (idResult.isSuccess) {
            idResult.stdout.trim() == "0"
        } else {
            false
        }
    }
}