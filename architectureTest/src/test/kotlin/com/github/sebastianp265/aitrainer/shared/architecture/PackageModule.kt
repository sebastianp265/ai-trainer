package com.github.sebastianp265.aitrainer.shared.architecture

enum class Group(val path: String) {
    INTEGRATION("integration"),
    SHARED("shared"),
    FEATURE("feature"),
}

enum class PackageModule(val path: String) {
    FEATURE_APP("feature.app"),
    INTEGRATION_MODULE("integration.module"),
    INTEGRATION_EXERCISE("integration.exercise"),
    INTEGRATION_WORKOUT("integration.workout"),
    INTEGRATION_PLAN("integration.plan"),
    INTEGRATION_VOLUME("integration.volume"),
    INTEGRATION_NUTRITION("integration.nutrition"),
    INTEGRATION_COACH("integration.coach"),
    INTEGRATION_HEALTH("integration.health"),
    INTEGRATION_SETTINGS("integration.settings"),
    INTEGRATION_SHARED("integration.shared"),
    SHARED_EXERCISE("shared.exercise"),
    SHARED_WORKOUT("shared.workout"),
    SHARED_PLAN("shared.plan"),
    SHARED_VOLUME("shared.volume"),
    SHARED_NUTRITION("shared.nutrition"),
    SHARED_COACH("shared.coach"),
    SHARED_HEALTH("shared.health"),
    SHARED_DATABASE("shared.database"),
    SHARED_SECURITY("shared.security"),
    SHARED_PLATFORM("shared.platform"),
    SHARED_NETWORK("shared.network"),
    SHARED_BACKUP("shared.backup"),
    SHARED_LOCALIZATION("shared.localization"),
    SHARED_STATE("shared.state"),
    SHARED_CHARTS("shared.charts"),
    SHARED_DESIGN("shared.design"),
    FEATURE_EXERCISE("feature.exercise"),
    FEATURE_WORKOUT("feature.workout"),
    FEATURE_HEALTH("feature.health"),
    FEATURE_SETTINGS("feature.settings"),
    FEATURE_PROGRESS("feature.progress"),
    FEATURE_PLAN("feature.plan"),
    FEATURE_VOLUME("feature.volume"),
    FEATURE_NUTRITION("feature.nutrition"),
    FEATURE_COACH("feature.coach"),
    FEATURE_HISTORY("feature.history");

    val group: Group
        get() = Group.entries.single { it.path == path.substringBefore('.') }

    val rootPackage: String
        get() = "$BASE_PACKAGE.$path"

    fun hides(importName: String): Boolean {
        if (!importName.startsWith("$rootPackage.")) return false
        val segments = importName.removePrefix("$rootPackage.").split(".")
        return segments.size > 1 && segments.first().first().isLowerCase()
    }

    companion object {
        const val BASE_PACKAGE = "com.github.sebastianp265.aitrainer"

        fun containing(packageName: String): PackageModule? =
            entries.find { packageName == it.rootPackage || packageName.startsWith("${it.rootPackage}.") }
    }
}
