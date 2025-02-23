import java.io.File
import java.nio.file.Paths
import main.kotlin.AstTreePrinter
import main.kotlin.Lexer
import main.kotlin.Parser

class RefalCodeProcessor {
    fun process() {
        val resourceUrl = object {}.javaClass.getResource("/refal_sources")
            ?: throw RuntimeException("Каталог refal_sources не найден в ресурсах")
        val inputDirPath = Paths.get(resourceUrl.toURI()).toFile().absolutePath
        val outputDirPath = "refal_output"
        val astDirPath = "refal_ast"
        val dedupedDirPath = "refal_deduped"
        val normalizedDirPath = "refal_normalized"
        //val expectedDirPath = "refal_expected"

        val outputDir = File(outputDirPath)
        val astDir = File(astDirPath)
        val dedupedDir = File(dedupedDirPath)
        val normalizedDir = File(normalizedDirPath)

        // Удаляем старые папки и создаём новые
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

        // Для каждого файла .ref
        inputDir.walk().filter { file -> file.isFile && file.extension == "ref" }.forEach { refFile ->
            try {
                println("Обрабатываем файл: ${refFile.name}")

                val code = refFile.readText()
                println("Исходный код:\n$code")

                // Лексический и синтаксический анализ
                val lexer = Lexer(code)
                val tokens = lexer.tokenize()

                // Записываем токены для отладки
                val tokensOutputFile = File(outputDir, "${refFile.nameWithoutExtension}_tokens.txt")
                tokensOutputFile.printWriter().use { out ->
                    tokens.forEach { token -> out.println(token) }
                }

                val parser = Parser(tokens)
                val tree = parser.parse()

                val semanticAnalyzer = SemanticAnalyzer()
                val errors = semanticAnalyzer.analyze(tree)

                // Если есть ошибки, выводим и пропускаем этот файл
                if (errors.isNotEmpty()) {
                    println("Ошибки семантического анализа в файле ${refFile.name}:")
                    errors.forEach { error -> println(error) }
                    println("Пропускаем файл ${refFile.name} и переходим к следующему.")
                    return@forEach // Переходим к следующему файлу
                }
                // Вывод AST для отладки
                val astOutputFile = File(astDir, "${refFile.nameWithoutExtension}_ast.txt")
                astOutputFile.printWriter().use { out ->
                    out.println(AstTreePrinter.printAst(tree))
                }

                // Нормализация AST
                val normalizedTree = FunctionNormalizer.normalize(tree)
                val normalizedOutputFile = File(normalizedDir, "${refFile.nameWithoutExtension}_normalized.ref")
                normalizedOutputFile.printWriter().use { out ->
                    out.println(AstTreePrinter.printAst(normalizedTree))
                }

                // Извлечение информации о функциях: функцияBodies и callGraph
                // (Предполагается, что у вас есть класс FunctionExtractor, который возвращает FunctionInfo)
                val extractor = FunctionExtractor()
                val functionInfo = extractor.extractFunctions(normalizedTree)
                val functionBodies = functionInfo.functionBodies

                // Дедупликация: удаляем дублирующие функции и заменяем вызовы на канонические имена.
                val deduplicator = FunctionDeduplicator()
                val dedupedTree = deduplicator.deduplicate(normalizedTree, functionBodies)

                // Генерация финального кода
                val refalCode = RefalCodeGenerator.generate(dedupedTree).trimEnd() + "\n"

                // Запись оптимизированного кода
                val dedupedOutputFile = File(dedupedDir, "${refFile.nameWithoutExtension}_optimized.ref")
                dedupedOutputFile.writeText(refalCode)

                println("Файл ${refFile.name} успешно обработан и оптимизирован.")
            } catch (e: Exception) {
                println("Ошибка при обработке файла ${refFile.name}: ${e.message}")
            }
        }

        println("Готово! Оптимизированные файлы сохранены в папку: $dedupedDirPath")
       // compareFiles(expectedDirPath, dedupedDirPath)
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
                throw FileComparisonException("Файл ${expectedFile.name} содержит различия!")
            }
        }
    }

    class FileComparisonException(message: String) : Exception(message)

}