object FunctionDeduplicator {
    fun deduplicateFunctions(program: Parser.Program): Parser.Program {
        val functionBodies = mutableMapOf<String, List<Parser.Sentence>>()

        println("Начинаем сбор информации о функциях...")

        // 1. Сохраняем тела всех функций
        for (function in program.elements.filterIsInstance<Parser.ProgramElement.FunctionDefinition>()) {
            functionBodies[function.name] = function.body
            println("Функция ${function.name}: тело -> ${function.body}")
        }

        // 2. Производим подстановку вызовов функций
        println("\nНачинаем раскрытие вызовов функций...")
        inlineFunctionCalls(functionBodies)
        println("Завершена подстановка вызовов.\n")

        // 3. Ищем эквивалентные функции
        val equivalenceClasses = findEquivalentFunctions(functionBodies)
        println("Найдены классы эквивалентности: $equivalenceClasses")

        // 4. Обновляем вызовы после объединения эквивалентных функций
        replaceEquivalentFunctions(functionBodies, equivalenceClasses)
        equivalenceClasses.entries.removeIf { it.value == it.key }

        // 5. Создаем обновленный список функций без дубликатов
        val updatedFunctions = program.elements.mapNotNull { element ->
            when (element) {
                is Parser.ProgramElement.FunctionDefinition -> {
                    if (equivalenceClasses.containsKey(element.name)) null
                    else functionBodies[element.name]?.let { newBody -> element.copy(body = newBody) }
                }

                else -> element
            }
        }
        return Parser.Program(updatedFunctions)
    }

    /**
     * Итеративно заменяет вызовы функций их телами до конечных значений.
     */
    private fun inlineFunctionCalls(functionBodies: MutableMap<String, List<Parser.Sentence>>) {
        var changed = true
        while (changed) {
            changed = false
            for ((func, body) in functionBodies) {
                val newBody = body.map { sentence ->
                    val inlinedElements = sentence.result.elements.flatMap {
                        replaceFunctionCall(it, functionBodies, mutableSetOf(func))
                    }
                    if (inlinedElements != sentence.result.elements) {
                        changed = true
                    }
                    sentence.copy(result = Parser.Result(inlinedElements))
                }
                functionBodies[func] = newBody
            }
        }
    }

    /**
     * Заменяет вызов функции (`AngleStructure`) на ее тело, если возможно.
     * Учитывает рекурсию.
     */
    private fun replaceFunctionCall(
        element: Parser.ResultElement,
        functionBodies: Map<String, List<Parser.Sentence>>,
        visitedFunctions: MutableSet<String>
    ): List<Parser.ResultElement> {
        return when (element) {
            is Parser.ResultElement.AngleStructure -> {
                if (visitedFunctions.contains(element.constructor)) {
                    println("⚠️ Обнаружена рекурсия в ${element.constructor}, останавливаем раскрытие.")
                    return listOf(element)
                }
                val body = functionBodies[element.constructor]
                if (body != null) {
                    for (sentence in body) {
                        if (sentence.pattern.elements.size == element.elements.size &&
                            sentence.pattern.elements.zip(element.elements).all { (p, c) ->
                                p is Parser.PatternElement.Literal &&
                                        c is Parser.ResultElement.Literal &&
                                        p.value == c.value
                            }
                        ) {
                            visitedFunctions.add(element.constructor)

                            // Раскрываем результат до конца (все вложенные вызовы)
                            val replaced = sentence.result.elements.flatMap {
                                replaceFunctionCall(it, functionBodies, visitedFunctions)
                            }
                            return if (replaced.isEmpty()) listOf(element) else replaced
                        }
                    }
                }
                listOf(element)
            }

            is Parser.ResultElement.ParenStructure -> {
                val newElements = element.elements.flatMap { replaceFunctionCall(it, functionBodies, visitedFunctions) }
                if (newElements.isEmpty()) listOf(element) else listOf(Parser.ResultElement.ParenStructure(newElements))
            }

            else -> listOf(element)
        }
    }




    /**
     *  Находит пары эквивалентных функций.
     */
    private fun findEquivalentFunctions(functionBodies: Map<String, List<Parser.Sentence>>): MutableMap<String, String> {
        val equivalenceMap = mutableMapOf<String, String>()

        for ((func1, body1) in functionBodies) {
            for ((func2, body2) in functionBodies) {
                if (func1 != func2 && isFunctionallyEquivalent(body1, body2)) {
                    val canonical1 = findCanonical(func1, equivalenceMap)
                    val canonical2 = findCanonical(func2, equivalenceMap)
                    val canonical = minOf(canonical1, canonical2)

                    equivalenceMap[canonical1] = canonical
                    equivalenceMap[canonical2] = canonical
                    equivalenceMap[func1] = canonical
                    equivalenceMap[func2] = canonical
                }
            }
        }

        return equivalenceMap
    }

