package core

expect class FileUtils() {
    fun exists(path: String): Boolean
    fun createDirectory(path: String): Boolean
    fun deleteDirectory(path: String): Boolean
    fun copyFile(source: String, destination: String): Boolean
    fun writeTextFile(path: String, content: String): Boolean
    fun readTextFile(path: String): String?
    fun makeExecutable(path: String): Boolean
    fun getFileSize(path: String): Long
    fun calculateChecksum(path: String): String
}