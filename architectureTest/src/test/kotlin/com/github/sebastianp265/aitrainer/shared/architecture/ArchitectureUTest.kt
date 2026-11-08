package com.github.sebastianp265.aitrainer.shared.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoInterfaceDeclaration
import com.lemonappdev.konsist.api.declaration.KoPropertyDeclaration
import com.lemonappdev.konsist.api.provider.modifier.KoVisibilityModifierProvider
import com.lemonappdev.konsist.api.verify.assertEmpty
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ArchitectureUTest {

    companion object {
        @JvmStatic
        fun pairs(): List<ModulePair> = GradleModule.entries.flatMap { gradleModule ->
            gradleModule.packageModules.map { ModulePair(gradleModule, it) }
        }

        @JvmStatic
        fun integrationPairs(): List<ModulePair> = pairs().filter { it.packageModule.group == Group.INTEGRATION && it.packageModule != PackageModule.INTEGRATION_MODULE }

        @JvmStatic
        fun gradleModules(): List<GradleModule> = GradleModule.entries
    }

    @Test
    fun `every Gradle module with code is declared`() {
        val modulesWithCode = Konsist.scopeFromProject().files.map { it.moduleName.replace('\\', '/') }.toSet() - "architectureTest"

        assertEquals(GradleModule.entries.map { it.directoryName }.toSet(), modulesWithCode)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("gradleModules")
    fun `every file belongs to a declared package module`(gradleModule: GradleModule) {
        gradleModule.files.assertTrue { it.packageModule in gradleModule.packageModules }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pairs")
    fun `package module keeps its code in its root and impl package only`(pair: ModulePair) {
        val allowedPackages = with(pair.packageModule) { setOf(rootPackage, "$rootPackage.impl") }

        val otherPackages = pair.files.map { it.packageName }.distinct().filterNot { it in allowedPackages }

        assertEquals(emptyList(), otherPackages, "Packages outside root and impl in $pair")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("gradleModules")
    fun `test classes are named UTest, ITest or E2ETest`(gradleModule: GradleModule) {
        Konsist.scopeFromTest(moduleName = gradleModule.directoryName)
            .classes()
            .filter { cls -> cls.functions().any { it.hasAnnotationWithName("Test") } }
            .assertTrue { cls -> listOf("UTest", "ITest", "E2ETest").any { cls.name.endsWith(it) } }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pairs")
    fun `package module uses other package modules only through their root packages`(pair: ModulePair) {
        val otherModules = PackageModule.entries - pair.packageModule

        val importsOfHiddenPackages = pair.imports.filter { import -> otherModules.any { it.hides(import) } }

        assertEquals(emptyList(), importsOfHiddenPackages, "Imports of hidden packages in $pair")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pairs")
    fun `package module depends only on allowed package modules`(pair: ModulePair) {
        val disallowed = pair.importedPackageModules.filterNot { pair.packageModule.mayDependOn(it) }

        assertEquals(emptyList(), disallowed, "Package modules that ${pair.packageModule} must not depend on in $pair")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("gradleModules")
    fun `package modules do not depend on each other in cycles`(gradleModule: GradleModule) {
        val dependencies = PackageModule.entries.associateWith { ModulePair(gradleModule, it).importedPackageModules }

        val modulesInCycles = PackageModule.entries.filter { it in reachableFrom(it, dependencies) }

        assertEquals(emptyList(), modulesInCycles, "Package modules in a dependency cycle in $gradleModule")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pairs")
    fun `package module is visible outside the Gradle module only if it is part of integration`(pair: ModulePair) {
        if (pair.packageModule.group == Group.INTEGRATION) return

        pair.files
            .flatMap { it.topLevelDeclarations() }
            .filterIsInstance<KoVisibilityModifierProvider>()
            .assertTrue { it.hasInternalModifier || it.hasPrivateModifier }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("integrationPairs")
    fun `integration contains only contracts, no logic`(pair: ModulePair) {
        pair.files
            .flatMap { it.topLevelDeclarations() }
            .assertTrue { declaration ->
                when (declaration) {
                    is KoInterfaceDeclaration -> true
                    is KoClassDeclaration -> declaration.hasEnumModifier || (declaration.hasDataModifier && declaration.functions().isEmpty())
                    is KoPropertyDeclaration -> declaration.hasConstModifier
                    else -> false
                }
            }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pairs")
    fun `everything at the root of the package module is used from outside it`(pair: ModulePair) {
        val filesThatMayUseTheRoot = Konsist
            .scopeFromModules((pair.gradleModule.consumers + pair.gradleModule).map { it.directoryName })
            .files
            .filterNot { it.moduleName == pair.gradleModule.directoryName && it.packageModule == pair.packageModule }

        pair.files
            .filter { it.isInPackageModuleRoot }
            .flatMap { rootFile ->
                rootFile.nonPrivateTopLevelDeclarations().filterNot { declaration ->
                    filesThatMayUseTheRoot.any { it.importsFromRoot(rootFile.packageName, declaration.name) }
                }
            }
            .assertEmpty()
    }

    private fun PackageModule.mayDependOn(other: PackageModule): Boolean {
        if (this == PackageModule.FEATURE_APP || this == PackageModule.INTEGRATION_MODULE) return true
        if (other == PackageModule.FEATURE_APP || other == PackageModule.INTEGRATION_MODULE) return false
        val allowedGroups = when (group) {
            Group.INTEGRATION -> setOf(Group.INTEGRATION)
            Group.SHARED -> setOf(Group.SHARED, Group.INTEGRATION)
            Group.FEATURE -> setOf(Group.FEATURE, Group.SHARED, Group.INTEGRATION)
        }
        return other.group in allowedGroups
    }

    private fun reachableFrom(start: PackageModule, graph: Map<PackageModule, Set<PackageModule>>): Set<PackageModule> {
        val visited = mutableSetOf<PackageModule>()
        val queue = ArrayDeque(graph.getValue(start))
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            if (visited.add(next)) queue.addAll(graph.getValue(next))
        }
        return visited
    }
}
