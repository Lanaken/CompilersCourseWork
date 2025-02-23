import main.kotlin.Parser
import main.kotlin.TokenType.ENTRY
import main.kotlin.TokenType.EXTERN

object RefalCodeGenerator {
    fun generate(program: Parser.Program): String {
        val builder = StringBuilder()

        for (element in program.elements) {
            when (element) {
                is Parser.ProgramElement.ExternDeclaration -> {
                    builder.append("$EXTERN ")
                    builder.append(element.names.joinToString(", "))
                    builder.append(";\n\n")
                }
                is Parser.ProgramElement.FunctionDefinition -> {
                    if (element.isEntryPoint) {
                        builder.append("$ENTRY Go\n")
                    }
                    builder.append("${element.name} {\n")

                    for (sentence in element.body) {
                        builder.append("  ")
                        builder.append(generatePattern(sentence.pattern))
                        builder.append(" = ")
                        builder.append(generateResult(sentence.result))
                        builder.append(";\n")
                    }

                    builder.append("}\n\n")
                }
            }
        }

        return builder.toString()
    }

    private fun generatePattern(pattern: Parser.Pattern): String  = pattern.elements.joinToString(" ") { generatePatternElement(it) }

    private fun generatePatternElement(element: Parser.PatternElement): String {
        return when (element) {
            is Parser.PatternElement.Variable -> element.name
            is Parser.PatternElement.Literal -> element.value
            is Parser.PatternElement.ParenStructure -> "(${element.elements.joinToString(" ") { generatePatternElement(it) }})"
            is Parser.PatternElement.Symbol -> element.text
            is Parser.PatternElement.Number -> element.value.toString()
            is Parser.PatternElement.StringVal -> element.value
        }
    }

    private fun generateResult(result: Parser.Result): String = result.elements.joinToString(" ") { generateResultElement(it) }


    private fun generateResultElement(element: Parser.ResultElement): String {
        return when (element) {
            is Parser.ResultElement.Variable -> element.name
            is Parser.ResultElement.Literal -> element.value
            is Parser.ResultElement.AngleStructure -> "<${element.constructor} ${element.elements.joinToString(" ") { generateResultElement(it) }}>"
            is Parser.ResultElement.ParenStructure -> "(${element.elements.joinToString(" ") { generateResultElement(it) }})"
            is Parser.ResultElement.Symbol -> element.text
            is Parser.ResultElement.Number -> element.value.toString()
            is Parser.ResultElement.StringVal -> element.value
        }
    }
}
