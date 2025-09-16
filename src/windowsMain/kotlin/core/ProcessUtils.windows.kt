package core

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.posix.system

@OptIn(ExperimentalForeignApi::class)
actual class ProcessUtils {
    actual suspend fun execute(
        command: String,
        args: List<String>,
        workingDir: String?,
        captureOutput: Boolean
    ): ProcessResult = withContext(Dispatchers.Default) {

        // Corrigir a construção do comando para Windows
        val fullCommand = buildString {
            // Adicionar aspas apenas se o comando contém espaços
            if (command.contains(" ")) {
                append("\"$command\"")
            } else {
                append(command)
            }

            if (args.isNotEmpty()) {
                append(" ")
                // Adicionar aspas apenas em argumentos que contêm espaços
                append(args.joinToString(" ") { arg ->
                    if (arg.contains(" ") || arg.contains("\"")) {
                        "\"${arg.replace("\"", "\\\"")}\""
                    } else {
                        arg
                    }
                })
            }
        }

        // Mudança de diretório corrigida para Windows
        val commandWithDir = if (workingDir != null) {
            "cd /d \"$workingDir\" && $fullCommand"
        } else {
            fullCommand
        }

        memScoped {
            val exitCode = system(commandWithDir)
            ProcessResult(exitCode, "", "")
        }
    }

    actual suspend fun executeAsRoot(
        command: String,
        args: List<String>,
        workingDir: String?
    ): ProcessResult {
        // Corrigir PowerShell com privilégios elevados
        val escapedCommand = command.replace("'", "''")
        val escapedArgs = args.map { it.replace("'", "''") }

        val psCommand = buildString {
            append("Start-Process -FilePath '$escapedCommand'")

            if (escapedArgs.isNotEmpty()) {
                append(" -ArgumentList ")
                append(escapedArgs.joinToString(",") { "'$it'" })
            }

            append(" -Verb RunAs -Wait")

            if (workingDir != null) {
                val escapedWorkingDir = workingDir.replace("'", "''")
                append(" -WorkingDirectory '$escapedWorkingDir'")
            }
        }

        return execute("powershell", listOf("-Command", psCommand), null)
    }
}