    /**
     * Находит каноническое имя функции (самую "раннюю" эквивалентную).
     */
    private fun findCanonical(func: String, equivalenceMap: MutableMap<String, String>): String {
        var current = func
        while (equivalenceMap.containsKey(current) && equivalenceMap[current] != current) {
            current = equivalenceMap[current]!!
        }
        return current
    }

    /**
     * Проверяет, эквивалентны ли две функции.
     */
    private fun isFunctionallyEquivalent(body1: List<Parser.Sentence>, body2: List<Parser.Sentence>): Boolean {
        if (body1.size != body2.size) return false

        val forwardMapping = mutableMapOf<String, String>()
        val reverseMapping = mutableMapOf<String, String>()

        // Проверяем, ведут ли себя функции одинаково на всех возможных входах
        val behavesSame = body1.zip(body2).all { (s1, s2) ->
            val patternEqual = isPatternEquivalent(s1.pattern, s2.pattern, forwardMapping, reverseMapping)
            val resultEqual = isResultEquivalent(s1.result, s2.result, forwardMapping, reverseMapping)

            if (!patternEqual || !resultEqual) return false
            true
        }

        if (!behavesSame) return false

        // Проверяем, влияет ли порядок на поведение
        return !doesOrderAffectBehavior(body1) && !doesOrderAffectBehavior(body2)
    }

    /**
     * Проверяет, влияет ли порядок предложений на поведение функции.
     */
    private fun doesOrderAffectBehavior(body: List<Parser.Sentence>): Boolean {
        for (i in body.indices) {
            for (j in i + 1 until body.size) {
                val first = body[i]
                val second = body[j]

                // Если первый шаблон более общий, а второй более конкретный, то порядок важен
                if (isMoreGeneralPattern(first.pattern, second.pattern)) {
                    println("Порядок предложений влияет: ${first.pattern} перед ${second.pattern}")
                    return true
                }
            }
        }
        return false
    }

    /**
     * Проверяет, является ли `general` более общим шаблоном, чем `specific`, учитывая вложенные структуры.
     */
    private fun isMoreGeneralPattern(general: Parser.Pattern, specific: Parser.Pattern): Boolean {
        if (general.elements.size != specific.elements.size) return false

        return general.elements.zip(specific.elements).all { (g, s) ->
            when {
                g is Parser.PatternElement.Variable && s is Parser.PatternElement.Literal -> true
                g is Parser.PatternElement.Variable && s is Parser.PatternElement.Variable -> true
                g is Parser.PatternElement.Literal && s is Parser.PatternElement.Literal -> g.value == s.value
                g is Parser.PatternElement.ParenStructure && s is Parser.PatternElement.ParenStructure ->
                    isMoreGeneralPattern(Parser.Pattern(g.elements), Parser.Pattern(s.elements))
                else -> false
            }
        }
    }



    /**
     *  Заменяет вызовы эквивалентных функций их каноническими версиями.
     */
    private fun replaceEquivalentFunctions(
        functionBodies: MutableMap<String, List<Parser.Sentence>>,
        equivalenceMap: Map<String, String>
    ) {
        for ((func, body) in functionBodies) {
            functionBodies[func] = body.map { sentence ->
                val newElements = sentence.result.elements.map {
                    val replaced = replaceEquivalentCall(it, equivalenceMap)
                    if (replaced is Parser.ResultElement.AngleStructure && equivalenceMap.containsKey(replaced.constructor)) {
                        replaced.copy(constructor = equivalenceMap[replaced.constructor] ?: replaced.constructor)
                    } else replaced
                }
                sentence.copy(result = Parser.Result(newElements))
            }
        }
    }


    private fun replaceEquivalentCall(
        element: Parser.ResultElement,
        equivalenceMap: Map<String, String>
    ): Parser.ResultElement {
        return when (element) {
            is Parser.ResultElement.AngleStructure ->
                element.copy(constructor = equivalenceMap[element.constructor] ?: element.constructor)

            is Parser.ResultElement.ParenStructure ->
                element.copy(elements = element.elements.map { replaceEquivalentCall(it, equivalenceMap) })

            else -> element
        }
    }


