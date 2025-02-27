import Parser.*
import Parser.ResultElement.*
import Parser.PatternElement.*


object AstTreePrinter {

    fun printAst(program: Program, indent: String = ""): String {
        val builder = StringBuilder()
        builder.appendLine("${indent}Program:")
        program.elements.forEach { element ->
            builder.append(printProgramElement(element, "$indent  "))
        }
        return builder.toString()
    }

    private fun printProgramElement(element: ProgramElement, indent: String): String {
        return when (element) {
            is ProgramElement.ExternDeclaration ->
                printExternDeclaration(element, indent)
            is ProgramElement.FunctionDefinition ->
                printFunctionDefinition(element, indent)
        }
    }

    private fun printExternDeclaration(decl: ProgramElement.ExternDeclaration, indent: String): String {
        val builder = StringBuilder()
        builder.appendLine("${indent}ExternDeclaration:")
        decl.names.forEach { name ->
            builder.appendLine("$indent  Name: $name")
        }
        return builder.toString()
    }

    private fun printFunctionDefinition(func: ProgramElement.FunctionDefinition, indent: String): String {
        val builder = StringBuilder()
        builder.appendLine("${indent}FunctionDefinition: ${func.name} (EntryPoint: ${func.isEntryPoint})")
        func.body.forEach { sentence ->
            builder.append(printSentence(sentence, "$indent  "))
        }
        return builder.toString()
    }

    private fun printSentence(sentence: Sentence, indent: String): String {
        val builder = StringBuilder()
        builder.appendLine("${indent}Sentence:")
        builder.appendLine("$indent  Pattern:")
        builder.append(printPattern(sentence.pattern, "$indent    "))
        builder.appendLine("$indent  Result:")
        builder.append(printResult(sentence.result, "$indent    "))
        return builder.toString()
    }

    private fun printPattern(pattern: Pattern, indent: String): String {
        val builder = StringBuilder()
        pattern.elements.forEach { element ->
            builder.append(printPatternElement(element, indent))
        }
        return builder.toString()
    }

    private fun printPatternElement(element: PatternElement, indent: String): String {
        return when (element) {
            is PatternVariable ->
                "$indent Variable(${element.type}): ${element.name}\n"
            is PatternLiteral ->
                "$indent Literal: ${element.value}\n"
            is PatternNumber ->
                "$indent Number: ${element.value}\n"
            is PatternStringVal ->
                "$indent StringVal: ${element.value}\n"
            is PatternParenStructure -> {
                val builder = StringBuilder()
                builder.appendLine("$indent ParenStructure:")
                element.elements.forEach { subElem ->
                    builder.append(printPatternElement(subElem, "$indent  "))
                }
                builder.toString()
            }
            is PatternSymbol ->
                "$indent Symbol: ${element.text}\n"
        }
    }


    private fun printResult(result: Result, indent: String): String {
        val builder = StringBuilder()
        result.elements.forEach { element ->
            builder.append(printResultElement(element, indent))
        }
        return builder.toString()
    }

    private fun printResultElement(element: ResultElement, indent: String): String {
        return when (element) {
            is ResultVariable ->
                "$indent Variable(${element.type}): ${element.name}\n"
            is ResultLiteral ->
                "$indent Literal: ${element.value}\n"
            is ResultNumber ->
                "$indent Number: ${element.value}\n"
            is ResultStringVal ->
                "$indent StringVal: ${element.value}\n"
            is ResultAngleStructure -> {
                val builder = StringBuilder()
                builder.appendLine("$indent AngleStructure(constructor='${element.constructor}')")
                element.elements.forEach { subElem ->
                    builder.append(printResultElement(subElem, "$indent  "))
                }
                builder.toString()
            }
            is ResultParenStructure -> {
                val builder = StringBuilder()
                builder.appendLine("$indent ParenStructure:")
                element.elements.forEach { subElem ->
                    builder.append(printResultElement(subElem, "$indent  "))
                }
                builder.toString()
            }
            is ResultSymbol ->
                "$indent Symbol: ${element.text}\n"
        }
    }

}
