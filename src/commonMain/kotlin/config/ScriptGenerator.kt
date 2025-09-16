package config

expect class ScriptGenerator() {
    suspend fun generateBackupScript(scriptsDir: String):String
    suspend fun generateIntegrityScript(scriptsDir: String):String
    suspend fun setExecutablePermissions(scriptPath: String)
}