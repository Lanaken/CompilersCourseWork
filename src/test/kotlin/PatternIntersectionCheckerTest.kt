import Parser.Pattern
import Parser.PatternElement
import Parser.PatternElement.PatternLiteral
import Parser.PatternElement.PatternParenStructure
import Parser.PatternElement.PatternVariable
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PatternIntersectionCheckerTest {

    private val checker = PatternIntersectionChecker()

    private fun patternOf(vararg elements: PatternElement): Pattern {
        return Pattern(elements.toList())
    }

    @Test
    fun `literal vs identical literal should intersect`() {
        val p1 = patternOf(PatternLiteral("A"))
        val p2 = patternOf(PatternLiteral("A"))
        assertTrue(checker.hasIntersection(p1, p2))
    }

    @Test
    fun `literal vs different literal should not intersect`() {
        val p1 = patternOf(PatternLiteral("A"))
        val p2 = patternOf(PatternLiteral("B"))
        assertFalse(checker.hasIntersection(p1, p2))
    }

    @Test
    fun `e variable should match anything`() {
        val p1 = patternOf(PatternVariable("e", "x"))
        val p2 = patternOf(PatternLiteral("A"), PatternLiteral("B"))
        assertTrue(checker.hasIntersection(p1, p2))
    }

    @Test
    fun `t variable should match single term`() {
        val p1 = patternOf(PatternVariable("t", "x"))
        val p2 = patternOf(PatternLiteral("A"))
        assertTrue(checker.hasIntersection(p1, p2))
    }

    @Test
    fun `t variable should not match multiple literals`() {
        val p1 = patternOf(PatternVariable("t", "x"))
        val p2 = patternOf(PatternLiteral("A"), PatternLiteral("B"))
        assertFalse(checker.hasIntersection(p1, p2))
    }

    @Test
    fun `s variable should match single literal`() {
        val p1 = patternOf(PatternVariable("s", "x"))
        val p2 = patternOf(PatternLiteral("A"))
        assertTrue(checker.hasIntersection(p1, p2))
    }

    @Test
    fun `nested structures should match`() {
        val p1 = patternOf(PatternParenStructure(listOf(PatternLiteral("A"))))
        val p2 = patternOf(PatternParenStructure(listOf(PatternLiteral("A"))))
        assertTrue(checker.hasIntersection(p1, p2))
    }

    @Test
    fun `nested structures should not match if different`() {
        val p1 = patternOf(PatternParenStructure(listOf(PatternLiteral("A"))))
        val p2 = patternOf(PatternParenStructure(listOf(PatternLiteral("B"))))
        assertFalse(checker.hasIntersection(p1, p2))
    }

    @Test
    fun `e x e x should match any sequence`() {
        val p1 = patternOf(
            PatternLiteral("A"),
            PatternVariable("e", "x"),
            PatternVariable("e", "x"),
            PatternLiteral("B")
        )
        val p2 = patternOf(
            PatternVariable("t", "x"),
            PatternVariable("e", "x"),
            PatternLiteral("B")
        )
        assertTrue(checker.hasIntersection(p1, p2))
    }

    @Test
    fun `t x should not match multiple elements`() {
        val p1 = patternOf(PatternVariable("t", "x"))
        val p2 = patternOf(PatternLiteral("A"), PatternLiteral("B"))
        assertFalse(checker.hasIntersection(p1, p2))
    }

    @Test
    fun `e x should match an empty sequence`() {
        val p1 = patternOf(PatternVariable("e", "x"))
        val p2 = patternOf()
        assertTrue(checker.hasIntersection(p1, p2))
    }

    @Test
    fun `t x should match parentheses`() {
        val p1 = patternOf(PatternVariable("t", "x"))
        val p2 = patternOf(PatternParenStructure(listOf(PatternLiteral("A"))))
        assertTrue(checker.hasIntersection(p1, p2))
    }

    @Test
    fun `complex nested pattern should match`() {
        val p1 = patternOf(
            PatternLiteral("A"),
            PatternVariable("e", "x"),
            PatternParenStructure(
                listOf(PatternLiteral("B"))
            )
        )
        val p2 = patternOf(
            PatternLiteral("A"),
            PatternParenStructure(
                listOf(PatternLiteral("B"))
            )
        )
        assertTrue(checker.hasIntersection(p1, p2))
    }

    @Test
    fun `complex patterns with no intersection`() {
        val p1 = patternOf(
            PatternLiteral("X"),
            PatternVariable("e", "x"),
            PatternLiteral("Y")
        )
        val p2 = patternOf(
            PatternLiteral("A"),
            PatternLiteral("B")
        )
        assertFalse(checker.hasIntersection(p1, p2))
    }

    @Test
    fun `multiple e variables should match correct segment`() {
        val p1 = patternOf(
            PatternVariable("e", "x"),
            PatternLiteral("A"),
            PatternVariable("e", "y")
        )
        val p2 = patternOf(
            PatternLiteral("B"),
            PatternLiteral("A"),
            PatternLiteral("C")
        )
        assertTrue(checker.hasIntersection(p1, p2))
    }

    @Test
    fun `e x e x should allow flexible matching`() {
        val p1 = patternOf(
            PatternLiteral("A"),
            PatternVariable("e", "x"),
            PatternVariable("e", "x"),
            PatternLiteral("B")
        )
        val p2 = patternOf(
            PatternLiteral("A"),
            PatternLiteral("C"),
            PatternLiteral("D"),
            PatternLiteral("B")
        )
        assertFalse(checker.hasIntersection(p1, p2))
    }

    @Test
    fun `s variable should not match multiple literals`() {
        val p1 = patternOf(PatternVariable("s", "x"))
        val p2 = patternOf(
            PatternLiteral("A"),
            PatternLiteral("B")
        )
        assertFalse(checker.hasIntersection(p1, p2))
    }

    /**
     * p1: (A) t.X (B)
     * p2: (A) (B)
     */
    @Test
    fun `brackets plus t variable for single bracket group`() {
        val p1 = patternOf(
            PatternParenStructure(listOf(
                PatternLiteral("A")
            )),
            // t.X
            PatternVariable("t","X"),
            PatternParenStructure(listOf(
                PatternLiteral("B")
            ))
        )
        val p2 = patternOf(
            PatternParenStructure(listOf(
                PatternLiteral("A")
            )),
            PatternParenStructure(listOf(
                PatternLiteral("B")
            ))
        )
        assertFalse(checker.hasIntersection(p1, p2))
    }

    /**
     * p1: (A B) (C D)
     * p2: t.X t.X
     */
    @Test
    fun `two bracket groups vs t X repeated should fail if they differ`() {
        val p1 = patternOf(
            PatternParenStructure(listOf(
                PatternLiteral("A"),
                PatternLiteral("B")
            )),
            PatternParenStructure(listOf(
                PatternLiteral("C"),
                PatternLiteral("D")
            ))
        )
        val p2 = patternOf(
            PatternVariable("t","X"),
            PatternVariable("t","X")
        )
        assertFalse(checker.hasIntersection(p1, p2))
    }

    /**
     * p1: (A B) (A B)
     * p2: t.X t.X
     */
    @Test
    fun `two identical bracket groups vs repeated t X should match`() {
        val p1 = patternOf(
            PatternParenStructure(listOf(
                PatternLiteral("A"),
                PatternLiteral("B")
            )),
            PatternParenStructure(listOf(
                PatternLiteral("A"),
                PatternLiteral("B")
            ))
        )
        val p2 = patternOf(
            PatternVariable("t","X"),
            PatternVariable("t","X")
        )
        assertTrue(checker.hasIntersection(p1, p2))
    }

    /**
     * p1: e.X ( e.Y )
     * p2: (A B) (C)
     */
    @Test
    fun `bracket inside bracket with e variables`() {
        val p1 = patternOf(
            PatternVariable("e","X"),
            PatternParenStructure(listOf(
                PatternVariable("e","Y")
            ))
        )
        val p2 = patternOf(
            PatternParenStructure(listOf(
                PatternLiteral("A"),
                PatternLiteral("B")
            )),
            PatternParenStructure(listOf(
                PatternLiteral("C")
            ))
        )
        assertTrue(checker.hasIntersection(p1,p2))
    }

    /**
     * p1: s.A ( e.B ) s.C
     * p2: 'Z' ( X Y ) 'Z'
     */
    @Test
    fun `mixed s var around bracket e var`() {
        val p1 = patternOf(
            PatternVariable("s","A"),
            PatternParenStructure(listOf(
                PatternVariable("e","B")
            )),
            PatternVariable("s","C")
        )
        val p2 = patternOf(
            PatternLiteral("Z"),
            PatternParenStructure(listOf(
                PatternLiteral("X"),
                PatternLiteral("Y")
            )),
            PatternLiteral("Z")
        )
        assertTrue(checker.hasIntersection(p1, p2))
    }

    /**
     * p1: t.X t.X e.Y
     * p2: 'A' 'B' 'C' 'D'
     */
    @Test
    fun `repeated t X should fail on different consecutive items`() {
        val p1 = patternOf(
            PatternVariable("t","X"),
            PatternVariable("t","X"),
            PatternVariable("e","Y")
        )
        val p2 = patternOf(
            PatternLiteral("A"),
            PatternLiteral("B"),
            PatternLiteral("C"),
            PatternLiteral("D")
        )
        assertFalse(checker.hasIntersection(p1,p2))
    }

    /**
     * p1: t.X e.Y
     * p2: 'A' 'A' 'B'
     */
    @Test
    fun `t X plus e Y with multiple elements`() {
        val p1 = patternOf(
            PatternVariable("t","X"),
            PatternVariable("e","Y")
        )
        val p2 = patternOf(
            PatternLiteral("A"),
            PatternLiteral("A"),
            PatternLiteral("B")
        )
        assertTrue(checker.hasIntersection(p1,p2))
    }

    /**
     * p1: ( s.X ) t.Y
     * p2: ( 'A' ) 'A'
     */
    @Test
    fun `bracket with s var plus t var after it`() {
        val p1 = patternOf(
            PatternParenStructure(listOf(
                PatternVariable("s","X")
            )),
            PatternVariable("t","Y")
        )
        val p2 = patternOf(
            PatternParenStructure(listOf(
                PatternLiteral("A")
            )),
            PatternLiteral("A")
        )
        assertTrue(checker.hasIntersection(p1,p2))
    }

    /**
     * p1: s.A s.A
     * p2: 'A' 'B'
     */
    @Test
    fun `repeated s var with different input items should fail`() {
        val p1 = patternOf(
            PatternVariable("s","A"),
            PatternVariable("s","A")
        )
        val p2 = patternOf(
            PatternLiteral("A"),
            PatternLiteral("B")
        )
        assertFalse(checker.hasIntersection(p1,p2))
    }

    /**
     * p1: s.A s.A
     * p2: 'X' 'X'
     */
    @Test
    fun `repeated s var with identical input items should pass`() {
        val p1 = patternOf(
            PatternVariable("s","A"),
            PatternVariable("s","A")
        )
        val p2 = patternOf(
            PatternLiteral("X"),
            PatternLiteral("X")
        )
        assertTrue(checker.hasIntersection(p1,p2))
    }

    @Test
    fun `complex example`() {
        val p1 = patternOf(
            PatternVariable("e", "x"),
            PatternLiteral("a"),
            PatternVariable("t", "y"),
            PatternVariable("s", "z")
        )
        val p2 = patternOf(
            PatternLiteral("b"),
            PatternLiteral("a"),
            PatternVariable("t", "w"),
            PatternVariable("s", "z")
        )
        assertTrue(checker.hasIntersection(p1, p2))
    }

    @Test
    fun `complex pattern with e and t bindings`() {
        // Паттерн 1: [ "start", e.x, "mid", t.y, "end" ]
        val p1 = patternOf(
            PatternLiteral("start"),
            PatternVariable("e", "x"),
            PatternLiteral("mid"),
            PatternVariable("t", "y"),
            PatternLiteral("end")
        )
        // Паттерн 2: [ "start", "A", "B", "mid", "X", "end" ]
        val p2 = patternOf(
            PatternLiteral("start"),
            PatternLiteral("A"),
            PatternLiteral("B"),
            PatternLiteral("mid"),
            PatternLiteral("X"),
            PatternLiteral("end")
        )
        assertTrue(checker.hasIntersection(p1, p2))
    }


    @Test
    fun `example with ex, "a", ty, sz vs "b", "a", tw, sz`() {
        val p1 = patternOf(
            PatternVariable("e", "x"),
            PatternLiteral("a"),
            PatternVariable("t", "y"),
            PatternVariable("s", "z")
        )
        val p2 = patternOf(
            PatternLiteral("b"),
            PatternLiteral("a"),
            PatternVariable("e", "x"),
            PatternVariable("t", "w"),
            PatternVariable("s", "z")
        )
        // Ожидаем, что алгоритм найдёт пересечение,
        // при котором:
        // e.x будет сопоставлена с [PatternLiteral("b")]
        // t.y будет унифицирована с t.w (или через binding t.y = [ ... ] и t.w = [ ... ])
        // s.z унифицируются напрямую
        assertTrue(checker.hasIntersection(p1, p2))
    }

    @Test
    fun `super complex pattern intersection test`() {
        // Паттерн 1:
        // [ "A", e.x, "B", ( "C", t.y, e.z, ( "D", s.w ) ), "E", e.a ]
        val p1 = patternOf(
            PatternLiteral("A"),
            PatternVariable("e", "x"),
            PatternLiteral("B"),
            PatternParenStructure(
                listOf(
                    PatternLiteral("C"),
                    PatternVariable("t", "y"),
                    PatternVariable("e", "z"),
                    PatternParenStructure(
                        listOf(
                            PatternLiteral("D"),
                            PatternVariable("s", "w")
                        )
                    )
                )
            ),
            PatternLiteral("E"),
            PatternVariable("e", "a")
        )

        // Паттерн 2:
        // [ "A", "X", e.x, "B", ( "C", t.y, "Z", ( "D", s.w ) ), "E", e.a ]
        // Здесь предполагается, что:
        // - e.x из p1 унифицируется с литералом "X" из p2,
        // - затем "B" совпадает,
        // - внутри скобок "C" совпадает, t.y унифицируется (будет взята из p1, например),
        // - e.z из p1 унифицируется с литералом "Z" из p2,
        // - вложенные скобки: "D" совпадает, s.w унифицируется напрямую,
        // - "E" совпадает, и e.a из обоих шаблонов объединяются.
        val p2 = patternOf(
            PatternLiteral("A"),
            PatternLiteral("X"),  // Ожидается, что это задаст binding для e.x как [PatternLiteral("X")]
            PatternVariable("e", "x"),
            PatternLiteral("B"),
            PatternParenStructure(
                listOf(
                    PatternLiteral("C"),
                    PatternVariable("t", "y"),
                    PatternLiteral("Z"),  // Ожидается, что e.z унифицируется с [PatternLiteral("Z")]
                    PatternParenStructure(
                        listOf(
                            PatternLiteral("D"),
                            PatternVariable("s", "w")
                        )
                    )
                )
            ),
            PatternLiteral("E"),
            PatternVariable("e", "a")
        )

        // Если алгоритм корректно вычисляет пересечение, то найдётся такая подстановка, при которой:
        // - e.x будет связана с [PatternLiteral("X")]
        // - e.z будет связана с [PatternLiteral("Z")]
        // - t.y, s.w и e.a унифицируются напрямую (т.е. совпадут во входе)
        assertTrue(checker.hasIntersection(p1, p2))
    }

    @Test
    fun `t variable should match bracketed structure as a single term`() {
        // Паттерн 1: [ "Start", t.X, "End" ]
        // Здесь t.X должна сопоставиться с одним термином.
        val p1 = patternOf(
            PatternLiteral("Start"),
            PatternVariable("t", "X"),
            PatternLiteral("End")
        )
        // Паттерн 2: [ "Start", ( "A", "B", "C" ), "End" ]
        // Здесь ( "A", "B", "C" ) представлена как скобочная структура, которая
        // считается одним термином для t‑переменной.
        val p2 = patternOf(
            PatternLiteral("Start"),
            PatternParenStructure(
                listOf(
                    PatternLiteral("A"),
                    PatternLiteral("B"),
                    PatternLiteral("C")
                )
            ),
            PatternLiteral("End")
        )
        // Ожидается, что t.X из p1 будет установлен как binding, равный
        // [ PatternParenStructure([ "A", "B", "C" ]) ], то есть t.X сопоставится
        // с одним термом – скобочной структурой.
        assertTrue(checker.hasIntersection(p1, p2))
    }



}
