import Parser.*
import Parser.ResultElement.*

class FunctionExtractor {
    fun extractFunctions(program: Program): FunctionInfo {
        val functionBodies = mutableMapOf<String, List<Sentence>>()
        val callGraph = mutableMapOf<String, MutableSet<Pair<String, List<ResultElement>>>>()
        val callers = mutableMapOf<String, MutableSet<String>>()

        for (element in program.elements) {
            if (element is ProgramElement.FunctionDefinition) {
                functionBodies[element.name] = element.body
            }
        }

        for ((name, body) in functionBodies) {
            val calls = extractFunctionCalls(body)
            callGraph[name] = calls
            for ((calledFunc, _) in calls) {
                callers.computeIfAbsent(calledFunc) { mutableSetOf() }.add(name)
            }
        }

        return FunctionInfo(functionBodies, callGraph, callers)
    }

    private fun extractFunctionCalls(body: List<Sentence>): MutableSet<Pair<String, List<ResultElement>>> {
        val calls = mutableSetOf<Pair<String, List<ResultElement>>>()
        for (sentence in body) {
            for (element in sentence.result.elements) {
                collectFunctionCalls(element, calls)
            }
        }
        return calls
    }

    private fun collectFunctionCalls(
        element: ResultElement,
        calls: MutableSet<Pair<String, List<ResultElement>>>
    ) {
        when (element) {
            is ResultAngleStructure -> {
                calls.add(Pair(element.constructor, element.elements))
                element.elements.forEach { collectFunctionCalls(it, calls) }
            }
            is ResultParenStructure -> {
                element.elements.forEach { collectFunctionCalls(it, calls) }
            }
            else -> {}
        }
    }

    data class FunctionInfo(
        val functionBodies: Map<String, List<Sentence>>,
        val callGraph: Map<String, MutableSet<Pair<String, List<ResultElement>>>>,
        val callers: Map<String, MutableSet<String>>
    )
}
