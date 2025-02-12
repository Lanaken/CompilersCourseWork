object AstTreePrinter {

    fun printAst(program: Parser.Program, indent: String = ""): String {
        val builder = StringBuilder()
        builder.appendLine("${indent}Program:")
        program.elements.forEach { element ->
            builder.append(printProgramElement(element, "$indent  "))
        }
        return builder.toString()
    }

    private fun printProgramElement(element: Parser.ProgramElement, indent: String): String {
        return when (element) {
            is Parser.ProgramElement.ExternDeclaration ->
                printExternDeclaration(element, indent)
            is Parser.ProgramElement.FunctionDefinition ->
                printFunctionDefinition(element, indent)
        }
    }

    private fun printExternDeclaration(decl: Parser.ProgramElement.ExternDeclaration, indent: String): String {
        val builder = StringBuilder()
        builder.appendLine("${indent}ExternDeclaration:")
        decl.names.forEach { name ->
            builder.appendLine("$indent  Name: $name")
        }
        return builder.toString()
    }

    private fun printFunctionDefinition(func: Parser.ProgramElement.FunctionDefinition, indent: String): String {
        val builder = StringBuilder()
        builder.appendLine("${indent}FunctionDefinition: ${func.name} (EntryPoint: ${func.isEntryPoint})")
        func.body.forEach { sentence ->
            builder.append(printSentence(sentence, "$indent  "))
        }
        return builder.toString()
    }

    private fun printSentence(sentence: Parser.Sentence, indent: String): String {
        val builder = StringBuilder()
        builder.appendLine("${indent}Sentence:")
        builder.appendLine("$indent  Pattern:")
        builder.append(printPattern(sentence.pattern, "$indent    "))
        builder.appendLine("$indent  Result:")
        builder.append(printResult(sentence.result, "$indent    "))
        return builder.toString()
    }

    private fun printPattern(pattern: Parser.Pattern, indent: String): String {
        val builder = StringBuilder()
        pattern.elements.forEach { element ->
            builder.append(printPatternElement(element, indent))
        }
        return builder.toString()
    }

    private fun printPatternElement(element: Parser.PatternElement, indent: String): String {
        return when (element) {
            is Parser.PatternElement.Variable ->
                "$indent Variable(${element.type}): ${element.name}\n"
            is Parser.PatternElement.Literal ->
                "$indent Literal: ${element.value}\n"
            is Parser.PatternElement.Number ->
                "$indent Number: ${element.value}\n"
            is Parser.PatternElement.StringVal ->
                "$indent StringVal: ${element.value}\n"
            is Parser.PatternElement.ParenStructure -> {
                val builder = StringBuilder()
                builder.appendLine("$indent ParenStructure:")
                element.elements.forEach { subElem ->
                    builder.append(printPatternElement(subElem, "$indent  "))
                }
                builder.toString()
            }
            is Parser.PatternElement.Symbol ->
                "$indent Symbol: ${element.text}\n"
        }
    }


    private fun printResult(result: Parser.Result, indent: String): String {
        val builder = StringBuilder()
        result.elements.forEach { element ->
            builder.append(printResultElement(element, indent))
        }
        return builder.toString()
    }

    private fun printResultElement(element: Parser.ResultElement, indent: String): String {
        return when (element) {
            is Parser.ResultElement.Variable ->
                "$indent Variable(${element.type}): ${element.name}\n"
            is Parser.ResultElement.Literal ->
                "$indent Literal: ${element.value}\n"
            is Parser.ResultElement.Number ->
                "$indent Number: ${element.value}\n"
            is Parser.ResultElement.StringVal ->
                "$indent StringVal: ${element.value}\n"
            is Parser.ResultElement.AngleStructure -> {
                val builder = StringBuilder()
                builder.appendLine("$indent AngleStructure(constructor='${element.constructor}')")
                element.elements.forEach { subElem ->
                    builder.append(printResultElement(subElem, "$indent  "))
                }
                builder.toString()
            }
            is Parser.ResultElement.ParenStructure -> {
                val builder = StringBuilder()
                builder.appendLine("$indent ParenStructure:")
                element.elements.forEach { subElem ->
                    builder.append(printResultElement(subElem, "$indent  "))
                }
                builder.toString()
            }
            is Parser.ResultElement.Symbol ->
                "$indent Symbol: ${element.text}\n"
        }
    }

}
