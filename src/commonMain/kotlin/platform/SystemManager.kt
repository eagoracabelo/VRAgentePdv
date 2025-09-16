package platform

import InstallationConfig
import core.Platform

expect class SystemManager() {
    fun isInstalled(platform: Platform): Boolean
    fun getInstalledVersion(platform: Platform): String?
    suspend fun install(packagePath: String, config: InstallationConfig)
    fun createService(config: InstallationConfig)
    fun startService(serviceName: String)
    fun stopService(serviceName: String)
    fun removeService(serviceName: String)
    fun getServiceStatus(serviceName: String): String
    fun removeInstallation(platform: Platform)
    fun backup(platform: Platform)
}