plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Deliberately zero runtime dependencies (stdlib only), like core/detect:
    // formats, resize math, target-size search, EXIF presets, and rename
    // patterns are pure logic, fully testable without an Android SDK.
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
