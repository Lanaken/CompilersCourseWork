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
            is Parser.PatternElement.PatternStringVal -> normalizeStringVal(element)
            is Parser.PatternElement.PatternParenStructure ->
                Parser.PatternElement.PatternParenStructure(element.elements.map { normalizePatternElement(it) })
            else -> element
        }
    }

    private fun normalizeResultElement(element: Parser.ResultElement): Parser.ResultElement {
        return when (element) {
            is Parser.ResultElement.ResultStringVal -> normalizeStringVal(element)
            is Parser.ResultElement.ResultAngleStructure ->
                Parser.ResultElement.ResultAngleStructure(
                    element.constructor, element.elements.map { normalizeResultElement(it) }
                )
            is Parser.ResultElement.ResultParenStructure ->
                Parser.ResultElement.ResultParenStructure(element.elements.map { normalizeResultElement(it) })
            else -> element
        }
    }

    // Функция объединения литералов: 'x' 'y' 'z' → 'xyz'
    private fun mergeResultLiterals(elements: List<Parser.ResultElement>): List<Parser.ResultElement> {
        val merged = mutableListOf<Parser.ResultElement>()
        var buffer: StringBuilder? = null

        for (element in elements) {
            if (element is Parser.ResultElement.ResultLiteral) {
                if (buffer == null) {
                    buffer = StringBuilder()
                }
                buffer.append(element.value.replace("'", "")) // Убираем кавычки временно
            } else {
                if (buffer != null) {
                    merged.add(Parser.ResultElement.ResultLiteral("'$buffer'")) // Возвращаем одинарные кавычки
                    buffer = null
                }
                merged.add(element)
            }
        }

        if (buffer != null) {
            merged.add(Parser.ResultElement.ResultLiteral("'$buffer'")) // Возвращаем кавычки
        }

        return merged
    }

    private fun mergePatternLiterals(elements: List<Parser.PatternElement>): List<Parser.PatternElement> {
        val merged = mutableListOf<Parser.PatternElement>()
        var buffer: StringBuilder? = null

        for (element in elements) {
            if (element is Parser.PatternElement.PatternLiteral) {
                if (buffer == null) {
                    buffer = StringBuilder()
                }
                buffer.append(element.value.replace("'", "")) // Убираем кавычки временно
            } else {
                if (buffer != null) {
                    merged.add(Parser.PatternElement.PatternLiteral("'$buffer'")) // Возвращаем одинарные кавычки
                    buffer = null
                }
                merged.add(element)
            }
        }

        if (buffer != null) {
            merged.add(Parser.PatternElement.PatternLiteral("'$buffer'")) // Возвращаем кавычки
        }

        return merged
    }

    private fun normalizeStringVal(element: Parser.PatternElement.PatternStringVal): Parser.PatternElement.PatternStringVal {
        return if (element.value.startsWith("\"") && element.value.endsWith("\"")) {
            element // Если уже в кавычках, не меняем
        } else {
            Parser.PatternElement.PatternStringVal("\"${element.value}\"") // Добавляем кавычки
        }
    }

    private fun normalizeStringVal(element: Parser.ResultElement.ResultStringVal): Parser.ResultElement.ResultStringVal {
        return if (element.value.startsWith("\"") && element.value.endsWith("\"")) {
            element // Если уже в кавычках, не меняем
        } else {
            Parser.ResultElement.ResultStringVal("\"${element.value}\"") // Добавляем кавычки
        }
    }
}
