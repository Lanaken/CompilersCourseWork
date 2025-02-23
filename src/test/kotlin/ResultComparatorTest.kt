import main.kotlin.Parser
import main.kotlin.Parser.ResultElement
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ResultComparatorTest {

    private val comparator = ResultComparator()

    @Test
    fun `should be equivalent when variables are renamed with same type`() {
        val result1 = Parser.Result(
            listOf(
                ResultElement.Literal("A"),
                ResultElement.Variable("e", "x")
            )
        )
        val result2 = Parser.Result(
            listOf(
                ResultElement.Literal("A"),
                ResultElement.Variable("e", "y")
            )
        )
        val areEq = comparator.areEquivalent(result1, result2,
            forwardMap = mutableMapOf(),
            reverseMap = mutableMapOf(),
            bisimulationGroups = mutableListOf(mutableSetOf()),
            level = 0)
        assertTrue(areEq, "Результаты с переименованными переменными должны быть эквивалентны")
    }

    @Test
    fun `should not be equivalent when literals have different values`() {
        val result1 = Parser.Result(
            listOf(
                ResultElement.Literal("A"),
                ResultElement.Variable("e", "x")
            )
        )
        val result2 = Parser.Result(
            listOf(
                ResultElement.Literal("B"),
                ResultElement.Variable("e", "x")
            )
        )
        val areEq = comparator.areEquivalent(result1, result2,
            forwardMap = mutableMapOf(),
            reverseMap = mutableMapOf(),
            bisimulationGroups = mutableListOf(mutableSetOf()),
            level = 0)
        assertFalse(areEq, "Результаты с разными литералами должны быть не эквивалентны")
    }

    @Test
    fun `should be equivalent when nested angle structures with renamed variables`() {
        val result1 = Parser.Result(
            listOf(
                ResultElement.AngleStructure("F", listOf(
                    ResultElement.ParenStructure(
                        listOf(
                            ResultElement.Literal("A"),
                            ResultElement.Variable("e", "x")
                        )
                    ),
                    ResultElement.Literal("B")
                ))
            )
        )
        val result2 = Parser.Result(
            listOf(
                ResultElement.AngleStructure("F", listOf(
                    ResultElement.ParenStructure(
                        listOf(
                            ResultElement.Literal("A"),
                            ResultElement.Variable("e", "y")
                        )
                    ),
                    ResultElement.Literal("B")
                ))
            )
        )
        val areEq = comparator.areEquivalent(result1, result2,
            forwardMap = mutableMapOf(),
            reverseMap = mutableMapOf(),
            bisimulationGroups = mutableListOf(mutableSetOf("F")),
            level = 1)
        assertTrue(areEq, "Вложенные вызовы с переименованными переменными должны считаться эквивалентными")
    }

    @Test
    fun `should not be equivalent when order of elements differs`() {
        val result1 = Parser.Result(
            listOf(
                ResultElement.Literal("A"),
                ResultElement.Variable("e", "x")
            )
        )
        val result2 = Parser.Result(
            listOf(
                ResultElement.Variable("e", "x"),
                ResultElement.Literal("A")
            )
        )
        val areEq = comparator.areEquivalent(result1, result2,
            forwardMap = mutableMapOf(),
            reverseMap = mutableMapOf(),
            bisimulationGroups = mutableListOf(mutableSetOf()),
            level = 0)
        assertFalse(areEq, "Порядок элементов важен, результаты с разным порядком не эквивалентны")
    }

    @Test
    fun `should be equivalent when deeply nested result with renamed variables`() {
        val result1 = Parser.Result(
            listOf(
                ResultElement.AngleStructure("F", listOf(
                    ResultElement.ParenStructure(
                        listOf(
                            ResultElement.Literal("A"),
                            ResultElement.Variable("e", "x")
                        )
                    ),
                    ResultElement.AngleStructure("G", listOf(
                        ResultElement.Literal("B")
                    ))
                ))
            )
        )
        val result2 = Parser.Result(
            listOf(
                ResultElement.AngleStructure("F", listOf(
                    ResultElement.ParenStructure(
                        listOf(
                            ResultElement.Literal("A"),
                            ResultElement.Variable("e", "y")
                        )
                    ),
                    ResultElement.AngleStructure("G", listOf(
                        ResultElement.Literal("B")
                    ))
                ))
            )
        )
        val areEq = comparator.areEquivalent(result1, result2,
            forwardMap = mutableMapOf(),
            reverseMap = mutableMapOf(),
            bisimulationGroups = mutableListOf(mutableSetOf("F", "G")),
            level = 1)
        assertTrue(areEq, "Глубоко вложенные структуры с переименованными переменными должны быть эквивалентными")
    }

    @Test
    fun `should not be equivalent when nested elements order differs`() {
        val result1 = Parser.Result(
            listOf(
                ResultElement.AngleStructure("F", listOf(
                    ResultElement.ParenStructure(
                        listOf(
                            ResultElement.Literal("A"),
                            ResultElement.Variable("e", "x")
                        )
                    ),
                    ResultElement.AngleStructure("G", listOf(
                        ResultElement.Literal("B")
                    ))
                ))
            )
        )
        val result2 = Parser.Result(
            listOf(
                ResultElement.AngleStructure("F", listOf(
                    ResultElement.AngleStructure("G", listOf(
                        ResultElement.Literal("B")
                    )),
                    ResultElement.ParenStructure(
                        listOf(
                            ResultElement.Literal("A"),
                            ResultElement.Variable("e", "x")
                        )
                    )
                ))
            )
        )
        val areEq = comparator.areEquivalent(result1, result2,
            forwardMap = mutableMapOf(),
            reverseMap = mutableMapOf(),
            bisimulationGroups = mutableListOf(mutableSetOf("F", "G")),
            level = 0)
        assertFalse(areEq, "Изменение порядка вложенных элементов должно привести к неэквивалентности")
    }

    @Test
    fun `should be equivalent when numeric literals are the same with renamed variables`() {
        val result1 = Parser.Result(
            listOf(
                ResultElement.Number(42),
                ResultElement.Variable("e", "x")
            )
        )
        val result2 = Parser.Result(
            listOf(
                ResultElement.Number(42),
                ResultElement.Variable("e", "y")
            )
        )
        val areEq = comparator.areEquivalent(result1, result2,
            forwardMap = mutableMapOf(),
            reverseMap = mutableMapOf(),
            bisimulationGroups = mutableListOf(mutableSetOf()),
            level = 0)
        assertTrue(areEq, "Числовые литералы одинакового значения должны быть эквивалентны, даже если переменные переименованы")
    }

    @Test
    fun `should not be equivalent when numeric literals differ`() {
        val result1 = Parser.Result(
            listOf(
                ResultElement.Number(42),
                ResultElement.Variable("e", "x")
            )
        )
        val result2 = Parser.Result(
            listOf(
                ResultElement.Number(43),
                ResultElement.Variable("e", "x")
            )
        )
        val areEq = comparator.areEquivalent(result1, result2,
            forwardMap = mutableMapOf(),
            reverseMap = mutableMapOf(),
            bisimulationGroups = mutableListOf(mutableSetOf()),
            level = 0)
        assertFalse(areEq, "Числовые литералы с разными значениями должны быть не эквивалентны")
    }

    @Test
    fun `should not be equivalent when variables have different types`() {
        val result1 = Parser.Result(
            listOf(
                ResultElement.Variable("s", "x"),
                ResultElement.Literal("A")
            )
        )
        val result2 = Parser.Result(
            listOf(
                ResultElement.Variable("e", "x"),
                ResultElement.Literal("A")
            )
        )
        val areEq = comparator.areEquivalent(result1, result2,
            forwardMap = mutableMapOf(),
            reverseMap = mutableMapOf(),
            bisimulationGroups = mutableListOf(mutableSetOf()),
            level = 0)
        assertFalse(areEq, "Переменные разных типов должны приводить к неэквивалентности")
    }

    @Test
    fun `should not be equivalent when functions have different names at higher levels`() {
        val result1 = Parser.Result(
            listOf(
                ResultElement.AngleStructure("F", listOf(ResultElement.Variable("e", "x")))
            )
        )
        val result2 = Parser.Result(
            listOf(
                ResultElement.AngleStructure("G", listOf(ResultElement.Variable("e", "x")))
            )
        )
        val areEqLevel0 = comparator.areEquivalent(result1, result2,
            forwardMap = mutableMapOf(),
            reverseMap = mutableMapOf(),
            bisimulationGroups = mutableListOf(mutableSetOf("F", "G")),
            level = 0)
        assertTrue(areEqLevel0, "На уровне 0 имена функций игнорируются")

        val areEqLevel1 = comparator.areEquivalent(result1, result2,
            forwardMap = mutableMapOf(),
            reverseMap = mutableMapOf(),
            bisimulationGroups = mutableListOf(mutableSetOf("F"), mutableSetOf("G")),
            level = 1)
        assertFalse(areEqLevel1)
    }
}
