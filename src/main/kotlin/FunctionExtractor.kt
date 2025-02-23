import main.kotlin.Parser

class FunctionExtractor {
    fun extractFunctions(program: Parser.Program): FunctionInfo {
        val functionBodies = mutableMapOf<String, List<Parser.Sentence>>()
        val callGraph = mutableMapOf<String, MutableSet<Pair<String, List<Parser.ResultElement>>>>()
        val callers = mutableMapOf<String, MutableSet<String>>()

        for (element in program.elements) {
            if (element is Parser.ProgramElement.FunctionDefinition) {
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

    private fun extractFunctionCalls(body: List<Parser.Sentence>): MutableSet<Pair<String, List<Parser.ResultElement>>> {
        val calls = mutableSetOf<Pair<String, List<Parser.ResultElement>>>()
        for (sentence in body) {
            for (element in sentence.result.elements) {
                collectFunctionCalls(element, calls)
            }
        }
        return calls
    }

    private fun collectFunctionCalls(
        element: Parser.ResultElement,
        calls: MutableSet<Pair<String, List<Parser.ResultElement>>>
    ) {
        when (element) {
            is Parser.ResultElement.AngleStructure -> {
                calls.add(Pair(element.constructor, element.elements))
                element.elements.forEach { collectFunctionCalls(it, calls) }
            }
            is Parser.ResultElement.ParenStructure -> {
                element.elements.forEach { collectFunctionCalls(it, calls) }
            }
            else -> {}
        }
    }

    data class FunctionInfo(
        val functionBodies: Map<String, List<Parser.Sentence>>,
        val callGraph: Map<String, MutableSet<Pair<String, List<Parser.ResultElement>>>>,
        val callers: Map<String, MutableSet<String>>
    )
}
