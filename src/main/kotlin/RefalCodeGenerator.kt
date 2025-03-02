import main.kotlin.TokenType.ENTRY
import main.kotlin.TokenType.EXTERN
import Parser.Pattern
import Parser.PatternElement
import Parser.PatternElement.PatternLiteral
import Parser.PatternElement.PatternNumber
import Parser.PatternElement.PatternParenStructure
import Parser.PatternElement.PatternStringVal
import Parser.PatternElement.PatternSymbol
import Parser.PatternElement.PatternVariable
import Parser.ResultElement.*

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
                else -> {}
            }
        }

        return builder.toString()
    }

    private fun generatePattern(pattern: Pattern): String  = pattern.elements.joinToString(" ") { generatePatternElement(it) }

    private fun generatePatternElement(element: PatternElement): String {
        return when (element) {
            is PatternVariable -> element.name
            is PatternLiteral -> element.value
            is PatternParenStructure -> "(${element.elements.joinToString(" ") { generatePatternElement(it) }})"
            is PatternSymbol -> element.text
            is PatternNumber -> element.value.toString()
            is PatternStringVal -> element.value
        }
    }

    private fun generateResult(result: Parser.Result): String = result.elements.joinToString(" ") { generateResultElement(it) }


    private fun generateResultElement(element: Parser.ResultElement): String {
        return when (element) {
            is ResultVariable -> element.name
            is ResultLiteral -> element.value
            is ResultAngleStructure -> "<${element.constructor} ${element.elements.joinToString(" ") { generateResultElement(it) }}>"
            is ResultParenStructure -> "(${element.elements.joinToString(" ") { generateResultElement(it) }})"
            is ResultSymbol -> element.text
            is ResultNumber -> element.value.toString()
            is ResultStringVal -> element.value
        }
    }
}
