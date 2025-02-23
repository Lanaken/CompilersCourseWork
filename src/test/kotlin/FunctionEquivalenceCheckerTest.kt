import main.kotlin.Parser.*
import main.kotlin.Parser.PatternElement.*
import main.kotlin.Parser.ResultElement.*
import main.kotlin.Parser.ResultElement.Literal as ResultLiteral
import main.kotlin.Parser.ResultElement.Variable as ResultVariable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FunctionEquivalenceCheckerTest {

    private val checker = FunctionEquivalenceChecker()

    @Test
    fun `functions F and G are equivalent`() {
        // Функция F:
        // Sentence 1: Pattern: ('B' e.x) → Result: (<F e.x>)
        // Sentence 2: Pattern: ('A' e.X2) → Result: (<G e.X2>)
        // Sentence 3: Pattern: (e.Z e.Z1 'C' e.Z) → Result: (e.Z)
        val fSentences = listOf(
            Sentence(
                Pattern(listOf(Literal("B"), Variable("e", "x"))),
                Result(listOf(AngleStructure("F", listOf(ResultVariable("e", "x")))))
            ),
            Sentence(
                Pattern(listOf(Literal("A"), Variable("e", "X2"))),
                Result(listOf(AngleStructure("G", listOf(ResultVariable("e", "X2")))))
            ),
            Sentence(
                Pattern(listOf(Variable("e", "Z"), Variable("e", "Z1"), Literal("C"), Variable("e", "Z"))),
                Result(listOf(ResultVariable("e", "Z")))
            )
        )
        val F = ProgramElement.FunctionDefinition("F", false, fSentences)

        // Функция G:
        // Sentence 1: Pattern: ('A' e.X) → Result: (<F e.X>)
        // Sentence 2: Pattern: ('B' e.X11) → Result: (<H e.X11>)
        // Sentence 3: Pattern: (e.5 e.Q 'C' e.5) → Result: (e.5)
        val gSentences = listOf(
            Sentence(
                Pattern(listOf(Literal("A"), Variable("e", "X"))),
                Result(listOf(AngleStructure("F", listOf(ResultVariable("e", "X")))))
            ),
            Sentence(
                Pattern(listOf(Literal("B"), Variable("e", "X11"))),
                Result(listOf(AngleStructure("H", listOf(ResultVariable("e", "X11")))))
            ),
            Sentence(
                Pattern(listOf(Variable("e", "5"), Variable("e", "Q"), Literal("C"), Variable("e", "5"))),
                Result(listOf(ResultVariable("e", "5")))
            )
        )
        val G = ProgramElement.FunctionDefinition("G", false, gSentences)

        // Пусть callGraph оставляем пустым для простоты.
        val functionBodies = mapOf("F" to F.body, "G" to G.body)
        // Для 0-бисимуляции имена вызываемых функций игнорируются,
        // поэтому можно передать группу с обоими именами.
        val bisimulationGroups = mutableListOf(mutableSetOf("F", "G"))
        val areEq = checker.areEquivalent("F", "G", functionBodies, level = 0, bisimulationGroups = bisimulationGroups)
        assertTrue(areEq, "Functions F and G should be equivalent")
    }

    @Test
    fun `functions with different number of sentences are not equivalent`() {
        val fSentences = listOf(
            Sentence(
                Pattern(listOf(Literal("A"), Variable("e", "x"))),
                Result(listOf(ResultVariable("e", "x")))
            )
        )
        val gSentences = listOf(
            Sentence(
                Pattern(listOf(Literal("A"), Variable("e", "x"))),
                Result(listOf(ResultVariable("e", "x")))
            ),
            Sentence(
                Pattern(listOf(Literal("B"))),
                Result(listOf(ResultLiteral("B")))
            )
        )
        val F = ProgramElement.FunctionDefinition("F", false, fSentences)
        val G = ProgramElement.FunctionDefinition("G", false, gSentences)

        val functionBodies = mapOf("F" to F.body, "G" to G.body)
        val bisimulationGroups = mutableListOf(mutableSetOf("F", "G"))
        val areEq = checker.areEquivalent("F", "G", functionBodies, level = 0, bisimulationGroups = bisimulationGroups)
        assertFalse(areEq, "Functions with different number of sentences should not be equivalent")
    }
}
