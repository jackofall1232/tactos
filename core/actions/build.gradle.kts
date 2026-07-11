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
    // Both stack-approved in CLAUDE.md section 2: zxing-core (offline QR),
    // kotlinx-serialization (JSON tooling). Runtime library only — no
    // serialization compiler plugin, we only use JsonElement.
    implementation(libs.zxing.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
