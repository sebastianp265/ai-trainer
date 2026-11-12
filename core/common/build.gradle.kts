plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            api(libs.androidx.room.runtime)
            api(libs.androidx.datastore.preferences.core)
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
            api(libs.kotlinx.serialization.json)
        }
        jvmTest.dependencies {
            implementation(project.dependencies.platform(libs.junit.bom))
            implementation(libs.kotlin.test.junit5)
            implementation(libs.androidx.room.testing)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.kotlinx.coroutines.test)
            runtimeOnly(libs.junit.platform.launcher)
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspJvm", libs.androidx.room.compiler)
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
