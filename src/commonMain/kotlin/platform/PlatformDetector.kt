package platform

import core.Platform

expect class PlatformDetector() {
    fun detect(): Platform
}