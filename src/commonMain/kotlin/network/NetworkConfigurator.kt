package network

import core.Logger
import core.ProcessUtils

class NetworkConfigurator {
    private val logger = Logger
    private val processUtils = ProcessUtils()

    suspend fun validateMasterConnection(masterIp: String, port: Int): Boolean {
        logger.info("Validating connection to Salt Master at $masterIp:$port")

        return try {
            // Try multiple validation methods
            val tcpCheck = checkTcpConnection(masterIp, port)
            val pingCheck = pingHost(masterIp)

            val result = tcpCheck || pingCheck

            if (result) {
                logger.info("Master connection validation successful")
            } else {
                logger.warn("Master connection validation failed")
            }

            result
        } catch (e: Exception) {
            logger.error("Connection validation error: ${e.message}")
            false
        }
    }

    private suspend fun checkTcpConnection(host: String, port: Int): Boolean {
        return try {
            val result = processUtils.execute("timeout", listOf("5", "bash", "-c", "echo > /dev/tcp/$host/$port"))
            result.isSuccess
        } catch (e: Exception) {
            // Fallback methods for different platforms
            checkTcpConnectionFallback(host, port)
        }
    }

    private suspend fun checkTcpConnectionFallback(host: String, port: Int): Boolean {
        // Try with netcat if available
        return try {
            val result = processUtils.execute("nc", listOf("-z", "-w", "3", host, port.toString()))
            result.isSuccess
        } catch (e: Exception) {
            // Try with telnet as last resort
            try {
                val result = processUtils.execute("timeout", listOf("3", "telnet", host, port.toString()))
                result.stdout.contains("Connected") || result.exitCode == 0
            } catch (e: Exception) {
                false
            }
        }
    }

    private suspend fun pingHost(host: String): Boolean {
        return try {
            val result = processUtils.execute("ping", listOf("-c", "1", "-W", "3", host))
            result.isSuccess
        } catch (e: Exception) {
            logger.debug("Ping failed: ${e.message}")
            false
        }
    }

    fun validateIpAddress(ip: String): Boolean {
        val ipRegex = """^(\d{1,3}\.){3}\d{1,3}$""".toRegex()
        if (!ipRegex.matches(ip)) return false

        return ip.split(".").all { octet ->
            val num = octet.toIntOrNull()
            num != null && num in 0..255
        }
    }

    fun validatePort(port: Int): Boolean {
        return port in 1..65535
    }

    suspend fun getNetworkConfiguration(): NetworkConfig? {
        return try {
            val interfaces = getNetworkInterfaces()
            val defaultGateway = getDefaultGateway()
            val dnsServers = getDnsServers()

            NetworkConfig(
                interfaces = interfaces,
                defaultGateway = defaultGateway,
                dnsServers = dnsServers
            )
        } catch (e: Exception) {
            logger.error("Failed to get network configuration: ${e.message}")
            null
        }
    }

    private suspend fun getNetworkInterfaces(): List<NetworkInterface> {
        val interfaces = mutableListOf<NetworkInterface>()

        try {
            val result = processUtils.execute("ip", listOf("addr", "show"))
            if (result.isSuccess) {
                // Parse ip command output
                parseIpAddrOutput(result.stdout, interfaces)
            }
        } catch (e: Exception) {
            // Fallback to ifconfig
            try {
                val result = processUtils.execute("ifconfig")
                if (result.isSuccess) {
                    parseIfconfigOutput(result.stdout, interfaces)
                }
            } catch (e: Exception) {
                logger.warn("Failed to get network interfaces: ${e.message}")
            }
        }

        return interfaces
    }

    private fun parseIpAddrOutput(output: String, interfaces: MutableList<NetworkInterface>) {
        val lines = output.split("\n")
        var currentInterface: String? = null
        var currentIp: String? = null

        for (line in lines) {
            val trimmed = line.trim()

            // Interface line: "2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500"
            if (trimmed.matches("""^\d+: \w+:.*""".toRegex())) {
                val interfaceName = trimmed.split(":")[1].trim()
                currentInterface = interfaceName
            }

            // IP address line: "inet 192.168.1.100/24 brd 192.168.1.255 scope global eth0"
            if (trimmed.startsWith("inet ") && currentInterface != null) {
                val ipWithCidr = trimmed.split(" ")[1]
                currentIp = ipWithCidr.split("/")[0]

                interfaces.add(NetworkInterface(currentInterface, currentIp))
            }
        }
    }

    private fun parseIfconfigOutput(output: String, interfaces: MutableList<NetworkInterface>) {
        // Simple parsing for ifconfig output
        val interfaceBlocks = output.split("\n\n")

        for (block in interfaceBlocks) {
            val lines = block.split("\n")
            if (lines.isNotEmpty()) {
                val firstLine = lines[0]
                val interfaceName = firstLine.split(":")[0].trim()

                val inetLine = lines.find { it.contains("inet ") && !it.contains("inet6") }
                if (inetLine != null) {
                    val ip = inetLine.split("inet ")[1].split(" ")[0]
                    interfaces.add(NetworkInterface(interfaceName, ip))
                }
            }
        }
    }

    private suspend fun getDefaultGateway(): String? {
        return try {
            val result = processUtils.execute("ip", listOf("route", "show", "default"))
            if (result.isSuccess) {
                val lines = result.stdout.split("\n")
                for (line in lines) {
                    if (line.contains("default via")) {
                        return line.split("via ")[1].split(" ")[0]
                    }
                }
            }
            null
        } catch (e: Exception) {
            logger.debug("Failed to get default gateway: ${e.message}")
            null
        }
    }

    private suspend fun getDnsServers(): List<String> {
        return try {
            val result = processUtils.execute("cat", listOf("/etc/resolv.conf"))
            if (result.isSuccess) {
                result.stdout.split("\n")
                    .filter { it.startsWith("nameserver ") }
                    .map { it.split(" ")[1] }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            logger.debug("Failed to get DNS servers: ${e.message}")
            emptyList()
        }
    }
}

data class NetworkConfig(
    val interfaces: List<NetworkInterface>,
    val defaultGateway: String?,
    val dnsServers: List<String>
)

data class NetworkInterface(
    val name: String,
    val ip: String
)