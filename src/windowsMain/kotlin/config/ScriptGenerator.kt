package config

import core.FileUtils
import core.Logger

actual class ScriptGenerator {

    private val fileUtils = FileUtils()
    private val logger = Logger

    actual suspend fun generateBackupScript(scriptsDir: String): String {
        // Script de backup do VRPdv.jar
        val backupScript = """
@echo off
setlocal enabledelayedexpansion

set PDV_PATH=C:\pdv\exec
set BACKUP_PATH=C:\pdv\backup
set TIMESTAMP=%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=!TIMESTAMP: =0!

if not exist "%BACKUP_PATH%" mkdir "%BACKUP_PATH%"

if exist "%PDV_PATH%\VRPdv.jar" (
    copy "%PDV_PATH%\VRPdv.jar" "%BACKUP_PATH%\VRPdv_backup_%TIMESTAMP%.jar"
    echo Backup created: VRPdv_backup_%TIMESTAMP%.jar
) else (
    echo VRPdv.jar not found in %PDV_PATH%
)

rem Cleanup old backups (keep last 7)
forfiles /p "%BACKUP_PATH%" /m "VRPdv_backup_*.jar" /d -7 /c "cmd /c del @path" 2>nul

endlocal
    """.trimIndent()

        fileUtils.writeTextFile("$scriptsDir/backup_vrpdv.bat", backupScript)
        return backupScript
    }

    actual suspend fun generateIntegrityScript(scriptsDir: String): String {
        val integrityScript = """
@echo off
setlocal

set PDV_JAR=C:\pdv\exec\VRPdv.jar
set VR_PROPS=C:\vr\vr.properties

echo Checking PDV/VR integrity...

if exist "%PDV_JAR%" (
    echo [OK] VRPdv.jar found
    for %%A in ("%PDV_JAR%") do set JAR_SIZE=%%~zA
    echo [INFO] JAR size: !JAR_SIZE! bytes
) else (
    echo [ERROR] VRPdv.jar not found
    exit /b 1
)
scriptsDir
if exist "%VR_PROPS%" (
    echo [OK] vr.properties found
    for %%A in ("%VR_PROPS%") do set PROPS_SIZE=%%~zA
    echo [INFO] Properties size: !PROPS_SIZE! bytes
) else (
    echo [ERROR] vr.properties not found
    exit /b 1
)

echo [OK] All files present
endlocal
    """.trimIndent()

        fileUtils.writeTextFile("$scriptsDir/check_integrity.bat", integrityScript)
        return integrityScript
    }

    actual suspend fun setExecutablePermissions(scriptPath: String) {}
}