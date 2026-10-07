// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    val agpVersion = "9.4.1"
    id("com.android.application") version agpVersion apply false
    id("com.android.library") version agpVersion apply false
    val kotlinVersion = "2.4.20"
    kotlin("android") version kotlinVersion apply false
    kotlin("plugin.parcelize") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false
    id("com.google.devtools.ksp") version "2.3.12" apply false
}

tasks.withType(JavaCompile::class) {
    options.compilerArgs.add("-Xlint:all")
}
