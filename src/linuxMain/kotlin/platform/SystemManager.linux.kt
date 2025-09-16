package platform

import InstallationConfig
import core.FileUtils
import core.Logger
import core.Platform
import core.ProcessUtils
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock

actual class SystemManager {
    private val processUtils = ProcessUtils()
    private val fileUtils = FileUtils()
    private val logger = Logger

    actual fun isInstalled(platform: Platform): Boolean {
        // Check if salt-minion service exists or binary is present
        val serviceCheck = runBlocking {
            processUtils.execute("systemctl", listOf("list-unit-files", "salt-minion.service"))
        }

        if (serviceCheck.isSuccess && serviceCheck.stdout.contains("salt-minion.service")) {
            return true
        }

        // Check for binary in common locations
        return fileUtils.exists("/usr/bin/salt-minion") ||
               fileUtils.exists("/usr/local/bin/salt-minion") ||
               fileUtils.exists("/opt/saltstack/salt/bin/salt-minion")
    }

    actual fun getInstalledVersion(platform: Platform): String? {
        val saltPaths = listOf(
            "/usr/bin/salt-minion",
            "/usr/local/bin/salt-minion",
            "/opt/saltstack/salt/bin/salt-minion"
        )

        for (saltPath in saltPaths) {
            if (fileUtils.exists(saltPath)) {
                val versionResult = runBlocking {
                    processUtils.execute(saltPath, listOf("--version"))
                }

                if (versionResult.isSuccess) {
                    val output = versionResult.stdout.trim()
                    val parts = output.split(" ")
                    if (parts.size >= 2) return parts[1]
                }
            }
        }

        return null
    }

    actual suspend fun install(packagePath: String, config: InstallationConfig) {
        logger.info("Installing Salt-Minion from $packagePath")

        when {
            packagePath.endsWith(".deb") -> installDeb(packagePath)
            packagePath.endsWith(".rpm") -> installRpm(packagePath)
            packagePath.endsWith(".tar.gz") || packagePath.endsWith(".tgz") -> installFromTarball(packagePath, config)
            else -> throw Exception("Unsupported package format for Linux")
        }
    }

    private suspend fun installDeb(packagePath: String) {
        // Update package list first
        val updateResult = processUtils.executeAsRoot("apt-get", listOf("update"))
        if (!updateResult.isSuccess) {
            logger.warn("Failed to update package list")
        }

        // Install dependencies first
        val depsResult = processUtils.executeAsRoot("apt-get", listOf("install", "-y", "python3", "python3-pip"))
        if (!depsResult.isSuccess) {
            logger.warn("Failed to install dependencies")
        }

        // Install the package
        val installResult = processUtils.executeAsRoot("dpkg", listOf("-i", packagePath))

        if (!installResult.isSuccess) {
            logger.info("Fixing dependencies...")
            val fixResult = processUtils.executeAsRoot("apt-get", listOf("install", "-f", "-y"))

            if (!fixResult.isSuccess) {
                throw Exception("Failed to install Salt-Minion: ${installResult.stderr}")
            }
        }
    }

    private suspend fun installRpm(packagePath: String) {
        // Try different package managers
        val packageManagers = listOf(
            listOf("dnf", "localinstall", "-y"),
            listOf("yum", "localinstall", "-y"),
            listOf("zypper", "install", "-y"),
            listOf("rpm", "-i")
        )

        var lastError = ""

        for (manager in packageManagers) {
            val command = manager.toMutableList()
            command.add(packagePath)

            val result = processUtils.executeAsRoot(command[0], command.drop(1))

            if (result.isSuccess) {
                return
            } else {
                lastError = result.stderr
            }
        }

        throw Exception("Failed to install Salt-Minion with all package managers: $lastError")
    }

    private suspend fun installFromTarball(packagePath: String, config: InstallationConfig) {
        logger.info("Installing from tarball (this may take a while)")

        val tempDir = "/tmp/salt-install"
        fileUtils.createDirectory(tempDir)

        try {
            // Extract tarball
            val extractResult = processUtils.execute("tar", listOf("-xzf", packagePath, "-C", tempDir))
            if (!extractResult.isSuccess) {
                throw Exception("Failed to extract tarball: ${extractResult.stderr}")
            }

            // Find extracted directory
            val listResult = processUtils.execute("ls", listOf(tempDir))
            val extractedDir = listResult.stdout.lines().first { it.isNotBlank() }
            val fullPath = "$tempDir/$extractedDir"

            // Install using setup.py
            val installResult = processUtils.executeAsRoot("python3", listOf("$fullPath/setup.py", "install"))
            if (!installResult.isSuccess) {
                throw Exception("Failed to install from source: ${installResult.stderr}")
            }

        } finally {
            fileUtils.deleteDirectory(tempDir)
        }
    }

    actual fun createService(config: InstallationConfig) {
        logger.info("Creating systemd service")

        val serviceContent = """
[Unit]
Description=The Salt Minion
Documentation=man:salt-minion(1) file:///usr/share/doc/salt/html/contents.html https://docs.saltstack.com/en/latest/
After=network.target salt-master.service

[Service]
KillMode=process
Type=notify
NotifyAccess=all
ExecStart=/usr/bin/salt-minion
LimitNOFILE=8192
User=root
Group=root
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
        """.trimIndent()

        val serviceFile = "/etc/systemd/system/salt-minion.service"

        if (!fileUtils.writeTextFile(serviceFile, serviceContent)) {
            throw Exception("Failed to create systemd service file")
        }

        // Reload systemd
        runBlocking {
            val reloadResult = processUtils.executeAsRoot("systemctl", listOf("daemon-reload"))
            if (!reloadResult.isSuccess) {
                logger.warn("Failed to reload systemd daemon")
            }

            val enableResult = processUtils.executeAsRoot("systemctl", listOf("enable", "salt-minion"))
            if (!enableResult.isSuccess) {
                logger.warn("Failed to enable salt-minion service")
            }
        }
    }

    actual fun startService(serviceName: String) {
        runBlocking {
            val result = processUtils.executeAsRoot("systemctl", listOf("start", serviceName))
            if (!result.isSuccess) {
                // Try with service command as fallback
                val serviceResult = processUtils.executeAsRoot("service", listOf(serviceName, "start"))
                if (!serviceResult.isSuccess) {
                    throw Exception("Failed to start service $serviceName: ${result.stderr}")
                }
            }
        }
    }

    actual fun stopService(serviceName: String) {
        runBlocking {
            val result = processUtils.executeAsRoot("systemctl", listOf("stop", serviceName))
            if (!result.isSuccess) {
                // Try with service command as fallback
                processUtils.executeAsRoot("service", listOf(serviceName, "stop"))
            }
        }
    }

    actual fun removeService(serviceName: String) {
        runBlocking {
            processUtils.executeAsRoot("systemctl", listOf("disable", serviceName))
            processUtils.executeAsRoot("systemctl", listOf("stop", serviceName))
        }

        fileUtils.deleteDirectory("/etc/systemd/system/$serviceName.service")

        runBlocking {
            processUtils.executeAsRoot("systemctl", listOf("daemon-reload"))
        }
    }

    actual fun getServiceStatus(serviceName: String): String {
        val result = runBlocking {
            processUtils.execute("systemctl", listOf("is-active", serviceName))
        }

        return if (result.isSuccess) {
            result.stdout.trim()
        } else {
            // Try with service command as fallback
            val serviceResult = runBlocking {
                processUtils.execute("service", listOf(serviceName, "status"))
            }

            if (serviceResult.isSuccess) {
                when {
                    serviceResult.stdout.contains("running") -> "active"
                    serviceResult.stdout.contains("stopped") -> "inactive"
                    else -> "unknown"
                }
            } else {
                "inactive"
            }
        }
    }

    actual fun removeInstallation(platform: Platform) {
        logger.info("Removing Salt-Minion installation")

        runBlocking {
            // Try different package managers
            var removeResult = processUtils.executeAsRoot("apt-get", listOf("remove", "--purge", "-y", "salt-minion"))

            if (!removeResult.isSuccess) {
                removeResult = processUtils.executeAsRoot("dnf", listOf("remove", "-y", "salt-minion"))
                if (!removeResult.isSuccess) {
                    removeResult = processUtils.executeAsRoot("yum", listOf("remove", "-y", "salt-minion"))
                    if (!removeResult.isSuccess) {
                        removeResult = processUtils.executeAsRoot("zypper", listOf("remove", "-y", "salt-minion"))
                    }
                }
            }

            if (!removeResult.isSuccess) {
                logger.warn("Package removal failed, performing manual cleanup")
            }
        }

        // Remove configuration directories
        fileUtils.deleteDirectory("/etc/salt")
        fileUtils.deleteDirectory("/var/cache/salt")
        fileUtils.deleteDirectory("/var/log/salt")
        fileUtils.deleteDirectory("/var/run/salt")
        fileUtils.deleteDirectory("/opt/saltstack")

        // Remove binary if installed manually
        fileUtils.deleteDirectory("/usr/local/bin/salt-minion")
    }

    actual fun backup(platform: Platform) {
        logger.info("Creating backup of existing installation")

        val timestamp = Clock.System.now().epochSeconds
        val backupDir = "/tmp/salt-backup-$timestamp"

        fileUtils.createDirectory(backupDir)

        // Backup configuration
        if (fileUtils.exists("/etc/salt")) {
            fileUtils.copyFile("/etc/salt", "$backupDir/etc-salt")
        }

        // Backup logs (recent ones)
        if (fileUtils.exists("/var/log/salt")) {
            fileUtils.copyFile("/var/log/salt", "$backupDir/var-log-salt")
        }

        // Backup cache if exists
        if (fileUtils.exists("/var/cache/salt")) {
            fileUtils.copyFile("/var/cache/salt", "$backupDir/var-cache-salt")
        }

        logger.info("Backup created at: $backupDir")
    }
}