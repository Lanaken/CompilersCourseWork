import Parser.ResultElement.ResultAngleStructure
import Parser.ResultElement.ResultLiteral
import Parser.ResultElement.ResultNumber
import Parser.ResultElement.ResultParenStructure
import Parser.ResultElement.ResultStringVal
import Parser.ResultElement.ResultSymbol
import Parser.ResultElement.ResultVariable

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
            e1 is ResultVariable && e2 is ResultVariable ->
                matchVariable(e1, e2, forwardMap, reverseMap)

            e1 is ResultLiteral && e2 is ResultLiteral ->
                e1.value == e2.value

            e1 is ResultNumber && e2 is ResultNumber ->
                e1.value == e2.value

            e1 is ResultStringVal && e2 is ResultStringVal ->
                e1.value == e2.value

            e1 is ResultAngleStructure && e2 is ResultAngleStructure -> {
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

            e1 is ResultParenStructure && e2 is ResultParenStructure ->
                areEquivalent(
                    Parser.Result(e1.elements),
                    Parser.Result(e2.elements),
                    forwardMap,
                    reverseMap,
                    bisimulationGroups,
                    level
                )

            e1 is ResultSymbol && e2 is ResultSymbol ->
                e1.text == e2.text

            else -> false
        }
    }

    private fun matchVariable(
        v1: ResultVariable,
        v2: ResultVariable,
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
