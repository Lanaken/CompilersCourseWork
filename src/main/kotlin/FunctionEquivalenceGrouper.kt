import main.kotlin.Parser

class FunctionEquivalenceGrouper(
    private val equivalenceChecker: FunctionEquivalenceChecker = FunctionEquivalenceChecker()
) {
    /**
     * Группирует функции по их эквивалентности.
     *
     * @param functionBodies отображение: имя функции → список предложений (каркасный AST)
     * @return Отображение канонического имени группы к множеству имен функций, принадлежащих этой группе.
     */
    fun groupFunctions(
        functionBodies: Map<String, List<Parser.Sentence>>
    ): Map<String, Set<String>> {
        // Изначально все функции в одной группе.
        var groups: List<Set<String>> = mutableListOf(functionBodies.keys.toMutableSet())

        var level = 0
        var changed: Boolean

        do {
            changed = false
            val newGroups = partitionGroup(groups, functionBodies, level)
            if (groups != newGroups) {
                changed = true
                groups = newGroups
            }
            level++
        } while (changed)

        // Для каждой группы выбираем каноническое имя (лексикографически минимальное).
        return groups.associateBy(
            keySelector = { it.minOrNull()!! },
            valueTransform = { it.toSet() }
        )
    }

    /**
     * Разбиение группы функций на подгруппы на текущем уровне сравнения.
     * @param bisimulationGroups группа функций
     * @param functionBodies тела функций
     * @param level уровень сравнения
     */
    private fun partitionGroup(
        bisimulationGroups: List<Set<String>>,
        functionBodies: Map<String, List<Parser.Sentence>>,
        level: Int
    ): List<Set<String>> {
        val newGroups = mutableListOf<Set<String>>()

        for (group in bisimulationGroups) {
            if (group.size == 1) {
                newGroups.add(group)
                continue
            }
            val calculatedGroups = calculateNewGroups(functionBodies, level, group, bisimulationGroups)
            newGroups.addAll(calculatedGroups)
        }

        return newGroups
    }

    private fun calculateNewGroups(
        functionBodies: Map<String, List<Parser.Sentence>>,
        level: Int,
        currentGroup: Set<String>,
        bisimulationGroups: List<Set<String>>
    ): List<Set<String>> {
        if (level == 1)
            println()
        val newGroups = mutableListOf<Set<String>>()
        for (i in currentGroup.indices) {
            var newGroup: Set<String> = newGroups.firstOrNull { it.contains(currentGroup.elementAt(i)) } ?: setOf()
            if (newGroup.isNotEmpty()) {
                newGroup = calculateIfFunctionIsInNewGroup(
                    functionBodies,
                    level,
                    currentGroup.toMutableSet(),
                    bisimulationGroups,
                    newGroups,
                    newGroup.toMutableSet(),
                    i
                )
                val index = newGroups.indexOfFirst { it == newGroup }
                newGroups[index] = newGroup
            }
            else {
                newGroup = 
                    calculateIfFunctionIsNotInNewGroup(functionBodies, level, currentGroup.toMutableSet(), bisimulationGroups, i)
                newGroups.add(newGroup)
            }
        }
        return newGroups
    }

    private fun calculateIfFunctionIsInNewGroup(
        functionBodies: Map<String, List<Parser.Sentence>>,
        level: Int,
        currentGroup: MutableSet<String>,
        bisimulationGroups: List<Set<String>>,
        newGroups: MutableList<Set<String>>,
        newGroup: MutableSet<String>,
        functionIndex: Int
    ): Set<String> {
        for (j in functionIndex + 1..<currentGroup.size) {
            if (checkIfAlreadyInGroup(newGroups, currentGroup.elementAt(j)))
                continue
            val f1 = currentGroup.elementAt(functionIndex)
            val f2 = currentGroup.elementAt(j)
            if (equivalenceChecker.areEquivalent(f1, f2, functionBodies, level, bisimulationGroups))
                newGroup.add(f2)
        }
        return newGroup
    }

    private fun calculateIfFunctionIsNotInNewGroup(
        functionBodies: Map<String, List<Parser.Sentence>>,
        level: Int,
        currentGroup: MutableSet<String>,
        bisimulationGroups: List<Set<String>>,
        functionIndex: Int
    ): Set<String> {
        val f1 = currentGroup.elementAt(functionIndex)
        val newGroup = mutableSetOf(f1)

        for (j in functionIndex + 1..<currentGroup.size) {
            val f2 = currentGroup.elementAt(j)
            if (equivalenceChecker.areEquivalent(f1, f2, functionBodies, level, bisimulationGroups))
                newGroup.add(f2)
        }
        return newGroup
    }

    private fun checkIfAlreadyInGroup(newGroups: MutableList<Set<String>>, functionName: String): Boolean =
        newGroups.any { it.contains(functionName) }
}
