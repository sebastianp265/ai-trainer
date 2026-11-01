package com.github.sebastianp265.aitrainer.shared.architecture

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration

data class ModulePair(val gradleModule: GradleModule, val packageModule: PackageModule) {

    val files: List<KoFileDeclaration>
        get() = gradleModule.files.filter { it.packageModule == packageModule }

    val imports: List<String>
        get() = files.flatMap { file -> file.imports.map { it.name } }

    val importedPackageModules: Set<PackageModule>
        get() = imports.mapNotNull { PackageModule.containing(it) }.toSet() - packageModule

    override fun toString() = "$gradleModule / ${packageModule.path}"
}
