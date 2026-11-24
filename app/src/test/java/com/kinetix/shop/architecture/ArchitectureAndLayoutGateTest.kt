package com.kinetix.shop.architecture

import java.io.File
import org.junit.Assert.assertFalse
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

    private val allowedLayerImports: Map<String, Set<String>> = mapOf(
        "ui" to setOf("ui", "viewmodels", "state", "routes", "models", "core"),
        "viewmodels" to setOf("viewmodels", "repositories", "services", "state", "models", "core"),
        "state" to setOf("state", "models", "core"),
        "routes" to setOf("routes", "models"),
        "repositories" to setOf("repositories", "services", "dto", "models", "core"),
        "services" to setOf("services", "dto", "models", "core"),
        "dto" to setOf("dto"),
        "models" to setOf("models"),
        "core" to setOf("core"),
    )

    @Test
    fun commentScannerHandlesStringsCharsUrlsAndTrailingComments() {
        assertFalse(containsCommentOutsideLiterals("""val expected = "a // b""""))
        assertFalse(containsCommentOutsideLiterals("""val url = "https://api.kinetix.invalid""""))
        assertFalse(containsCommentOutsideLiterals("val json = \"\"\"{\"url\":\"https://api.kinetix.invalid\"}\"\"\""))
        assertFalse(containsCommentOutsideLiterals("""val slash = '/'"""))
        assertTrue(containsCommentOutsideLiterals("""val value = "x" // note"""))
        assertTrue(containsCommentOutsideLiterals("""val c = '/' // note"""))
        assertTrue(containsCommentOutsideLiterals("""foo() // trailing"""))
        assertTrue(containsCommentOutsideLiterals("""/* block */"""))
    }

    @Test
    fun gateScannersCatchViolatingSyntheticSnippets() {
        assertTrue(
            findImportDirectionViolations(
                layer = "services",
                lines = listOf("import com.kinetix.shop.repositories.auth.SessionRepository"),
            ).isNotEmpty()
        )
        assertTrue(
            findImportDirectionViolations(
                layer = "ui",
                lines = listOf("import com.kinetix.shop.repositories.auth.SessionRepository"),
            ).isNotEmpty()
        )
        assertTrue(
            findImportDirectionViolations(
                layer = "core",
                lines = listOf("import com.kinetix.shop.dto.common.ApiErrorResponseDto"),
            ).isNotEmpty()
        )
        assertTrue(
            findImportDirectionViolations(
                layer = "viewmodels",
                lines = listOf("import com.kinetix.shop.viewmodels.auth.SignInViewModel"),
            ).isEmpty()
        )

        assertTrue(containsDispatchersReference("withContext(Dispatchers.IO) {}"))
        assertFalse(containsDispatchersReference("withContext(ioDispatcher) {}"))

        assertTrue(
            findFunctionTypeParameters(
                """
                class BadViewModel {
                    fun signIn(
                        email: String,
                        onAuthenticated: () -> Unit,
                    ) {}
                }
                """.trimIndent()
            ).isNotEmpty()
        )
        assertTrue(
            findFunctionTypeParameters(
                """
                class BadPrimaryConstructorViewModel(
                    val onAuthenticated: () -> Unit,
                )
                """.trimIndent()
            ).isNotEmpty()
        )
        assertTrue(
            findFunctionTypeParameters(
                """
                class BadSecondaryConstructorViewModel {
                    constructor(onDone: () -> Unit)
                }
                """.trimIndent()
            ).isNotEmpty()
        )
        assertTrue(
            findFunctionTypeParameters(
                """
                class GoodViewModel(
                    private val savedStateHandle: SavedStateHandle,
                ) {
                    fun signIn(
                        email: String,
                        password: String,
                    ) {}
                }
                """.trimIndent()
            ).isEmpty()
        )

        assertTrue(
            findDisallowedDestructiveMigrations(
                relPath = "di/CoreModule.kt",
                sourceText = """
                object CoreModule {
                    fun provideKinetixCacheDatabase(): KinetixCacheDatabase {
                        return Room.databaseBuilder(ctx, KinetixCacheDatabase::class.java, "cache")
                            .fallbackToDestructiveMigration(dropAllTables = true)
                            .build()
                    }
                }
                """.trimIndent(),
            ).isEmpty()
        )
        assertTrue(
            findDisallowedDestructiveMigrations(
                relPath = "di/CoreModule.kt",
                sourceText = """
                object CoreModule {
                    fun provideKinetixCacheDatabase(): KinetixCacheDatabase {
                        return Room.databaseBuilder(ctx, KinetixCacheDatabase::class.java, "cache")
                            .fallbackToDestructiveMigration(dropAllTables = true)
                            .build()
                    }

                    private fun provideKinetixLedgerDatabase(): KinetixLedgerDatabase {
                        return Room.databaseBuilder(ctx, KinetixLedgerDatabase::class.java, "ledger")
                            .fallbackToDestructiveMigration(dropAllTables = true)
                            .build()
                    }
                }
                """.trimIndent(),
            ).isNotEmpty()
        )

        assertTrue(findDisplayTextFailureLiterals("""UnauthorizedFailure("Invalid password")""").isNotEmpty())
        assertTrue(findDisplayTextFailureLiterals("""NetworkFailure("no route to host")""").isNotEmpty())
        assertTrue(
            findDisplayTextFailureLiterals(
                """
                UnauthorizedFailure(
                    "Session expired",
                )
                """.trimIndent()
            ).isNotEmpty()
        )
        assertTrue(findDisplayTextFailureLiterals("""UnauthorizedFailure(code = "NOT_SIGNED_IN")""").isEmpty())
        assertTrue(
            findDisplayTextFailureLiterals(
                """
                UnauthorizedFailure(
                    code = "NOT_SIGNED_IN",
                )
                """.trimIndent()
            ).isEmpty()
        )
        assertTrue(findDisplayTextFailureLiterals("""NetworkFailure()""").isEmpty())

        assertTrue(findSaveableSecrets("""var password by rememberSaveable { mutableStateOf("") }""").isNotEmpty())
        assertTrue(
            findSaveableSecrets(
                """
                var password by
                    rememberSaveable { mutableStateOf("") }
                """.trimIndent()
            ).isNotEmpty()
        )
        assertTrue(findSaveableSecrets("""var totpCode by rememberSaveable { mutableStateOf("") }""").isNotEmpty())
        assertTrue(findSaveableSecrets("""var email by rememberSaveable { mutableStateOf("") }""").isEmpty())
        assertTrue(findSaveableSecrets("""var postalCode by rememberSaveable { mutableStateOf("") }""").isEmpty())
        assertTrue(findSaveableSecrets("""var password by remember { mutableStateOf("") }""").isEmpty())
    }

    @Test
    fun enforcesZeroCommentsInAllKotlinAndGradleFiles() {
        val violations = mutableListOf<String>()
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
                if (containsCommentOutsideLiterals(line)) {
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
            "ui/screens",
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
    fun enforcesLayerImportDirectionFromSection13() {
        val violations = mutableListOf<String>()
        allKotlinFiles(mainJavaRoot).forEach { file ->
            val relPath = file.relativeTo(mainJavaRoot).path.replace(File.separatorChar, '/')
            val layer = relPath.substringBefore('/')
            if (layer in allowedLayerImports) {
                val fileViolations = findImportDirectionViolations(
                    layer = layer,
                    lines = file.readLines(),
                )
                fileViolations.forEach { detail ->
                    violations.add("$relPath $detail")
                }
            }
        }

        assertTrue(
            "Found layer import direction violations:\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    @Test
    fun forbidsDispatchersOutsideDi() {
        val violations = mutableListOf<String>()
        allKotlinFiles(mainJavaRoot).forEach { file ->
            val relPath = file.relativeTo(mainJavaRoot).path.replace(File.separatorChar, '/')
            if (!relPath.startsWith("di/")) {
                file.readLines().forEachIndexed { idx, line ->
                    if (containsDispatchersReference(line)) {
                        violations.add("$relPath:${idx + 1} references Dispatchers directly outside di/")
                    }
                }
            }
        }

        assertTrue(
            "Found Dispatchers violations outside di/:\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    @Test
    fun forbidsFunctionTypeParametersInViewModels() {
        val violations = mutableListOf<String>()
        val viewModelsDir = File(mainJavaRoot, "viewmodels")
        allKotlinFiles(viewModelsDir).forEach { file ->
            val relPath = file.relativeTo(mainJavaRoot).path.replace(File.separatorChar, '/')
            findFunctionTypeParameters(file.readText()).forEach { detail ->
                violations.add("$relPath $detail")
            }
        }

        assertTrue(
            "Found function-type parameters in viewmodels/:\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    @Test
    fun forbidsDestructiveMigrationFallbackOutsideCacheBuilder() {
        val violations = mutableListOf<String>()
        allKotlinFiles(mainJavaRoot).forEach { file ->
            val relPath = file.relativeTo(mainJavaRoot).path.replace(File.separatorChar, '/')
            violations.addAll(
                findDisallowedDestructiveMigrations(
                    relPath = relPath,
                    sourceText = file.readText(),
                )
            )
        }

        assertTrue(
            "Found destructive migration fallback violations:\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    @Test
    fun forbidsDisplayTextPropertiesAndStringLiteralsInFailures() {
        val violations = mutableListOf<String>()
        val failureDir = File(mainJavaRoot, "core/failure")
        val forbiddenPropRegex = Regex("""val\s+(detail|message)\s*:\s*String""")
        allKotlinFiles(failureDir).forEach { file ->
            val relPath = file.relativeTo(mainJavaRoot).path.replace(File.separatorChar, '/')
            file.readLines().forEachIndexed { idx, line ->
                if (forbiddenPropRegex.containsMatchIn(line)) {
                    violations.add("$relPath:${idx + 1} declares display-text property on Failure")
                }
            }
        }

        val failureClassNames = allKotlinFiles(failureDir)
            .map { it.nameWithoutExtension }
            .filter { it.endsWith("Failure") }
            .toSet()

        allKotlinFiles(mainJavaRoot).forEach { file ->
            val relPath = file.relativeTo(mainJavaRoot).path.replace(File.separatorChar, '/')
            findDisplayTextFailureLiterals(
                sourceText = file.readText(),
                failureClassNames = failureClassNames,
            ).forEach { lineNumber ->
                violations.add("$relPath:$lineNumber passes display-text string literal to a Failure")
            }
        }

        assertTrue(
            "Found Failure display-text violations:\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    @Test
    fun forbidsRememberSaveableForSecrets() {
        val violations = mutableListOf<String>()
        val uiDir = File(mainJavaRoot, "ui")
        allKotlinFiles(uiDir).forEach { file ->
            val relPath = file.relativeTo(mainJavaRoot).path.replace(File.separatorChar, '/')
            findSaveableSecrets(file.readText()).forEach { lineNumber ->
                violations.add("$relPath:$lineNumber stores a secret in rememberSaveable")
            }
        }

        assertTrue(
            "Found rememberSaveable secret violations:\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    @Test
    fun enforcesOneTopLevelDeclarationPerFileMatchingFileName() {
        val declarationRegex = Regex(
            """^(?:(?:public|internal|private|abstract|sealed|data|value|enum|annotation|fun)\s+)*(?:class|interface|object)\s+([A-Za-z0-9_]+)|^fun\s+([A-Za-z0-9_]+)\s*\("""
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
        val ioCatchRegex = Regex("""catch\s*\(\s*[A-Za-z0-9_]+\s*:\s*IOException\s*\)""")
        val reposDir = File(
            mainJavaRoot,
            "repositories",
        )
        allKotlinFiles(reposDir).forEach { file ->
            val text = file.readText()
            if (text.contains("SocketTimeoutException") || ioCatchRegex.containsMatchIn(text)) {
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

    private fun findImportDirectionViolations(
        layer: String,
        lines: List<String>,
    ): List<String> {
        val allowed = allowedLayerImports[layer] ?: return emptyList()
        val importRegex = Regex("""^import\s+com\.kinetix\.shop\.([A-Za-z0-9_]+)\b""")
        val violations = mutableListOf<String>()
        lines.forEachIndexed { idx, line ->
            val match = importRegex.find(line.trim()) ?: return@forEachIndexed
            val target = match.groupValues[1]
            if (target !in allowed && target != "R" && target != "BuildConfig") {
                violations.add(":${idx + 1} ($layer -> $target)")
            }
        }
        return violations
    }

    private fun containsDispatchersReference(
        line: String,
    ): Boolean {
        return Regex("""\bDispatchers\.""").containsMatchIn(line)
    }

    private fun findFunctionTypeParameters(
        sourceText: String,
    ): List<String> {
        val violations = mutableListOf<String>()
        val headerRegexes = listOf(
            Regex("""\bfun\s+([A-Za-z0-9_]+)\s*\(""") to "fun",
            Regex(
                """\bclass\s+([A-Za-z0-9_]+)(?:\s*<[^>]*>)?(?:\s*(?:@[A-Za-z0-9_.]+\s*)*(?:public|internal|private|protected)?\s*constructor)?\s*\("""
            ) to "class",
            Regex("""\bconstructor\s*\(""") to "constructor",
        )
        val seenOpenParens = mutableSetOf<Int>()
        headerRegexes.forEach { (regex, kind) ->
            regex.findAll(sourceText).forEach { match ->
                val openParenIndex = match.range.last
                if (!seenOpenParens.add(openParenIndex)) {
                    return@forEach
                }
                val closeParenIndex = findMatchingCloseParen(
                    sourceText = sourceText,
                    openParenIndex = openParenIndex,
                )
                if (closeParenIndex > openParenIndex) {
                    val params = sourceText.substring(openParenIndex + 1, closeParenIndex)
                    if (params.contains("->")) {
                        val name = match.groupValues.getOrNull(1).orEmpty()
                        val label = if (name.isNotEmpty()) "$kind $name" else kind
                        violations.add("$label declares a function-type parameter")
                    }
                }
            }
        }
        return violations
    }

    private fun findDisallowedDestructiveMigrations(
        relPath: String,
        sourceText: String,
    ): List<String> {
        val violations = mutableListOf<String>()
        val funHeaderRegex = Regex(
            """^\s*(?:(?:public|internal|private|protected|override|open|final|suspend|inline)\s+)*fun\s+([A-Za-z0-9_]+)\s*\("""
        )
        var braceDepth = 0
        var pendingFunctionName: String? = null
        var activeFunctionName: String? = null
        var activeFunctionEntryDepth = -1
        sourceText.lines().forEachIndexed { idx, line ->
            val match = funHeaderRegex.find(line)
            if (match != null) {
                pendingFunctionName = match.groupValues[1]
            }
            if (line.contains("fallbackToDestructiveMigration")) {
                val enclosingFunction = activeFunctionName ?: pendingFunctionName
                val isCacheBuilder = relPath == "di/CoreModule.kt" &&
                    enclosingFunction == "provideKinetixCacheDatabase"
                if (!isCacheBuilder) {
                    violations.add(
                        "$relPath:${idx + 1} uses fallbackToDestructiveMigration outside provideKinetixCacheDatabase"
                    )
                }
            }
            var inDoubleQuote = false
            var escaped = false
            for (ch in line) {
                if (escaped) {
                    escaped = false
                    continue
                }
                if (inDoubleQuote) {
                    if (ch == '\\') {
                        escaped = true
                    } else if (ch == '"') {
                        inDoubleQuote = false
                    }
                    continue
                }
                when (ch) {
                    '"' -> inDoubleQuote = true
                    '{' -> {
                        if (pendingFunctionName != null && activeFunctionName == null) {
                            activeFunctionName = pendingFunctionName
                            activeFunctionEntryDepth = braceDepth
                            pendingFunctionName = null
                        }
                        braceDepth += 1
                    }
                    '}' -> {
                        braceDepth = (braceDepth - 1).coerceAtLeast(0)
                        if (activeFunctionName != null && braceDepth <= activeFunctionEntryDepth) {
                            activeFunctionName = null
                            activeFunctionEntryDepth = -1
                        }
                    }
                }
            }
        }
        return violations
    }

    private fun findDisplayTextFailureLiterals(
        sourceText: String,
        failureClassNames: Set<String> = emptySet(),
    ): List<Int> {
        val violationLines = mutableListOf<Int>()
        val failureCallRegex = Regex("""\b([A-Z][A-Za-z0-9_]*Failure)\s*\(""")
        val stringLiteralRegex = Regex(""""([^"]*)"""")
        val machineCodeRegex = Regex("""^(?:[A-Z0-9_]+|\$\{[A-Za-z0-9_.]+\}|\$[A-Za-z0-9_]+)+$""")
        failureCallRegex.findAll(sourceText).forEach { match ->
            val className = match.groupValues[1]
            if (failureClassNames.isNotEmpty() && className !in failureClassNames) {
                return@forEach
            }
            val prefixStart = (match.range.first - 20).coerceAtLeast(0)
            val prefix = sourceText.substring(prefixStart, match.range.first)
            if (Regex("""\b(?:class|interface)\s+$""").containsMatchIn(prefix)) {
                return@forEach
            }
            val openParenIndex = match.range.last
            val closeParenIndex = findMatchingCloseParen(
                sourceText = sourceText,
                openParenIndex = openParenIndex,
            )
            if (closeParenIndex <= openParenIndex) {
                return@forEach
            }
            val args = sourceText.substring(openParenIndex + 1, closeParenIndex)
            val hasDisplayLiteral = stringLiteralRegex.findAll(args).any { literalMatch ->
                val content = literalMatch.groupValues[1]
                content.isNotEmpty() && !machineCodeRegex.matches(content)
            }
            if (hasDisplayLiteral) {
                val lineNumber = sourceText.substring(0, match.range.first).count { it == '\n' } + 1
                violationLines.add(lineNumber)
            }
        }
        return violationLines
    }

    private fun findSaveableSecrets(
        sourceText: String,
    ): List<Int> {
        val violationLines = mutableListOf<Int>()
        val declarationRegex = Regex(
            """\b(?:var|val)\s+([A-Za-z0-9_]+)\s*(?::\s*[^=\n]+)?\s*(?:=|by)\s*rememberSaveable\b"""
        )
        val secretNameRegex = Regex(
            """(password|totp|otp|token|secret|challenge|^code$|Code$)""",
            RegexOption.IGNORE_CASE,
        )
        declarationRegex.findAll(sourceText).forEach { match ->
            val varName = match.groupValues[1]
            if (!varName.equals("postalCode", ignoreCase = true) && secretNameRegex.containsMatchIn(varName)) {
                val lineNumber = sourceText.substring(0, match.range.first).count { it == '\n' } + 1
                violationLines.add(lineNumber)
            }
        }
        return violationLines
    }

    private fun findMatchingCloseParen(
        sourceText: String,
        openParenIndex: Int,
    ): Int {
        var depth = 0
        var inDoubleQuote = false
        var escaped = false
        for (i in openParenIndex until sourceText.length) {
            val ch = sourceText[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (inDoubleQuote) {
                if (ch == '\\') {
                    escaped = true
                } else if (ch == '"') {
                    inDoubleQuote = false
                }
                continue
            }
            when (ch) {
                '"' -> inDoubleQuote = true
                '(' -> depth += 1
                ')' -> {
                    depth -= 1
                    if (depth == 0) {
                        return i
                    }
                }
            }
        }
        return -1
    }

    private fun containsCommentOutsideLiterals(line: String): Boolean {
        var inTripleQuote = false
        var inDoubleQuote = false
        var inSingleQuote = false
        var escaped = false
        var index = 0
        while (index < line.length) {
            val ch = line[index]
            if (inTripleQuote) {
                if (line.startsWith("\"\"\"", index)) {
                    inTripleQuote = false
                    index += 3
                } else {
                    index += 1
                }
                continue
            }
            if (escaped) {
                escaped = false
                index += 1
                continue
            }
            if (inDoubleQuote) {
                if (ch == '\\') {
                    escaped = true
                } else if (ch == '"') {
                    inDoubleQuote = false
                }
                index += 1
                continue
            }
            if (inSingleQuote) {
                if (ch == '\\') {
                    escaped = true
                } else if (ch == '\'') {
                    inSingleQuote = false
                }
                index += 1
                continue
            }
            if (line.startsWith("\"\"\"", index)) {
                inTripleQuote = true
                index += 3
                continue
            }
            if (ch == '"') {
                inDoubleQuote = true
                index += 1
                continue
            }
            if (ch == '\'') {
                inSingleQuote = true
                index += 1
                continue
            }
            if (ch == '/' && index + 1 < line.length) {
                val next = line[index + 1]
                if (next == '/' || next == '*') {
                    return true
                }
            }
            index += 1
        }
        return false
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
