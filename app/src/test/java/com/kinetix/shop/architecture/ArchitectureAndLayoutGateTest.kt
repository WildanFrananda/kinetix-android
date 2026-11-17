package com.kinetix.shop.architecture

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureAndLayoutGateTest {
    private val projectRoot: File = resolveProjectRoot()
    private val mainJavaRoot = File(
        projectRoot,
        "app/src/main/java/com/kinetix/shop",
    )
    private val testJavaRoot = File(
        projectRoot,
        "app/src/test/java/com/kinetix/shop",
    )

    @Test
    fun enforcesZeroCommentsInAllKotlinAndGradleFiles() {
        val violations = mutableListOf<String>()
        val commentPattern = Regex("""(^\s*//|/\*)""")
        val files = allKotlinFiles(mainJavaRoot) +
            allKotlinFiles(testJavaRoot) +
            listOf(
                File(
                    projectRoot,
                    "build.gradle.kts",
                ),
                File(
                    projectRoot,
                    "settings.gradle.kts",
                ),
                File(
                    projectRoot,
                    "app/build.gradle.kts",
                ),
            )

        files.filter { it.exists() && it.name != "ArchitectureAndLayoutGateTest.kt" }.forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                if (commentPattern.containsMatchIn(line)) {
                    violations.add("${file.relativeTo(projectRoot)}:${index + 1} contains a comment")
                }
            }
        }

        assertTrue(
            "Found comment violations:\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    @Test
    fun enforcesSubfolderGroupingUnderLayerPackagesOptionA() {
        val layeredPackages = listOf(
            "core",
            "models",
            "dto",
            "services",
            "repositories",
            "state",
            "viewmodels",
        )
        val violations = mutableListOf<String>()

        layeredPackages.forEach { layer ->
            val dir = File(
                mainJavaRoot,
                layer,
            )
            if (dir.exists()) {
                dir.listFiles()?.filter { it.isFile && it.extension == "kt" }?.forEach { strayFile ->
                    violations.add(
                        "${strayFile.relativeTo(projectRoot)} must be placed in a feature subfolder under $layer/"
                    )
                }
            }
        }

        assertTrue(
            "Found flat-layer violations:\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    @Test
    fun enforcesOneTopLevelDeclarationPerFileMatchingFileName() {
        val declarationRegex = Regex(
            """^(?:(?:public|internal|private|abstract|sealed|data|value|enum|fun)\s+)*(?:class|interface|object)\s+([A-Za-z0-9_]+)|^fun\s+([A-Za-z0-9_]+)\s*\("""
        )
        val violations = mutableListOf<String>()

        (allKotlinFiles(mainJavaRoot) + allKotlinFiles(testJavaRoot)).forEach { file ->
            val expectedName = file.nameWithoutExtension
            val declaredNames = mutableListOf<String>()

            file.readLines().forEach { line ->
                val match = declarationRegex.find(line)
                if (match != null) {
                    val name = match.groupValues[1].ifEmpty { match.groupValues[2] }
                    if (name.isNotEmpty()) {
                        declaredNames.add(name)
                    }
                }
            }

            if (declaredNames.size != 1) {
                violations.add(
                    "${file.relativeTo(projectRoot)} declared ${declaredNames.size} top-level items: $declaredNames"
                )
            } else if (declaredNames.first() != expectedName) {
                violations.add(
                    "${file.relativeTo(projectRoot)} declared '${declaredNames.first()}' instead of '$expectedName'"
                )
            }
        }

        assertTrue(
            "Found one-type-per-file violations:\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    @Test
    fun enforcesIdiomaticTopLevelComposableScreensAndCamelCaseDtos() {
        val violations = mutableListOf<String>()
        val screensDir = File(
            mainJavaRoot,
            "ui/screens",
        )
        allKotlinFiles(screensDir).forEach { file ->
            val text = file.readText()
            if (text.contains(Regex("""object\s+[A-Za-z0-9_]*Screen"""))) {
                violations.add(
                    "${file.relativeTo(projectRoot)} uses 'object XScreen' instead of top-level '@Composable fun'"
                )
            }
        }

        val dtoDir = File(
            mainJavaRoot,
            "dto",
        )
        val snakeCasePropertyRegex = Regex("""val\s+[a-z0-9]+_[a-z0-9_]+\s*:""")
        allKotlinFiles(dtoDir).forEach { file ->
            file.readLines().forEachIndexed { idx, line ->
                if (snakeCasePropertyRegex.containsMatchIn(line)) {
                    violations.add(
                        "${file.relativeTo(projectRoot)}:${idx + 1} uses snake_case DTO property instead of camelCase + @SerialName"
                    )
                }
            }
        }

        assertTrue(
            "Found screen or DTO conventions violations:\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    @Test
    fun forbidsDuplicatedNetworkTryCatchAndEmptyUrlFallbacks() {
        val violations = mutableListOf<String>()
        val reposDir = File(
            mainJavaRoot,
            "repositories",
        )
        allKotlinFiles(reposDir).forEach { file ->
            val text = file.readText()
            if (text.contains("SocketTimeoutException") || text.contains("catch (exception: IOException)")) {
                violations.add(
                    "${file.relativeTo(projectRoot)} contains raw IOException/SocketTimeoutException catch instead of delegating to SafeApiCaller"
                )
            }
        }

        val diDir = File(
            mainJavaRoot,
            "di",
        )
        allKotlinFiles(diDir).forEach { file ->
            val text = file.readText()
            if (text.contains("""else -> """"")) {
                violations.add(
                    "${file.relativeTo(projectRoot)} contains empty-string fallback 'else -> \"\"'"
                )
            }
        }

        assertTrue(
            "Found network/DI guardrail violations:\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    private fun allKotlinFiles(dir: File): List<File> {
        if (!dir.exists()) {
            return emptyList()
        }
        return dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private fun resolveProjectRoot(): File {
        var current: File? = File(System.getProperty("user.dir") ?: ".")
        while (current != null && !File(current, "settings.gradle.kts").exists()) {
            current = current.parentFile
        }
        return current ?: File(".")
    }
}
