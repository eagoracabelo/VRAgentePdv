import config.ConfigurationGenerator
import core.Logger
import core.Platform
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import network.NetworkConfigurator
import platform.PlatformDetector
import platform.SystemManager
import version.SaltMinionVersionManager

fun main(args: Array<String>) = runBlocking {
    println("=== VRPdv - Salt-Minion v1.0.0 ===")
    println()

    val installer = SaltMinionInstaller()

    try {
        when {
            args.contains("--help") || args.contains("-h") -> showHelp()
            args.contains("--silent") -> installer.runSilentInstall(args)
            args.contains("--uninstall") -> installer.uninstall()
            args.contains("--version") -> showVersion()
            args.contains("--check") -> installer.checkInstallation()
            else -> installer.runInteractiveInstall()
        }
    } catch (e: Exception) {
        Logger.error("Installation failed: ${e.message}")
        println("Installation failed. Check logs for details.")
        kotlin.system.exitProcess(1)
    }
}

class SaltMinionInstaller {
    private val platformDetector = PlatformDetector()
    private val versionManager = SaltMinionVersionManager()
    private val networkConfig = NetworkConfigurator()
    private val configGenerator = ConfigurationGenerator()
    private val systemManager = SystemManager()
    private val logger = Logger

    suspend fun runInteractiveInstall() {
        logger.info("Starting interactive installation")

        // 1. Detect platform and validate
        val platform = platformDetector.detect()
        println("Detected platform: ${platform.name} ${platform.version} (${platform.architecture})")

        if (!validatePlatformSupport(platform)) {
            throw InstallationException("Platform ${platform.name} is not supported")
        }

        // 2. Check if already installed
        if (checkExistingInstallation()) {
            if (!confirmOverwrite()) {
                println("Installation cancelled.")
                return
            }
            backup()
        }

        // 3. Get installation preferences
        val config = getInstallationConfig()

        // 4. Validate master connectivity
        if (!networkConfig.validateMasterConnection(config.masterIp, config.port)) {
            if (!confirmContinueWithoutValidation()) {
                throw InstallationException("Cannot connect to Salt Master at ${config.masterIp}:${config.port}")
            }
        }

        // 5. Download and install
        downloadAndInstall(config)

        // 6. Configure
        configure(config)

        // 7. Start service
        startService()

        println("✅ Salt-Minion installation completed successfully!")
        println("Minion ID: ${config.minionId}")
        println("Master: ${config.masterIp}:${config.port}")
        println()
        println("Service status: ${systemManager.getServiceStatus("salt-minion")}")
    }

    suspend fun runSilentInstall(args: Array<String>) {
        logger.info("Starting silent installation")

        val config = parseSilentArgs(args)
        val platform = platformDetector.detect()

        if (!validatePlatformSupport(platform)) {
            throw InstallationException("Platform not supported")
        }

        if (checkExistingInstallation()) {
            backup()
        }

        downloadAndInstall(config)
        configure(config)
        startService()

        println("Silent installation completed")
    }

    suspend fun uninstall() {
        logger.info("Starting uninstallation")

        println("Uninstalling Salt-Minion...")

        // Stop service
        systemManager.stopService("salt-minion")

        // Remove service
        systemManager.removeService("salt-minion")

        // Remove files
        val platform = platformDetector.detect()
        systemManager.removeInstallation(platform)

        println("✅ Salt-Minion uninstalled successfully")
    }

    fun checkInstallation() {
        val platform = platformDetector.detect()
        val isInstalled = systemManager.isInstalled(platform)

        if (isInstalled) {
            val version = systemManager.getInstalledVersion(platform)
            val status = systemManager.getServiceStatus("salt-minion")
            val config = configGenerator.getCurrentConfig(platform)

            println("Salt-Minion Status:")
            println("  Installed: Yes")
            println("  Version: $version")
            println("  Service Status: $status")
            println("  Master: ${config?.masterIp ?: "Not configured"}")
            println("  Minion ID: ${config?.minionId ?: "Not configured"}")
        } else {
            println("Salt-Minion is not installed")
        }
    }

    private suspend fun downloadAndInstall(config: InstallationConfig) {
        println("Downloading Salt-Minion ${config.version}...")

        val downloadPath = versionManager.download(
            version = config.version,
            platform = platformDetector.detect()
        )

        println("Installing...")
        systemManager.install(downloadPath, config)

        println("Creating service...")
        systemManager.createService(config)
    }

