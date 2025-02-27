class FunctionEquivalenceChecker {
    private val patternComparator = PatternComparator()
    private val resultComparator = ResultComparator()
    private val patternIntersectionChecker = PatternIntersectionChecker()

    /**
     * Возвращает true, если функции f1 и f2 эквивалентны.
     * @param functionBodies отображение имен функций в их списки предложений (каркасный AST)
     * @param level уровень бисимуляции
     * @param bisimulationGroups классы эквивалентности по бисимуляциям
     */
    fun areEquivalent(
        f1: String,
        f2: String,
        functionBodies: Map<String, List<Parser.Sentence>>,
        level: Int,
        bisimulationGroups: List<Set<String>>
    ): Boolean {
        if (f1 == f2) return true

        val currentEquiv = areBisimilar(f1, f2, functionBodies, level = level, bisimulationGroups = bisimulationGroups)
        return currentEquiv
    }

    /**
     * Рекурсивное сравнение функций с учетом уровня итерации.
     * При level == 0 имена вызываемых функций игнорируются (сравниваются только аргументы),
     * а при level > 0 сравниваются только результаты, паттерны игнорируются.
     */
    private fun areBisimilar(
        f1: String,
        f2: String,
        functionBodies: Map<String, List<Parser.Sentence>>,
        level: Int,
        bisimulationGroups: List<Set<String>>
    ): Boolean {
        if (f1 == f2) return true

        val body1 = functionBodies[f1] ?: return false
        val body2 = functionBodies[f2] ?: return false

        if (body1.size != body2.size) {
            println("Разное число предложений: ${body1.size} vs ${body2.size} для $f1 и $f2")
            return false
        }

        val unmatched = body2.toMutableList()
        for (s1 in body1) {
            var match: Parser.Sentence? = null
            for (s2 in unmatched) {
                val forwardMap: MutableMap<Pair<String, String>, String> = mutableMapOf()
                val reverseMap: MutableMap<Pair<String, String>, String> = mutableMapOf()

                val areResultsEquvalient = resultComparator.areEquivalent(s1.result, s2.result, forwardMap, reverseMap, bisimulationGroups, level)

                if (level > 0) {

                    if (areResultsEquvalient) {
                        match = s2
                        break
                    }
                } else {

                    val arePatternsEquivalent = patternComparator.areEquivalent(s1.pattern, s2.pattern, forwardMap, reverseMap)
                    if (arePatternsEquivalent && areResultsEquvalient) {
                        match = s2
                        break
                    }

                    val arePatternsIntersect = patternIntersectionChecker.hasIntersection(s1.pattern, s2.pattern)
                    if (arePatternsIntersect)
                        return false
                }
            }
            if (match == null) {
                println("Не найдено соответствие для предложения $s1 в $f1")
                return false
            }
            unmatched.remove(match)
        }
        return true
    }
}
