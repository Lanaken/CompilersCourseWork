import Parser.PatternElement
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PatternComparatorTest {

    private val comparator = PatternComparator()

    @Test
    fun `equivalent patterns with variable renaming should be equivalent`() {
        // Паттерн 1: ('A' e.x)
        val pattern1 = Parser.Pattern(
            listOf(
                PatternElement.PatternLiteral("A"),
                PatternElement.PatternVariable("e", "x")
            )
        )
        // Паттерн 2: ('A' e.y) – переменная переименована, но тип тот же ('e')
        val pattern2 = Parser.Pattern(
            listOf(
                PatternElement.PatternLiteral("A"),
                PatternElement.PatternVariable("e", "y")
            )
        )

        val areEq = comparator.areEquivalent(pattern1, pattern2)
        assertTrue(areEq, "Паттерны с переименованными e-переменными должны считаться эквивалентными")
    }

    @Test
    fun `patterns with different literal values should not be equivalent`() {
        // Паттерн 1: ('A' e.x)
        val pattern1 = Parser.Pattern(
            listOf(
                PatternElement.PatternLiteral("A"),
                PatternElement.PatternVariable("e", "x")
            )
        )
        // Паттерн 2: ('B' e.x)
        val pattern2 = Parser.Pattern(
            listOf(
                PatternElement.PatternLiteral("B"),
                PatternElement.PatternVariable("e", "x")
            )
        )

        val areEq = comparator.areEquivalent(pattern1, pattern2)
        assertFalse(areEq, "Паттерны с разными литералами должны считаться не эквивалентными")
    }

    @Test
    fun `patterns with nested parentheses should be compared recursively`() {
        // Паттерн 1: (('A' e.x))
        val pattern1 = Parser.Pattern(
            listOf(
                PatternElement.PatternParenStructure(
                    listOf(
                        PatternElement.PatternLiteral("A"),
                        PatternElement.PatternVariable("e", "x")
                    )
                )
            )
        )
        // Паттерн 2: (('A' e.y)) – переменная переименована
        val pattern2 = Parser.Pattern(
            listOf(
                PatternElement.PatternParenStructure(
                    listOf(
                        PatternElement.PatternLiteral("A"),
                        PatternElement.PatternVariable("e", "y")
                    )
                )
            )
        )

        val areEq = comparator.areEquivalent(pattern1, pattern2)
        assertTrue(areEq, "Вложенные структуры с переименованием переменных должны сравниваться корректно")
    }

    @Test
    fun `patterns with different order of elements should be non equivalent`() {
        // Порядок важен, поэтому ('A' e.x) и (e.x 'A') должны быть не эквивалентны.
        val pattern1 = Parser.Pattern(
            listOf(
                PatternElement.PatternLiteral("A"),
                PatternElement.PatternVariable("e", "x")
            )
        )
        val pattern2 = Parser.Pattern(
            listOf(
                PatternElement.PatternVariable("e", "x"),
                PatternElement.PatternLiteral("A")
            )
        )
        val areEq = comparator.areEquivalent(pattern1, pattern2)
        assertFalse(areEq, "Паттерны с разным порядком элементов должны считаться не эквивалентными")
    }

    @Test
    fun `complex nested pattern with multiple variables should be equivalent`() {
        // Паттерн 1: (Outer (e.x 'B') 'C')
        val pattern1 = Parser.Pattern(
            listOf(
                PatternElement.PatternLiteral("Outer"),
                PatternElement.PatternParenStructure(
                    listOf(
                        PatternElement.PatternVariable("e", "x"),
                        PatternElement.PatternLiteral("B")
                    )
                ),
                PatternElement.PatternLiteral("C")
            )
        )
        // Паттерн 2: (Outer (e.y 'B') 'C')
        val pattern2 = Parser.Pattern(
            listOf(
                PatternElement.PatternLiteral("Outer"),
                PatternElement.PatternParenStructure(
                    listOf(
                        PatternElement.PatternVariable("e", "y"), // переименование переменной
                        PatternElement.PatternLiteral("B")
                    )
                ),
                PatternElement.PatternLiteral("C")
            )
        )
        val areEq = comparator.areEquivalent(pattern1, pattern2)
        assertTrue(areEq, "Сложные паттерны с вложенными структурами и переименованием переменных должны считаться эквивалентными")
    }

    @Test
    fun `complex nested pattern with different nested order should be non equivalent`() {
        // Паттерн 1: (Outer (e.x 'B') 'C')
        val pattern1 = Parser.Pattern(
            listOf(
                PatternElement.PatternLiteral("Outer"),
                PatternElement.PatternParenStructure(
                    listOf(
                        PatternElement.PatternVariable("e", "x"),
                        PatternElement.PatternLiteral("B")
                    )
                ),
                PatternElement.PatternLiteral("C")
            )
        )
        // Паттерн 2: (Outer ('B' e.x) 'C') – порядок элементов во вложенной группе поменян
        val pattern2 = Parser.Pattern(
            listOf(
                PatternElement.PatternLiteral("Outer"),
                PatternElement.PatternParenStructure(
                    listOf(
                        PatternElement.PatternLiteral("B"),
                        PatternElement.PatternVariable("e", "x")
                    )
                ),
                PatternElement.PatternLiteral("C")
            )
        )
        val areEq = comparator.areEquivalent(pattern1, pattern2)
        assertFalse(areEq, "Паттерны с разным порядком элементов во вложенной структуре должны считаться не эквивалентными")
    }

    @Test
    fun `complex nested pattern with repeating variables`() {
        // Паттерн 1: (Outer (e.x 'B') 'C', e.x)
        val pattern1 = Parser.Pattern(
            listOf(
                PatternElement.PatternLiteral("Outer"),
                PatternElement.PatternParenStructure(
                    listOf(
                        PatternElement.PatternVariable("e", "x"),
                        PatternElement.PatternLiteral("B")
                    )
                ),
                PatternElement.PatternLiteral("C"),
                PatternElement.PatternVariable("e", "x")
            )
        )
        // Паттерн 2: (Outer (e.x 'B') 'C', e.x)
        val pattern2 = Parser.Pattern(
            listOf(
                PatternElement.PatternLiteral("Outer"),
                PatternElement.PatternParenStructure(
                    listOf(
                        PatternElement.PatternVariable("e", "x"),
                        PatternElement.PatternLiteral("B")
                    )
                ),
                PatternElement.PatternLiteral("C"),
                PatternElement.PatternVariable("e", "x")
            )
        )
        val areEq = comparator.areEquivalent(pattern1, pattern2)
        assertTrue(areEq, "Паттерны с одинаковыми структурами эквивалентны")
    }

    @Test
    fun `complex nested pattern with repeating and non-repeating variables`() {
        // Паттерн 1: (Outer (e.x 'B') 'C', e.x)
        val pattern1 = Parser.Pattern(
            listOf(
                PatternElement.PatternLiteral("Outer"),
                PatternElement.PatternParenStructure(
                    listOf(
                        PatternElement.PatternVariable("e", "x"),
                        PatternElement.PatternLiteral("B")
                    )
                ),
                PatternElement.PatternLiteral("C"),
                PatternElement.PatternVariable("e", "x")
            )
        )
        // Паттерн 2: (Outer (e.x 'B') 'C', e.y)
        val pattern2 = Parser.Pattern(
            listOf(
                PatternElement.PatternLiteral("Outer"),
                PatternElement.PatternParenStructure(
                    listOf(
                        PatternElement.PatternVariable("e", "x"),
                        PatternElement.PatternLiteral("B")
                    )
                ),
                PatternElement.PatternLiteral("C"),
                PatternElement.PatternVariable("e", "y")
            )
        )
        val areEq = comparator.areEquivalent(pattern1, pattern2)
        assertFalse(areEq, "Паттерны с разными переменными не эквивалентны")
    }
}
