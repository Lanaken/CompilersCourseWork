import main.kotlin.Parser.*
import main.kotlin.Parser.PatternElement.*
import main.kotlin.Parser.ResultElement.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import main.kotlin.Parser.PatternElement.Literal as PatternLiteral

class FunctionExtractorTest {

    @Test
    fun `test single function with no calls`() {
        // Функция F: один единственный пункт: ('A') = ('B')
        val funcF = ProgramElement.FunctionDefinition(
            name = "F",
            isEntryPoint = false,
            body = listOf(
                Sentence(
                    pattern = Pattern(listOf(PatternLiteral("A"))),
                    result = Result(listOf(Literal("B")))
                )
            )
        )
        val program = Program(listOf(funcF))
        val extractor = FunctionExtractor()
        val functionInfo = extractor.extractFunctions(program)

        // Проверяем, что есть функция F и она содержит один пункт
        assertEquals(1, functionInfo.functionBodies.size)
        assertTrue(functionInfo.functionBodies.containsKey("F"))
        assertEquals(1, functionInfo.functionBodies["F"]!!.size)

        // Поскольку в правой части нет вызовов, callGraph и callers должны быть пустыми.
        assertTrue(functionInfo.callGraph["F"]?.isEmpty() ?: true)
        assertTrue(functionInfo.callers.isEmpty())
    }

    @Test
    fun `test two functions with a call`() {
        // Функция F: ('A') = <G 'B'>
        val funcF = ProgramElement.FunctionDefinition(
            name = "F",
            isEntryPoint = false,
            body = listOf(
                Sentence(
                    pattern = Pattern(listOf(PatternLiteral("A"))),
                    result = Result(
                        listOf(
                            AngleStructure("G", listOf(Literal("B")))
                        )
                    )
                )
            )
        )
        // Функция G: ('C') = ('D')
        val funcG = ProgramElement.FunctionDefinition(
            name = "G",
            isEntryPoint = false,
            body = listOf(
                Sentence(
                    pattern = Pattern(listOf(PatternLiteral("C"))),
                    result = Result(listOf(Literal("D")))
                )
            )
        )
        val program = Program(listOf(funcF, funcG))
        val extractor = FunctionExtractor()
        val functionInfo = extractor.extractFunctions(program)

        // Проверяем, что есть обе функции
        assertEquals(2, functionInfo.functionBodies.size)
        assertTrue(functionInfo.functionBodies.containsKey("F"))
        assertTrue(functionInfo.functionBodies.containsKey("G"))

        // Для функции F callGraph должна содержать один вызов: <G 'B'>
        val callsF = functionInfo.callGraph["F"]
        assertNotNull(callsF)
        assertEquals(1, callsF!!.size)
        val (calledFunc, args) = callsF.first()
        assertEquals("G", calledFunc)
        assertEquals(1, args.size)
        val arg0 = args[0]
        // Ожидаем, что аргумент – литерал "B"
        assertTrue(arg0 is Literal && (arg0 as Literal).value == "B")

        // Проверяем callers: функция G должна вызываться функцией F.
        val callersG = functionInfo.callers["G"]
        assertNotNull(callersG)
        assertEquals(1, callersG!!.size)
        assertTrue(callersG.contains("F"))
    }

    @Test
    fun `test nested calls extraction`() {
        // Функция F: ('A') = <G (<H 'X'>)>
        // Здесь ожидается, что из правой части извлекаются два вызова:
        // один вызов функции G с аргументом, содержащим вызов H.
        val funcF = ProgramElement.FunctionDefinition(
            name = "F",
            isEntryPoint = false,
            body = listOf(
                Sentence(
                    pattern = Pattern(listOf(PatternLiteral("A"))),
                    result = Result(
                        listOf(
                            AngleStructure("G", listOf(
                                ResultElement.ParenStructure(
                                    listOf(
                                        AngleStructure("H", listOf(Literal("X")))
                                    )
                                )
                            ))
                        )
                    )
                )
            )
        )
        // Функция G: ('B') = ('C')
        val funcG = ProgramElement.FunctionDefinition(
            name = "G",
            isEntryPoint = false,
            body = listOf(
                Sentence(
                    pattern = Pattern(listOf(PatternLiteral("B"))),
                    result = Result(listOf(Literal("C")))
                )
            )
        )
        // Функция H: ('D') = ('E')
        val funcH = ProgramElement.FunctionDefinition(
            name = "H",
            isEntryPoint = false,
            body = listOf(
                Sentence(
                    pattern = Pattern(listOf(PatternLiteral("D"))),
                    result = Result(listOf(Literal("E")))
                )
            )
        )
        val program = Program(listOf(funcF, funcG, funcH))
        val extractor = FunctionExtractor()
        val functionInfo = extractor.extractFunctions(program)

        // Для функции F должны быть извлечены вызовы:
        // один вызов к G и вложенный вызов к H.
        val callsF = functionInfo.callGraph["F"]
        assertNotNull(callsF)
        // Ожидается, что будет 2 вызова (один для <G ...> и один вложенный внутри аргументов)
        assertEquals(2, callsF!!.size)

        // Проверяем, что callers для H содержат "F" (из вложенного вызова)
        val callersH = functionInfo.callers["H"]
        assertNotNull(callersH)
        assertTrue(callersH!!.contains("F"))
    }
}
