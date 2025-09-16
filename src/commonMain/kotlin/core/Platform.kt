package core

data class Platform(
    val name: String,
    val version: String,
    val architecture: String,
    val hostname: String,
    val isRoot: Boolean = false
)

enum class PlatformType {
    LINUX, WINDOWS, UNSUPPORTED
}