    /**
     *  Проверяет, эквивалентны ли шаблоны (левая часть).
     */
    private fun isPatternEquivalent(
        p1: Parser.Pattern,
        p2: Parser.Pattern,
        forwardMapping: MutableMap<String, String>,
        reverseMapping: MutableMap<String, String>
    ): Boolean {
        if (p1.elements.size != p2.elements.size) return false

        return p1.elements.zip(p2.elements).all { (e1, e2) ->
            when {
                // Переменные должны соответствовать друг другу биективно
                e1 is Parser.PatternElement.Variable && e2 is Parser.PatternElement.Variable ->
                    mapVariablesBijection(e1.name, e2.name, forwardMapping, reverseMapping)

                // Литералы должны совпадать
                e1 is Parser.PatternElement.Literal && e2 is Parser.PatternElement.Literal ->
                    e1.value == e2.value

                // Числа должны быть равны
                e1 is Parser.PatternElement.Number && e2 is Parser.PatternElement.Number ->
                    e1.value == e2.value

                // Строковые значения должны быть равны
                e1 is Parser.PatternElement.StringVal && e2 is Parser.PatternElement.StringVal ->
                    e1.value == e2.value

                // Символы должны быть одинаковыми
                e1 is Parser.PatternElement.Symbol && e2 is Parser.PatternElement.Symbol ->
                    e1.text == e2.text

                // Вложенные структуры должны быть эквивалентными
                e1 is Parser.PatternElement.ParenStructure && e2 is Parser.PatternElement.ParenStructure ->
                    isPatternEquivalent(
                        Parser.Pattern(e1.elements),
                        Parser.Pattern(e2.elements),
                        forwardMapping,
                        reverseMapping
                    )

                else -> false
            }
        }
    }


    /**
     *  Проверяет, эквивалентны ли результаты (правая часть).
     */
    private fun isResultEquivalent(
        r1: Parser.Result,
        r2: Parser.Result,
        forwardMapping: MutableMap<String, String>,
        reverseMapping: MutableMap<String, String>
    ): Boolean {
        if (r1.elements.size != r2.elements.size) return false

        return r1.elements.zip(r2.elements).all { (e1, e2) ->
            when {
                // Переменные должны соответствовать друг другу биективно
                e1 is Parser.ResultElement.Variable && e2 is Parser.ResultElement.Variable ->
                    mapVariablesBijection(e1.name, e2.name, forwardMapping, reverseMapping)

                // Литералы должны совпадать
                e1 is Parser.ResultElement.Literal && e2 is Parser.ResultElement.Literal ->
                    e1.value == e2.value

                // Числа должны быть равны
                e1 is Parser.ResultElement.Number && e2 is Parser.ResultElement.Number ->
                    e1.value == e2.value

                // Строки должны быть равны
                e1 is Parser.ResultElement.StringVal && e2 is Parser.ResultElement.StringVal ->
                    e1.value == e2.value

                // Символы должны быть одинаковыми
                e1 is Parser.ResultElement.Symbol && e2 is Parser.ResultElement.Symbol ->
                    e1.text == e2.text

                // Вложенные структуры должны быть эквивалентными
                e1 is Parser.ResultElement.ParenStructure && e2 is Parser.ResultElement.ParenStructure ->
                    isResultEquivalent(
                        Parser.Result(e1.elements),
                        Parser.Result(e2.elements),
                        forwardMapping,
                        reverseMapping
                    )

                // Вызовы функций (`AngleStructure`) должны вести к эквивалентным результатам
                e1 is Parser.ResultElement.AngleStructure && e2 is Parser.ResultElement.AngleStructure ->
                    e1.constructor == e2.constructor &&
                            isResultEquivalent(
                                Parser.Result(e1.elements),
                                Parser.Result(e2.elements),
                                forwardMapping,
                                reverseMapping
                            )

                else -> false
            }
        }
    }


    /**
     * Проверяет, можно ли установить биективное соответствие между переменными двух шаблонов.
     * Если соответствие уже есть, оно проверяется, иначе создается новое.
     */
    private fun mapVariablesBijection(
        v1: String, v2: String,
        forwardMap: MutableMap<String, String>,
        reverseMap: MutableMap<String, String>
    ): Boolean {
        val existingForward = forwardMap[v1]
        val existingReverse = reverseMap[v2]

        return when {
            // Если соответствие еще не установлено, создаем его
            existingForward == null && existingReverse == null -> {
                forwardMap[v1] = v2
                reverseMap[v2] = v1
                true
            }
            // Если соответствие уже есть и совпадает — ок
            existingForward == v2 && existingReverse == v1 -> true
            // Если есть несовместимое соответствие — шаблоны не эквивалентны
            else -> false
        }
    }
}
