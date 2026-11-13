plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.serialization)
}

val roomSchemaDirectory: String = layout.projectDirectory.dir("schemas").asFile.absolutePath

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
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(project.dependencies.platform(libs.junit.bom))
            implementation(libs.androidx.room.testing)
            implementation(libs.androidx.sqlite.bundled)
            runtimeOnly(libs.junit.platform.launcher)
        }
    }
}

room {
    schemaDirectory(roomSchemaDirectory)
}

dependencies {
    add("kspJvm", libs.androidx.room.compiler)
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    systemProperty("roomSchemaDirectory", roomSchemaDirectory)
}
