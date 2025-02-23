import main.kotlin.Parser

class ResultComparator {
    /**
     * Сравнивает два результата (правые части) на эквивалентность.
     */
    fun areEquivalent(
        r1: Parser.Result,
        r2: Parser.Result,
        forwardMap: MutableMap<Pair<String, String>, String> = mutableMapOf(),
        reverseMap: MutableMap<Pair<String, String>, String> = mutableMapOf(),
        bisimulationGroups: List<Set<String>>,
        level: Int
    ): Boolean {
        if (r1.elements.size != r2.elements.size) return false
        for ((elem1, elem2) in r1.elements.zip(r2.elements)) {
            if (!compareElement(elem1, elem2, forwardMap, reverseMap, bisimulationGroups, level))
                return false
        }
        return true
    }

    private fun compareElement(
        e1: Parser.ResultElement,
        e2: Parser.ResultElement,
        forwardMap: MutableMap<Pair<String, String>, String>,
        reverseMap: MutableMap<Pair<String, String>, String>,
        bisimulationGroups: List<Set<String>>,
        level: Int
    ): Boolean {
        return when {
            e1 is Parser.ResultElement.Variable && e2 is Parser.ResultElement.Variable ->
                matchVariable(e1, e2, forwardMap, reverseMap)

            e1 is Parser.ResultElement.Literal && e2 is Parser.ResultElement.Literal ->
                e1.value == e2.value

            e1 is Parser.ResultElement.Number && e2 is Parser.ResultElement.Number ->
                e1.value == e2.value

            e1 is Parser.ResultElement.StringVal && e2 is Parser.ResultElement.StringVal ->
                e1.value == e2.value

            e1 is Parser.ResultElement.AngleStructure && e2 is Parser.ResultElement.AngleStructure -> {
                val firstFunctionGroup = bisimulationGroups.indexOfFirst { it.contains(e1.constructor) }
                val secondFunctionGroup = bisimulationGroups.indexOfFirst { it.contains(e2.constructor) }
                if (level > 0)
                    return firstFunctionGroup == secondFunctionGroup

                areEquivalent(
                    Parser.Result(e1.elements),
                    Parser.Result(e2.elements),
                    forwardMap,
                    reverseMap,
                    bisimulationGroups,
                    level
                )
            }

            e1 is Parser.ResultElement.ParenStructure && e2 is Parser.ResultElement.ParenStructure ->
                areEquivalent(
                    Parser.Result(e1.elements),
                    Parser.Result(e2.elements),
                    forwardMap,
                    reverseMap,
                    bisimulationGroups,
                    level
                )

            e1 is Parser.ResultElement.Symbol && e2 is Parser.ResultElement.Symbol ->
                e1.text == e2.text

            else -> false
        }
    }

    private fun matchVariable(
        v1: Parser.ResultElement.Variable,
        v2: Parser.ResultElement.Variable,
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
