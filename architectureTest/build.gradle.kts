plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.konsist)
}

tasks.test {
    useJUnitPlatform()
    inputs.files(fileTree(rootDir) { include("*/src/**/*.kt", "*/*/src/**/*.kt") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("projectSources")
}
