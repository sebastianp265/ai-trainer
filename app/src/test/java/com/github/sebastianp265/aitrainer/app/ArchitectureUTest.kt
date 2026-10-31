package com.github.sebastianp265.aitrainer.app

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals

class ArchitectureUTest {

    private val allowedFeatureDependencies = mapOf(
        "exercise" to setOf(),
        "workout" to setOf("exercise"),
        "health" to setOf(),
        "progress" to setOf("health"),
        "settings" to setOf(),
        "plan" to setOf(),
    )

    private val publicFeatureLayers = setOf("domain", "ui")

    private val scope = Konsist.scopeFromProduction()
    private val files = scope.files.filter { it.packageName.startsWith(ROOT) }

    @Test
    fun `every feature package is listed in the dependency map`() {
        val featuresInCode = files.mapNotNull { it.packageName.featureName() }.toSet()
        assertEquals(allowedFeatureDependencies.keys, featuresInCode)
    }

    @Test
    fun `feature dependency map has no cycles`() {
        val cycles = allowedFeatureDependencies.keys.mapNotNull { feature -> cycleFrom(feature, listOf(feature)) }
        assertEquals(emptyList(), cycles.map { it.joinToString(" -> ") })
    }

    private fun cycleFrom(feature: String, path: List<String>): List<String>? {
        for (dependency in allowedFeatureDependencies[feature].orEmpty()) {
            if (dependency == path.first()) return path + dependency
            if (dependency !in path) cycleFrom(dependency, path + dependency)?.let { return it }
        }
        return null
    }

    @Test
    fun `core depends on no feature and not on app`() {
        files.filter { it.packageName.startsWith("$ROOT.core") }.assertFalse { file ->
            file.importNames.any { it.startsWith("$ROOT.feature.") || it.startsWith("$ROOT.app.") }
        }
    }

    @Test
    fun `features depend only on allowed features and only on their domain or ui`() {
        val violations = files.flatMap { file ->
            val feature = file.packageName.featureName() ?: return@flatMap emptyList()
            file.importNames.filter { import ->
                val other = import.featureName()
                other != null && other != feature &&
                    (other !in allowedFeatureDependencies.getValue(feature) || !file.mayImportLayerOf(import))
            }.map { "${file.packageName}.${file.name} imports $it" }
        }
        assertEquals(emptyList(), violations)
    }

    private fun KoFileDeclaration.mayImportLayerOf(import: String): Boolean =
        import.featureLayer() in publicFeatureLayers || isRoomForeignKeyToAnotherFeature(import)

    private fun KoFileDeclaration.isRoomForeignKeyToAnotherFeature(import: String): Boolean =
        packageName.featureLayer() == "data" && import.featureLayer() == "data" && import.endsWith("Entity")

    @Test
    fun `features never import the app package`() {
        files.filter { it.packageName.featureName() != null }.assertFalse { file ->
            file.importNames.any { it.startsWith("$ROOT.app.") }
        }
    }

    @Test
    fun `domain and data do not depend on ui`() {
        files.filter { it.packageName.featureLayer() in setOf("domain", "data") }.assertFalse { file ->
            file.importNames.any { it.featureLayer() == "ui" }
        }
    }

    @Test
    fun `domain is pure Kotlin`() {
        files.filter { it.packageName.featureLayer() == "domain" }.assertFalse { file ->
            file.importNames.any { it.startsWith("android.") || it.startsWith("androidx.") || it.startsWith("java.time") }
        }
    }

    @Test
    fun `classes live in the layer their name says`() {
        val expectedLayerBySuffix = mapOf(
            "UseCase" to "domain",
            "Rules" to "domain",
            "Entity" to "data",
            "Dao" to "data",
            "Dto" to "data",
            "ViewModel" to "ui",
            "UiState" to "ui",
        )
        val declarations = scope.classes().map { it.name to it.packagee?.name.orEmpty() } +
            scope.interfaces().map { it.name to it.packagee?.name.orEmpty() }
        val violations = declarations.filter { (name, pkg) ->
            val expected = expectedLayerBySuffix.entries.firstOrNull { name.endsWith(it.key) }?.value
            expected != null && pkg.featureLayer() != expected
        }
        assertEquals(emptyList(), violations)
    }

    @Test
    fun `repository interfaces are in domain and their implementations in data`() {
        scope.interfaces().filter { it.name.endsWith("Repository") }.assertTrue {
            it.packagee?.name.orEmpty().featureLayer() == "domain"
        }
        scope.classes().filter { it.name.endsWith("Repository") }.assertTrue {
            it.packagee?.name.orEmpty().featureLayer() == "data"
        }
    }

    private val KoFileDeclaration.packageName: String get() = packagee?.name.orEmpty()

    private val KoFileDeclaration.importNames: List<String> get() = imports.map { it.name }

    private fun String.featureName(): String? =
        removePrefix("$ROOT.feature.").takeIf { it != this }?.substringBefore(".")

    private fun String.featureLayer(): String? =
        removePrefix("$ROOT.feature.").takeIf { it != this }?.split(".")?.getOrNull(1)

    private companion object {
        const val ROOT = "com.github.sebastianp265.aitrainer"
    }
}
