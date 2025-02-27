import Parser.Pattern
import Parser.PatternElement.PatternParenStructure
import Parser.PatternElement.PatternVariable
import Parser.Program
import Parser.ProgramElement
import Parser.Result
import Parser.ResultElement.ResultAngleStructure
import Parser.ResultElement.ResultParenStructure
import Parser.ResultElement.ResultVariable

/**
 * Этот класс выполняет семантический анализ функции:
 * для каждого предложения (rule) проверяет, что все переменные,
 * объявленные в левой части (pattern), присутствуют в правой части (result).
 * Если в правой части появляется переменная, которая не была объявлена в левой,
 * это считается ошибкой.
 */
class SemanticAnalyzer {

    /**
     * Анализирует семантику программы.
     *
     * @param program AST программы
     * @return список сообщений об ошибках, если найдены предложения,
     *         где в правой части встречается переменная, отсутствующая в паттерне.
     */
    fun analyze(program: Program): List<String> {
        val errors = mutableListOf<String>()
        for (element in program.elements) {
            if (element is ProgramElement.FunctionDefinition) {
                for ((index, sentence) in element.body.withIndex()) {
                    val patternVars = getPatternVariables(sentence.pattern)
                    val resultVars = getResultVariables(sentence.result)
                    // Если в правой части есть переменная, которой нет в левой, то это ошибка.
                    for (varName in resultVars) {
                        if (!patternVars.contains(varName)) {
                            errors.add(
                                "В функции '${element.name}', правило #${index + 1}: " +
                                        "переменная '$varName' присутствует в результате, но не объявлена в паттерне."
                            )
                        }
                    }
                }
            }
        }
        return errors
    }

    private fun getPatternVariables(pattern: Pattern): Set<String> {
        val vars = mutableSetOf<String>()
        for (element in pattern.elements) {
            when (element) {
                is PatternVariable -> vars.add("${element.type}.${element.name}")
                is PatternParenStructure -> vars.addAll(getPatternVariables(Pattern(element.elements)))
                // Остальные элементы не содержат переменных.
                else -> {}
            }
        }
        return vars
    }

    // Собираем все переменные, встречающиеся в результате (рекурсивно).
    private fun getResultVariables(result: Result): Set<String> {
        val vars = mutableSetOf<String>()
        for (element in result.elements) {
            when (element) {
                is ResultVariable -> vars.add("${element.type}.${element.name}")
                is ResultParenStructure -> vars.addAll(getResultVariables(Result(element.elements)))
                is ResultAngleStructure -> vars.addAll(getResultVariables(Result(element.elements)))
                else -> {} // Литералы, числа, символы
            }
        }
        return vars
    }
}
