plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.github.sebastianp265.aitrainer.core.android"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":core:common"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.health.connect.client)
    implementation(libs.anthropic.java)

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(libs.kotlin.test.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
