import main.kotlin.Parser.Pattern
import main.kotlin.Parser.PatternElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import redundant.PatternCanonicalNormalizer

class PatternCanonicalNormalizerTest {
    private val normalizer = PatternCanonicalNormalizer()

    @Test
    fun `test same structure with different variable names`() {
        // Паттерн 1: A, e.x, ,, e.x, B
        val p1 = Pattern(
            listOf(
                PatternElement.Literal("A"),
                PatternElement.Variable("e", "x"),
                PatternElement.Symbol(","),
                PatternElement.Variable("e", "x"),
                PatternElement.Literal("B")
            )
        )
        // Паттерн 2: A, e.y, ,, e.y, B
        val p2 = Pattern(
            listOf(
                PatternElement.Literal("A"),
                PatternElement.Variable("e", "y"),
                PatternElement.Symbol(","),
                PatternElement.Variable("e", "y"),
                PatternElement.Literal("B")
            )
        )
        // После канонизации оба должны дать: [A, v1, ,, v1, B]
        val flat1 = normalizer.flattenPatternCanonical(p1)
        val flat2 = normalizer.flattenPatternCanonical(p2)
        assertEquals(listOf("A", "v1", ",", "v1", "B"), flat1)
        assertEquals(flat1, flat2)
        // И сравнение должно вернуть true
        assertEquals(true, normalizer.comparePatternsCanonical(p1, p2))
    }

    @Test
    fun `test different literal order returns false`() {
        // Паттерн 1: A, e.x, ,, e.x, B
        val p1 = Pattern(
            listOf(
                PatternElement.Literal("A"),
                PatternElement.Variable("e", "x"),
                PatternElement.Symbol(","),
                PatternElement.Variable("e", "x"),
                PatternElement.Literal("B")
            )
        )
        // Паттерн 3: B, e.x, ,, e.x, A (порядок литералов меняется)
        val p3 = Pattern(
            listOf(
                PatternElement.Literal("B"),
                PatternElement.Variable("e", "x"),
                PatternElement.Symbol(","),
                PatternElement.Variable("e", "x"),
                PatternElement.Literal("A")
            )
        )
        // Канонические последовательности будут разными: [B, v1, ,, v1, A] != [A, v1, ,, v1, B]
        assertEquals(false, normalizer.comparePatternsCanonical(p1, p3))
    }

    @Test
    fun `test nested structure processing`() {
        // Паттерн 4: X, ( Y, Z ), W
        val p4 = Pattern(
            listOf(
                PatternElement.Literal("X"),
                PatternElement.ParenStructure(
                    listOf(
                        PatternElement.Literal("Y"),
                        PatternElement.Literal("Z")
                    )
                ),
                PatternElement.Literal("W")
            )
        )
        // Ожидаемая каноническая последовательность: ["X", "(", "Y", "Z", ")", "W"]
        val expected = listOf("X", "(", "Y", "Z", ")", "W")
        val flat = normalizer.flattenPatternCanonical(p4)
        assertEquals(expected, flat)
    }

    @Test
    fun `test pattern with only variables`() {
        // Паттерн 5: e.x, e.y, e.x
        val p5 = Pattern(
            listOf(
                PatternElement.Variable("e", "x"),
                PatternElement.Variable("e", "y"),
                PatternElement.Variable("e", "x")
            )
        )
        val p6 = Pattern(
            listOf(
                PatternElement.Literal("df")
            )
        )
        // Переменные получат канонические ярлыки в порядке появления:
        // Первое вхождение e.x → v1, затем e.y → v2, потом e.x снова → v1.
        val expected = listOf("v1", "v2", "v1")
        val flat = normalizer.flattenPatternCanonical(p5)
        val flat2 = normalizer.flattenPatternCanonical(p6)
        assertEquals(expected, flat)

        val result = normalizer.comparePatternsCanonical(p5, p6)

        assertEquals(false, result)
    }
}
