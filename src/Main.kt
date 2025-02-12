import java.io.File

fun main() {
    val inputDirPath = "refal_sources_1"
    val outputDirPath = "refal_output"
    val astDirPath = "refal_ast"
    val dedupedDirPath = "refal_deduped"
    val normalizedDirPath = "refal_normalized"
    val expectedDirPath = "refal_expected"

    val outputDir = File(outputDirPath)
    val astDir = File(astDirPath)
    val dedupedDir = File(dedupedDirPath)
    val normalizedDir = File(normalizedDirPath)

    if (outputDir.exists()) outputDir.deleteRecursively()
    if (astDir.exists()) astDir.deleteRecursively()
    if (dedupedDir.exists()) dedupedDir.deleteRecursively()
    if (normalizedDir.exists()) normalizedDir.deleteRecursively()

    outputDir.mkdirs()
    astDir.mkdirs()
    dedupedDir.mkdirs()
    normalizedDir.mkdirs()

    val inputDir = File(inputDirPath)
    if (!inputDir.exists() || !inputDir.isDirectory) {
        println("Указанная папка с файлами .ref не существует или не является директорией: $inputDirPath")
        return
    }

    inputDir.walk().filter { file -> file.isFile && file.extension == "ref" }.forEach { refFile ->
        try {
            println("Обрабатываем файл: ${refFile.name}")

            val code = refFile.readText()

            val lexer = Lexer(code)
            val tokens = lexer.tokenize()

            val tokensOutputFile = File(outputDir, "${refFile.nameWithoutExtension}_tokens.txt")
            tokensOutputFile.printWriter().use { out ->
                tokens.forEach { token -> out.println(token) }
            }

            val parser = Parser(tokens)
            val tree = parser.parse()

            val astOutputFile = File(astDir, "${refFile.nameWithoutExtension}_ast.txt")
            astOutputFile.printWriter().use { out ->
                out.println(AstTreePrinter.printAst(tree))
            }

            val normalizedTree = FunctionNormalizer.normalize(tree)
            val normilizedOutputFile = File(normalizedDir, "${refFile.nameWithoutExtension}_normalized.ref")
            normilizedOutputFile.printWriter().use { out ->
                out.println(AstTreePrinter.printAst(normalizedTree))
            }

            val deduplicatedTree = FunctionDeduplicator.deduplicateFunctions(normalizedTree)

            val refalCode = RefalCodeGenerator.generate(deduplicatedTree).trimEnd() + "\n"

            val dedupedOutputFile = File(dedupedDir, "${refFile.nameWithoutExtension}_optimized.ref")
            dedupedOutputFile.writeText(refalCode)

            println("Файл ${refFile.name} успешно обработан и оптимизирован.")
        } catch (e: Exception) {
            println("Ошибка при обработке файла ${refFile.name}: ${e.message}")
        }
    }

    println("Готово! Оптимизированные файлы сохранены в папку: $dedupedDirPath")
    compareFiles(expectedDirPath, dedupedDirPath)
}


fun compareFiles(expectedDir: String, actualDir: String) {
    val expectedFiles = File(expectedDir).walk().filter { it.isFile }.toList()

    for (expectedFile in expectedFiles) {
        val actualFile = File(actualDir, expectedFile.name)
        if (!actualFile.exists()) {
            println("Файл ${expectedFile.name} отсутствует в папке результатов!")
            continue
        }

        val expectedLines = expectedFile.readLines().map { it.trim().replace(Regex("\\s+"), " ") }
        val actualLines = actualFile.readLines().map { it.trim().replace(Regex("\\s+"), " ") }

        if (expectedLines == actualLines) {
            println("Файл ${expectedFile.name} совпадает с ожидаемым")
        } else {
            println("Файл ${expectedFile.name} отличается от ожидаемого!")
            println("Ожидаемое содержимое:")
            expectedLines.forEachIndexed { i, line -> println("${i + 1}: $line") }
            println("\nФактическое содержимое:")
            actualLines.forEachIndexed { i, line -> println("${i + 1}: $line") }

            for (i in expectedLines.indices) {
                val expected = expectedLines.getOrNull(i) ?: "<пусто>"
                val actual = actualLines.getOrNull(i) ?: "<пусто>"
                if (expected != actual) {
                    println("Строка ${i + 1}:")
                    println("Ожидаемое: \"$expected\"")
                    println("Фактическое: \"$actual\"")
                    println()
                }
            }
            throw FileComparisonException("Файл ${expectedFile.name} содержит различия!")
        }
    }
}




class FileComparisonException(message: String) : Exception(message)
