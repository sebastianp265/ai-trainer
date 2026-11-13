package com.github.sebastianp265.aitrainer.shared.architecture

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ConventionsUTest {

    companion object {
        const val MAX_KOTLIN_FILE_LINES = 700
        val COMMENT_STARTS = listOf("//", "/*", "<!--")
        val SKIPPED_DIRECTORIES = setOf("build", ".gradle", ".git", ".idea", ".kotlin")
        val projectRoot = File(checkNotNull(System.getProperty("projectRoot")))
    }

    @Test
    fun `Kotlin, Gradle and XML files have no comments`() {
        val withComments = sourceFiles("kt", "kts", "xml").filter { file ->
            file.readLines().any { line -> COMMENT_STARTS.any { line.trimStart().startsWith(it) } }
        }

        assertEquals(emptyList(), withComments.relativePaths())
    }

    @Test
    fun `Kotlin files stay within a few hundred lines`() {
        val tooLong = sourceFiles("kt").filter { it.readLines().size > MAX_KOTLIN_FILE_LINES }

        assertEquals(emptyList(), tooLong.relativePaths())
    }

    private fun sourceFiles(vararg extensions: String): List<File> {
        val buildFiles = checkNotNull(projectRoot.listFiles()).filter { it.isFile }
        val moduleFiles = GradleModule.entries.flatMap { module ->
            File(projectRoot, module.directoryName).walk().onEnter { it.name !in SKIPPED_DIRECTORIES }
        }
        return (buildFiles + moduleFiles).filter { it.isFile && it.extension in extensions }
    }

    private fun List<File>.relativePaths(): List<String> = map { it.relativeTo(projectRoot).invariantSeparatorsPath }
}
