import main.kotlin.Parser


object FunctionNormalizer {
    fun normalize(program: Parser.Program): Parser.Program {
        val normalizedElements = program.elements.map { element ->
            when (element) {
                is Parser.ProgramElement.ExternDeclaration -> element
                is Parser.ProgramElement.FunctionDefinition -> normalizeFunction(element)
            }
        }
        return Parser.Program(normalizedElements)
    }

    fun normalizeFunction(function: Parser.ProgramElement.FunctionDefinition): Parser.ProgramElement.FunctionDefinition {
        val normalizedBody = function.body.map { sentence ->
            Parser.Sentence(
                normalizePattern(sentence.pattern),
                normalizeResult(sentence.result)
            )
        }
        return Parser.ProgramElement.FunctionDefinition(function.name, function.isEntryPoint, normalizedBody)
    }

    private fun normalizePattern(pattern: Parser.Pattern): Parser.Pattern =
        Parser.Pattern(mergePatternLiterals(pattern.elements.map { normalizePatternElement(it) }))

    private fun normalizeResult(result: Parser.Result): Parser.Result =
        Parser.Result(mergeResultLiterals(result.elements.map { normalizeResultElement(it) }))

    private fun normalizePatternElement(element: Parser.PatternElement): Parser.PatternElement {
        return when (element) {
            is Parser.PatternElement.StringVal -> normalizeStringVal(element)
            is Parser.PatternElement.ParenStructure ->
                Parser.PatternElement.ParenStructure(element.elements.map { normalizePatternElement(it) })
            else -> element
        }
    }

    private fun normalizeResultElement(element: Parser.ResultElement): Parser.ResultElement {
        return when (element) {
            is Parser.ResultElement.StringVal -> normalizeStringVal(element)
            is Parser.ResultElement.AngleStructure ->
                Parser.ResultElement.AngleStructure(
                    element.constructor, element.elements.map { normalizeResultElement(it) }
                )
            is Parser.ResultElement.ParenStructure ->
                Parser.ResultElement.ParenStructure(element.elements.map { normalizeResultElement(it) })
            else -> element
        }
    }

    // 🔹 **НОВОЕ:** Нормализуем вызовы функций в графе вызовов
    fun normalizeCallGraph(
        callGraph: Map<String, MutableSet<Pair<String, List<Parser.ResultElement>>>>
    ): Map<String, MutableSet<Pair<String, List<Parser.ResultElement>>>> {
        return callGraph.mapValues { (_, calls) ->
            calls.map { (func, args) ->
                Pair(func, args.map { normalizeResultElement(it) })
            }.toMutableSet()
        }
    }

    // 🔹 **НОВОЕ:** Нормализуем вызывающие функции (callers)
    fun normalizeCallers(
        callers: Map<String, MutableSet<String>>
    ): Map<String, MutableSet<String>> {
        return callers.mapValues { (_, callingFunctions) -> callingFunctions.toMutableSet() }
    }

    // 🔹 **НОВОЕ:** Нормализуем аргументы вызовов функций
    fun normalizeFunctionCalls(body: List<Parser.Sentence>): List<Parser.Sentence> {
        return body.map { sentence ->
            val normalizedElements = sentence.result.elements.map { normalizeResultElement(it) }
            sentence.copy(result = Parser.Result(normalizedElements))
        }
    }

    // Функция объединения литералов: 'x' 'y' 'z' → 'xyz'
    private fun mergeResultLiterals(elements: List<Parser.ResultElement>): List<Parser.ResultElement> {
        val merged = mutableListOf<Parser.ResultElement>()
        var buffer: StringBuilder? = null

        for (element in elements) {
            if (element is Parser.ResultElement.Literal) {
                if (buffer == null) {
                    buffer = StringBuilder()
                }
                buffer.append(element.value.replace("'", "")) // Убираем кавычки временно
            } else {
                if (buffer != null) {
                    merged.add(Parser.ResultElement.Literal("'$buffer'")) // Возвращаем одинарные кавычки
                    buffer = null
                }
                merged.add(element)
            }
        }

        if (buffer != null) {
            merged.add(Parser.ResultElement.Literal("'$buffer'")) // Возвращаем кавычки
        }

        return merged
    }

    private fun mergePatternLiterals(elements: List<Parser.PatternElement>): List<Parser.PatternElement> {
        val merged = mutableListOf<Parser.PatternElement>()
        var buffer: StringBuilder? = null

        for (element in elements) {
            if (element is Parser.PatternElement.Literal) {
                if (buffer == null) {
                    buffer = StringBuilder()
                }
                buffer.append(element.value.replace("'", "")) // Убираем кавычки временно
            } else {
                if (buffer != null) {
                    merged.add(Parser.PatternElement.Literal("'$buffer'")) // Возвращаем одинарные кавычки
                    buffer = null
                }
                merged.add(element)
            }
        }

        if (buffer != null) {
            merged.add(Parser.PatternElement.Literal("'$buffer'")) // Возвращаем кавычки
        }

        return merged
    }

    private fun normalizeStringVal(element: Parser.PatternElement.StringVal): Parser.PatternElement.StringVal {
        return if (element.value.startsWith("\"") && element.value.endsWith("\"")) {
            element // Если уже в кавычках, не меняем
        } else {
            Parser.PatternElement.StringVal("\"${element.value}\"") // Добавляем кавычки
        }
    }

    private fun normalizeStringVal(element: Parser.ResultElement.StringVal): Parser.ResultElement.StringVal {
        return if (element.value.startsWith("\"") && element.value.endsWith("\"")) {
            element // Если уже в кавычках, не меняем
        } else {
            Parser.ResultElement.StringVal("\"${element.value}\"") // Добавляем кавычки
        }
    }
}
