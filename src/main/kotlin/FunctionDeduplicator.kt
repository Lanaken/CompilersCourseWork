class FunctionDeduplicator(
    private val grouper: FunctionEquivalenceGrouper = FunctionEquivalenceGrouper(),
) {
    /**
     * Удаляет дублирующие функции из AST.
     * Заменяет вызовы дублирующих функций на каноническое имя.
     */
    fun deduplicate(
        program: Parser.Program,
        functionBodies: Map<String, List<Parser.Sentence>>,
    ): Parser.Program {
        val groups = grouper.groupFunctions(functionBodies)
        val duplicates = mutableSetOf<String>()
        val replacementMap = mutableMapOf<String, String>()
        // Выбираем каноническое имя для каждой группы: все функции кроме канонической считаются дубликатами.
        groups.forEach { (canonical, group) ->
            group.forEach { funcName ->
                if (funcName != canonical) {
                    duplicates.add(funcName)
                    replacementMap[funcName] = canonical
                }
            }
        }
        // Обновляем AST: заменяем вызовы и удаляем определения дубликатов.
        val updatedElements = program.elements.map { element ->
            if (element is Parser.ProgramElement.FunctionDefinition) {
                val newBody = element.body.map { sentence ->
                    val newResult = replaceCallsInResult(sentence.result, replacementMap)
                    sentence.copy(result = newResult)
                }
                element.copy(body = newBody)
            } else element
        }.filterNot { it is Parser.ProgramElement.FunctionDefinition && it.name in duplicates }
        return Parser.Program(updatedElements)
    }

    private fun replaceCallsInResult(
        result: Parser.Result,
        replacementMap: Map<String, String>
    ): Parser.Result {
        val newElements = result.elements.map { re ->
            when (re) {
                is Parser.ResultElement.ResultAngleStructure -> {
                    val newConstructor = replacementMap[re.constructor] ?: re.constructor
                    val newElements = replaceCallsInResult(Parser.Result(re.elements), replacementMap).elements
                    re.copy(constructor = newConstructor, elements = newElements)
                }
                is Parser.ResultElement.ResultParenStructure -> {
                    val newElements = replaceCallsInResult(Parser.Result(re.elements), replacementMap).elements
                    re.copy(elements = newElements)
                }
                else -> re
            }
        }
        return Parser.Result(newElements)
    }
}
