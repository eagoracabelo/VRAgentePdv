plugins {
    kotlin("multiplatform") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
}

group = "br.com.vrsoftware"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {

    // Linux target
    linuxX64("linux") {
        binaries {
            executable {
                entryPoint = "main"
                baseName = "salt-installer-linux"
            }
        }
    }

    // Windows target
    mingwX64("windows") {
        binaries {
            executable {
                entryPoint = "main"
                baseName = "salt-installer-windows"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val linuxMain by getting {
            dependencies {
                // Linux specific dependencies
            }
        }

        val windowsMain by getting {
            dependencies {
                // Windows specific dependencies
            }
        }
    }
}

// Task to build both platforms
tasks.register("buildAll") {
    dependsOn("linuxBinaries", "windowsBinaries")
    description = "Build executables for all platforms"
}

// Task to package releases
tasks.register<Zip>("packageRelease") {
    dependsOn("buildAll")
    archiveFileName.set("salt-installer-${version}.zip")
    destinationDirectory.set(file("$buildDir/releases"))

    from("build/bin/linux/releaseExecutable") {
        into("linux")
    }
    from("build/bin/windows/releaseExecutable") {
        into("windows")
    }
    from("README.md")
    from("LICENSE")
}