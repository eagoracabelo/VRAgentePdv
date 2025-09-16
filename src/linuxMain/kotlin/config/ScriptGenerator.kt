package config

import core.FileUtils
import core.Logger
import core.ProcessUtils

actual class ScriptGenerator {

    private val fileUtils = FileUtils()
    private val logger = Logger

    actual suspend fun generateBackupScript(scriptsDir: String): String {

        // Script de backup do VRPdv.jar
        val backupScript = """
#!/bin/bash

PDV_PATH="/pdv/exec"
BACKUP_PATH="/pdv/backup"
TIMESTAMP=${'$'}(date +"%Y%m%d_%H%M%S")

mkdir -p "${'$'}BACKUP_PATH"

if [ -f "${'$'}PDV_PATH/VRPdv.jar" ]; then
    cp "${'$'}PDV_PATH/VRPdv.jar" "${'$'}BACKUP_PATH/VRPdv_backup_${'$'}TIMESTAMP.jar"
    echo "Backup created: VRPdv_backup_${'$'}TIMESTAMP.jar"

    # Set permissions
    chmod 644 "${'$'}BACKUP_PATH/VRPdv_backup_${'$'}TIMESTAMP.jar"
else
    echo "VRPdv.jar not found in ${'$'}PDV_PATH"
fi

# Cleanup old backups (keep last 7)
find "${'$'}BACKUP_PATH" -name "VRPdv_backup_*.jar" -mtime +7 -delete 2>/dev/null

# Create checksum
if [ -f "${'$'}PDV_PATH/VRPdv.jar" ]; then
    md5sum "${'$'}PDV_PATH/VRPdv.jar" > "${'$'}PDV_PATH/VRPdv.jar.md5"
fi
    """.trimIndent()

        fileUtils.writeTextFile("$scriptsDir/backup_vrpdv.sh", backupScript)
        setExecutablePermissions(scriptsDir)
        return backupScript
    }

    actual suspend fun generateIntegrityScript(scriptsDir: String): String {
        val integrityScript = """
#!/bin/bash

PDV_JAR="/pdv/exec/VRPdv.jar"
VR_PROPS="/vr/vr.properties"

echo "Checking PDV/VR integrity..."

if [ -f "${'$'}PDV_JAR" ]; then
    echo "[OK] VRPdv.jar found"
    JAR_SIZE=${'$'}(stat -f%z "${'$'}PDV_JAR" 2>/dev/null || stat -c%s "${'$'}PDV_JAR")
    echo "[INFO] JAR size: ${'$'}JAR_SIZE bytes"

    # Verify JAR integrity
    if command -v jar &> /dev/null; then
        if jar tf "${'$'}PDV_JAR" > /dev/null 2>&1; then
            echo "[OK] JAR file is valid"
        else
            echo "[ERROR] JAR file is corrupted"
            exit 1
        fi
    fi
else
    echo "[ERROR] VRPdv.jar not found"
    exit 1
fi

if [ -f "${'$'}VR_PROPS" ]; then
    echo "[OK] vr.properties found"
    PROPS_SIZE=${'$'}(stat -f%z "${'$'}VR_PROPS" 2>/dev/null || stat -c%s "${'$'}VR_PROPS")
    echo "[INFO] Properties size: ${'$'}PROPS_SIZE bytes"

    # Validate properties format
    if grep -q "=" "${'$'}VR_PROPS"; then
        echo "[OK] Properties file format is valid"
    else
        echo "[WARN] Properties file may be empty or invalid"
    fi
else
    echo "[ERROR] vr.properties not found"
    exit 1
fi

echo "[OK] All files present and valid"
    """.trimIndent()

        fileUtils.writeTextFile("$scriptsDir/check_integrity.sh", integrityScript)
        setExecutablePermissions(scriptsDir)

        return integrityScript
    }

    actual suspend fun setExecutablePermissions(scriptPath: String) {
        try {
            val processUtils = ProcessUtils()
            processUtils.execute("chmod", listOf("+x", "$scriptPath/backup_vrpdv.sh"))
            processUtils.execute("chmod", listOf("+x", "$scriptPath/check_integrity.sh"))
        } catch (e: Exception) {
            logger.warn("Failed to set script permissions: ${e.message}")
        }
    }


    companion object {
        private const val PDV_PATH = "pdv_path"
        private const val BACKUP_PATH = "backup_path"
        private const val TIMESTAMP = "timestamp"
        private const val PDV_JAR = "pdv_jar"
        private const val VR_PROPS = "vr_props"
        private const val JAR_SIZE = "jar_size"
        private const val PROPS_SIZE = "props_size"
    }
}