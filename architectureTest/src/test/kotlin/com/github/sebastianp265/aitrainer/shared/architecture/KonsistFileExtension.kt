package com.github.sebastianp265.aitrainer.shared.architecture

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.declaration.KoSourceDeclaration
import com.lemonappdev.konsist.api.provider.modifier.KoVisibilityModifierProvider

val KoFileDeclaration.packageName: String
    get() = packagee!!.name

val KoFileDeclaration.packageModule: PackageModule?
    get() = PackageModule.containing(packageName)

val KoFileDeclaration.isInPackageModuleRoot: Boolean
    get() = packageName == packageModule!!.rootPackage

fun KoFileDeclaration.topLevelDeclarations(): List<KoSourceDeclaration> =
    declarations(includeNested = false, includeLocal = false).filterIsInstance<KoSourceDeclaration>()

fun KoFileDeclaration.nonPrivateTopLevelDeclarations(): List<KoSourceDeclaration> =
    declarations(includeNested = false, includeLocal = false)
        .filterIsInstance<KoVisibilityModifierProvider>()
        .filterNot { it.hasPrivateModifier }
        .filterIsInstance<KoSourceDeclaration>()

fun KoFileDeclaration.importsFromRoot(rootPackage: String, name: String): Boolean =
    hasImport { it.name == "$rootPackage.$name" || it.name == "$rootPackage.*" }