    private suspend fun configure(config: InstallationConfig) {
        println("Configuring Salt-Minion...")

        val platform = platformDetector.detect()
        configGenerator.generateMinionConfig(config, platform)
        configGenerator.setupDirectoryStructure(platform)
    }

    private suspend fun startService() {
        println("Starting Salt-Minion service...")
        systemManager.startService("salt-minion")

        // Wait a bit and check status
        delay(2000)
        val status = systemManager.getServiceStatus("salt-minion")

        if (status.contains("running", ignoreCase = true)) {
            println("✅ Service started successfully")
        } else {
            logger.warn("Service may not have started correctly. Status: $status")
        }
    }

    private suspend fun getInstallationConfig(): InstallationConfig {
        println("Configuration:")

        // Get available versions
        val versions = versionManager.getAvailableVersions()
        println("Available versions: ${versions.joinToString(", ")}")

        val version = prompt("Select version", versions.first())
        val masterIp = prompt("Salt Master IP address", "127.0.0.1")
        val port = prompt("Salt Master port", "4506").toIntOrNull() ?: 4506
        val minionId = prompt("Minion ID", generateDefaultMinionId())

        return InstallationConfig(
            version = version,
            masterIp = masterIp,
            port = port,
            minionId = minionId
        )
    }

    private fun parseSilentArgs(args: Array<String>): InstallationConfig {
        var version = "latest"
        var masterIp = "127.0.0.1"
        var port = 4506
        var minionId = generateDefaultMinionId()

        for (i in args.indices) {
            when (args[i]) {
                "--version" -> version = args.getOrNull(i + 1) ?: version
                "--master" -> masterIp = args.getOrNull(i + 1) ?: masterIp
                "--port" -> port = args.getOrNull(i + 1)?.toIntOrNull() ?: port
                "--minion-id" -> minionId = args.getOrNull(i + 1) ?: minionId
            }
        }

        return InstallationConfig(version, masterIp, port, minionId)
    }

    private fun validatePlatformSupport(platform: Platform): Boolean {
        return when (platform.name.lowercase()) {
            "linux", "ubuntu", "centos", "rhel", "debian", "fedora" -> true
            "windows" -> true
            else -> false
        }
    }

    private fun checkExistingInstallation(): Boolean {
        return systemManager.isInstalled(platformDetector.detect())
    }

    private fun confirmOverwrite(): Boolean {
        return prompt("Salt-Minion is already installed. Overwrite? (y/N)", "n")
            .lowercase().startsWith("y")
    }

    private fun confirmContinueWithoutValidation(): Boolean {
        return prompt("Cannot validate master connection. Continue anyway? (y/N)", "n")
            .lowercase().startsWith("y")
    }

    private fun backup() {
        println("Creating backup of existing configuration...")
        val platform = platformDetector.detect()
        systemManager.backup(platform)
    }

    private fun generateDefaultMinionId(): String {
        val platform = platformDetector.detect()
        return "${platform.hostname}-${Clock.System.now().epochSeconds}"
    }

    private fun prompt(message: String, default: String = ""): String {
        print("$message${if (default.isNotEmpty()) " [$default]" else ""}: ")
        val input = readlnOrNull()?.trim() ?: ""
        return if (input.isEmpty()) default else input
    }
}

data class InstallationConfig(
    val version: String,
    val masterIp: String,
    val port: Int,
    val minionId: String
)

class InstallationException(message: String) : Exception(message)

fun showHelp() {
    println(
        """
Salt-Minion Universal Installer

USAGE:
    salt-installer [OPTIONS]

OPTIONS:
    --help, -h              Show this help message
    --version               Show installer version
    --silent                Run silent installation with default/provided options
    --uninstall             Uninstall Salt-Minion
    --check                 Check installation status

SILENT INSTALLATION OPTIONS:
    --master <ip>           Salt Master IP address (default: 127.0.0.1)
    --port <port>           Salt Master port (default: 4506)
    --minion-id <id>        Minion ID (default: auto-generated)
    --version <version>     Salt version to install (default: latest)

EXAMPLES:
    # Interactive installation
    ./salt-installer

    # Silent installation with custom master
    ./salt-installer --silent --master 192.168.1.100 --minion-id web-server-01

    # Check installation status
    ./salt-installer --check

    # Uninstall
    ./salt-installer --uninstall
    """.trimIndent()
    )
}

fun showVersion() {
    println("Salt-Minion Universal Installer v1.0.0")
    println("Built with Kotlin Native")
}