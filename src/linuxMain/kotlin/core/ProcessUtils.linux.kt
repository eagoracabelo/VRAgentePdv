package core

import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
actual class ProcessUtils {
    actual suspend fun execute(
        command: String,
        args: List<String>,
        workingDir: String?,
        captureOutput: Boolean
    ): ProcessResult = withContext(Dispatchers.Default) {

        val fullCommand = if (args.isNotEmpty()) {
            "$command ${args.joinToString(" ")}"
        } else {
            command
        }

        return@withContext memScoped {
            val file = popen(fullCommand, "r")
            if (file == null) {
                return@withContext ProcessResult(-1, "", "Failed to execute command")
            }

            val buffer = ByteArray(4096)
            val output = StringBuilder()

            while (true) {
                val bytesRead = fread(buffer.refTo(0), 1.convert(), buffer.size.convert(), file).toInt()
                if (bytesRead <= 0) break
                output.append(buffer.decodeToString(0, bytesRead))
            }

            val exitCode = pclose(file)
            ProcessResult(exitCode, output.toString(), "")
        }
    }

    actual suspend fun executeAsRoot(
        command: String,
        args: List<String>,
        workingDir: String?
    ): ProcessResult {
        return execute("sudo", listOf(command) + args, workingDir)
    }
}