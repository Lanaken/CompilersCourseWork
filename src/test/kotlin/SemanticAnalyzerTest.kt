import Parser.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import Parser.ProgramElement.FunctionDefinition

class SemanticAnalyzerTest {

    private val analyzer = SemanticAnalyzer()

    @Test
    fun `no errors when pattern and result have same variables`() {
        // Функция F, правило:
        // Паттерн: (e.x, 'A')
        // Результат: (e.x, 'A')
        val pattern = Pattern(
            listOf(
                PatternElement.PatternVariable("e", "x"),
                PatternElement.PatternLiteral("A")
            )
        )
        val result = Result(
            listOf(
                ResultElement.ResultVariable("e", "x"),
                ResultElement.ResultLiteral("A")
            )
        )
        val sentence = Sentence(pattern, result)
        val func = FunctionDefinition("F", false, listOf(sentence))
        val program = Program(listOf(func))

        val errors = analyzer.analyze(program)
        // Ожидается, что ошибок нет, так как переменная e.x присутствует в обеих частях
        assertTrue(errors.isEmpty(), "Ошибок не должно быть, все переменные из паттерна присутствуют в результате")
    }

    @Test
    fun `error when result contains variable not declared in pattern`() {
        // Функция F, правило:
        // Паттерн: ('A')
        // Результат: ('A', e.x)
        val pattern = Pattern(
            listOf(
                PatternElement.PatternLiteral("A")
            )
        )
        val result = Result(
            listOf(
                ResultElement.ResultLiteral("A"),
                ResultElement.ResultVariable("e", "x")
            )
        )
        val sentence = Sentence(pattern, result)
        val func = FunctionDefinition("F", false, listOf(sentence))
        val program = Program(listOf(func))

        val errors = analyzer.analyze(program)
        // Ожидается, что обнаружится ошибка, т.к. переменная e.x присутствует в правой части, но не объявлена в паттерне
        assertEquals(1, errors.size, "Должна быть обнаружена ошибка: в результате присутствует переменная e.x, отсутствующая в паттерне")
        assertTrue(errors[0].contains("e.x"), "Сообщение об ошибке должно содержать 'e.x'")
    }

    @Test
    fun `no errors for nested structures when all variables are declared`() {
        // Функция F, правило:
        // Паттерн: ((e.x, 'B'), 'C')
        // Результат: ((e.x, 'B'), 'C')
        val pattern = Pattern(
            listOf(
                PatternElement.PatternParenStructure(
                    listOf(
                        PatternElement.PatternVariable("e", "x"),
                        PatternElement.PatternLiteral("B")
                    )
                ),
                PatternElement.PatternLiteral("C")
            )
        )
        val result = Result(
            listOf(
                ResultElement.ResultParenStructure(
                    listOf(
                        ResultElement.ResultVariable("e", "x"),
                        ResultElement.ResultLiteral("B")
                    )
                ),
                ResultElement.ResultLiteral("C")
            )
        )
        val sentence = Sentence(pattern, result)
        val func = FunctionDefinition("F", false, listOf(sentence))
        val program = Program(listOf(func))

        val errors = analyzer.analyze(program)
        // Ожидается отсутствие ошибок, так как переменная e.x присутствует во вложенной структуре и в целом
        assertTrue(errors.isEmpty(), "Ошибок не должно быть – переменная e.x присутствует в обеих частях, даже во вложенной структуре")
    }

    @Test
    fun `error for nested structure when variable is missing`() {
        // Функция F, правило:
        // Паттерн: (('B'), 'C')
        // Результат: ((e.x, 'B'), 'C') – переменная e.x отсутствует во вложенной структуре
        val pattern = Pattern(
            listOf(
                PatternElement.PatternParenStructure(
                    listOf(
                        PatternElement.PatternLiteral("B")
                    )
                ),
                PatternElement.PatternLiteral("C")
            )
        )
        val result = Result(
            listOf(
                ResultElement.ResultParenStructure(
                    listOf(
                        ResultElement.ResultVariable("e", "x"),
                        ResultElement.ResultLiteral("B")
                    )
                ),
                ResultElement.ResultLiteral("C")
            )
        )
        val sentence = Sentence(pattern, result)
        val func = FunctionDefinition("F", false, listOf(sentence))
        val program = Program(listOf(func))

        val errors = analyzer.analyze(program)
        assertEquals(1, errors.size, "Ожидается одна ошибка – переменная e.x отсутствует во вложенной структуре")
        assertTrue(errors[0].contains("e.x"), "Сообщение об ошибке должно содержать 'e.x'")
    }

    @Test
    fun `multiple sentences, one missing variable should produce error`() {
        // Функция F с двумя предложениями.
        // Предложение 1: Паттерн: (e.x, 'A') → Результат: (e.x, 'A') (корректно)
        // Предложение 2: Паттерн: ('B') → Результат: (e.y 'B') (ошибка, e.y отсутствует)
        val pattern1 = Pattern(
            listOf(
                PatternElement.PatternVariable("e", "x"),
                PatternElement.PatternLiteral("A")
            )
        )
        val result1 = Result(
            listOf(
                ResultElement.ResultVariable("e", "x"),
                ResultElement.ResultLiteral("A")
            )
        )
        val sentence1 = Sentence(pattern1, result1)

        val pattern2 = Pattern(
            listOf(
                PatternElement.PatternLiteral("B")
            )
        )
        val result2 = Result(
            listOf(
                ResultElement.ResultVariable("e", "y"),
                ResultElement.ResultLiteral("B")
            )
        )
        val sentence2 = Sentence(pattern2, result2)

        val func = FunctionDefinition("F", false, listOf(sentence1, sentence2))
        val program = Program(listOf(func))

        val errors = analyzer.analyze(program)
        assertEquals(1, errors.size, "Ожидается одна ошибка, так как во втором предложении отсутствует переменная e.y")
        assertTrue(errors[0].contains("e.y"), "Сообщение об ошибке должно содержать 'e.y'")
    }
}
