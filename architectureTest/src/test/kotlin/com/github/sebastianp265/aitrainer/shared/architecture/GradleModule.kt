package com.github.sebastianp265.aitrainer.shared.architecture

import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.FEATURE_APP
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.FEATURE_EXERCISE
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.FEATURE_HEALTH
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.FEATURE_HISTORY
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.INTEGRATION_EXERCISE
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.INTEGRATION_HEALTH
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.INTEGRATION_PLAN
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.INTEGRATION_COACH
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.FEATURE_COACH
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.SHARED_COACH
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.SHARED_NETWORK
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.INTEGRATION_SETTINGS
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.INTEGRATION_VOLUME
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.FEATURE_VOLUME
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.SHARED_VOLUME
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.INTEGRATION_SHARED
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.INTEGRATION_WORKOUT
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.INTEGRATION_MODULE
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.FEATURE_PLAN
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.FEATURE_PROGRESS
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.FEATURE_SETTINGS
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.SHARED_BACKUP
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.SHARED_CHARTS
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.SHARED_DESIGN
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.SHARED_DATABASE
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.SHARED_EXERCISE
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.SHARED_HEALTH
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.SHARED_LOCALIZATION
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.SHARED_PLAN
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.SHARED_PLATFORM
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.SHARED_SECURITY
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.SHARED_STATE
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.SHARED_WORKOUT
import com.github.sebastianp265.aitrainer.shared.architecture.PackageModule.FEATURE_WORKOUT
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration

enum class GradleModule(
    val directoryName: String,
    val packageModules: Set<PackageModule>,
    private val dependencies: () -> Set<GradleModule> = { emptySet() },
) {
    CORE_COMMON(
        directoryName = "core/common",
        packageModules = setOf(
            INTEGRATION_MODULE,
            INTEGRATION_EXERCISE, INTEGRATION_WORKOUT, INTEGRATION_PLAN, INTEGRATION_VOLUME, INTEGRATION_COACH, INTEGRATION_HEALTH, INTEGRATION_SETTINGS, INTEGRATION_SHARED,
            SHARED_EXERCISE, SHARED_WORKOUT, SHARED_PLAN, SHARED_VOLUME, SHARED_COACH, SHARED_HEALTH, SHARED_DATABASE,
            FEATURE_EXERCISE, FEATURE_WORKOUT, FEATURE_PLAN, FEATURE_VOLUME, FEATURE_COACH, FEATURE_HEALTH, FEATURE_SETTINGS,
        ),
    ),
    CORE_ANDROID(
        directoryName = "core/android",
        packageModules = setOf(INTEGRATION_MODULE, SHARED_HEALTH, SHARED_SECURITY, SHARED_PLATFORM, SHARED_NETWORK, SHARED_BACKUP),
        dependencies = { setOf(CORE_COMMON) },
    ),
    ANDROID_APP(
        directoryName = "androidApp",
        packageModules = setOf(
            FEATURE_APP,
            SHARED_LOCALIZATION, SHARED_STATE, SHARED_HEALTH, SHARED_WORKOUT, SHARED_CHARTS, SHARED_DESIGN, SHARED_VOLUME, SHARED_COACH,
            FEATURE_EXERCISE, FEATURE_WORKOUT, FEATURE_HEALTH, FEATURE_SETTINGS, FEATURE_PROGRESS, FEATURE_PLAN, FEATURE_HISTORY, FEATURE_VOLUME, FEATURE_COACH,
        ),
        dependencies = { setOf(CORE_COMMON, CORE_ANDROID) },
    );

    val consumers: Set<GradleModule>
        get() = entries.filter { this in it.dependencies() }.toSet()

    val files: List<KoFileDeclaration>
        get() = Konsist.scopeFromProduction(moduleName = directoryName).files

    override fun toString() = directoryName
}
