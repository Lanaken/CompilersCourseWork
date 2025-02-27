

class PatternComparator {
    /**
     * Сравнивает два паттерна на эквивалентность
     */
    fun areEquivalent(
        p1: Parser.Pattern,
        p2: Parser.Pattern,
        forwardMap: MutableMap<Pair<String, String>, String> = mutableMapOf(),
        reverseMap: MutableMap<Pair<String, String>, String> = mutableMapOf()
    ): Boolean {
        if (p1.elements.size != p2.elements.size) return false
        for ((elem1, elem2) in p1.elements.zip(p2.elements)) {
            if (!compareElement(elem1, elem2, forwardMap, reverseMap)) return false
        }
        return true
    }

    private fun compareElement(
        e1: Parser.PatternElement,
        e2: Parser.PatternElement,
        forwardMap: MutableMap<Pair<String, String>, String>,
        reverseMap: MutableMap<Pair<String, String>, String>
    ): Boolean {
        return when {
            e1 is Parser.PatternElement.PatternVariable && e2 is Parser.PatternElement.PatternVariable ->
                matchVariable(e1, e2, forwardMap, reverseMap)
            e1 is Parser.PatternElement.PatternLiteral && e2 is Parser.PatternElement.PatternLiteral ->
                e1.value == e2.value
            e1 is Parser.PatternElement.PatternNumber && e2 is Parser.PatternElement.PatternNumber ->
                e1.value == e2.value
            e1 is Parser.PatternElement.PatternStringVal && e2 is Parser.PatternElement.PatternStringVal ->
                e1.value == e2.value
            e1 is Parser.PatternElement.PatternParenStructure && e2 is Parser.PatternElement.PatternParenStructure ->
                areEquivalent(Parser.Pattern(e1.elements), Parser.Pattern(e2.elements), forwardMap, reverseMap)
            e1 is Parser.PatternElement.PatternSymbol && e2 is Parser.PatternElement.PatternSymbol ->
                e1.text == e2.text
            else -> false
        }
    }

    private fun matchVariable(
        v1: Parser.PatternElement.PatternVariable,
        v2: Parser.PatternElement.PatternVariable,
        forwardMap: MutableMap<Pair<String, String>, String>,
        reverseMap: MutableMap<Pair<String, String>, String>
    ): Boolean {
        if (v1.type != v2.type) return false

        val key1 = Pair(v1.type, v1.name)
        val key2 = Pair(v2.type, v2.name)

        val existForward = forwardMap[key1]
        val existReverse = reverseMap[key2]

        return when {
            existForward == null && existReverse == null -> {
                forwardMap[key1] = v2.name
                reverseMap[key2] = v1.name
                true
            }
            existForward == v2.name && existReverse == v1.name -> true
            else -> false
        }
    }

}