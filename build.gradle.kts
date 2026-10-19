// Top-level build file for Arrow Bloom.
plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}

allprojects {
    layout.buildDirectory.set(file("${System.getProperty("user.home")}/.build-arrowbloom/${project.name}"))
}
