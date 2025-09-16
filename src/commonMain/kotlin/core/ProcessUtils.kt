package core

expect class ProcessUtils() {
    suspend fun execute(
        command: String,
        args: List<String> = emptyList(),
        workingDir: String? = null,
        captureOutput: Boolean = true
    ): ProcessResult

    suspend fun executeAsRoot(
        command: String,
        args: List<String> = emptyList(),
        workingDir: String? = null
    ): ProcessResult
}

data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val isSuccess: Boolean get() = exitCode == 0
}