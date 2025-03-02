import Parser.Pattern
import Parser.PatternElement
import Parser.PatternElement.PatternLiteral
import Parser.PatternElement.PatternNumber
import Parser.PatternElement.PatternParenStructure
import Parser.PatternElement.PatternVariable
import Parser.PatternElement.*

class PatternIntersectionChecker {

    private val listOfSimpleTypes = listOf(
        PatternSymbol::class.java,
        PatternVariable::class.java,
        PatternLiteral::class.java,
        PatternNumber::class.java
    )

    fun hasIntersection(p1: Pattern, p2: Pattern): Boolean {
        val substs1 = Substitution()
        val susbts2 = Substitution()
        return intersect(p1.elements, p2.elements, substs1, susbts2)
    }

    /**
     * Рекурсивно сопоставляем списки elems1 и elems2.
     * substs хранит подстановки для e/t/s‑переменных.
     */
    private fun intersect(
        elems1: List<PatternElement>,
        elems2: List<PatternElement>,
        substs1: Substitution,
        substs2: Substitution
    ): Boolean {

        if (elems1.isEmpty() && elems2.isEmpty()) return true
        if (elems1.isEmpty()) return canEsSwallow(elems2, substs1)
        if (elems2.isEmpty()) return canEsSwallow(elems1, substs2)

        val head1 = elems1.first()
        val head2 = elems2.first()

        if (!isEVar(head1) && !isEVar(head2) &&
            !isTVar(head1) && !isTVar(head2)
        ) {
            return matchOneElement(head1, head2, substs1, substs2) &&
                    intersect(elems1.drop(1), elems2.drop(1), substs1, substs2)
        }

        if (isEVar(head1)) {
            val eVar = head1 as PatternVariable
            val oldBinding = substs1.eBindings[eVar]
            if (oldBinding != null) {
                if (!prefixMatches(oldBinding, elems2, substs1, substs2)) {
                    return false
                }
                val k = oldBinding.size
                return intersect(elems1.drop(1), elems2.drop(k), substs1, substs2)
            } else {
                for (k in 0..elems2.size) {
                    val newSubsts = substs1.deepCopy()

                    newSubsts.eBindings[eVar] = elems2.take(k)

                    if (intersect(elems1.drop(1), elems2.drop(k), newSubsts, substs2)) {
                        return true
                    }

                }
                return false
            }
        }


        if (isEVar(head2)) {
            val eVar = head2 as PatternVariable
            val oldBinding = substs2.eBindings[eVar]
            if (oldBinding != null) {
                if (!prefixMatches(oldBinding, elems1, substs1, substs2)) {
                    return false
                }
                val k = oldBinding.size
                return intersect(elems1.drop(k), elems2.drop(1), substs1, substs2)
            } else {
                for (k in 0..elems1.size) {
                    val segment = elems1.take(k)
                    substs2.eBindings[eVar] = segment
                    if (intersect(elems1.drop(k), elems2.drop(1), substs1, substs2)) {
                        return true
                    }
                    substs2.eBindings.remove(eVar)
                }
                return false
            }
        }

        if (isTVar(head1)) {
            val tVar = head1 as PatternVariable
            val oldBinding = substs1.tBindings[tVar]
            if (oldBinding != null) {

                if (!prefixMatches(oldBinding, elems2, substs1, substs2)) {
                    return false
                }
                val k = oldBinding.size
                return intersect(elems1.drop(1), elems2.drop(k), substs1, substs2)
            } else {

                val oneTerm = takeOneTerm(elems2)
                    ?: return false
                substs1.tBindings[tVar] = oneTerm.term

                if (intersect(elems1.drop(1), elems2.drop(oneTerm.length), substs1, substs2))
                    return true

                substs1.tBindings.remove(tVar)
                return false
            }
        }

        if (isTVar(head2)) {
            val tVar = head2 as PatternVariable
            val oldBinding = substs2.tBindings[tVar]
            if (oldBinding != null) {
                if (!prefixMatches(oldBinding, elems1, substs1, substs2)) {
                    return false
                }
                val k = oldBinding.size
                return intersect(elems1.drop(k), elems2.drop(1), substs1, substs2)
            } else {
                for (k in 1..elems1.size) {
                    val segment = elems1.take(k)
                    substs2.tBindings[tVar] = segment
                    if (intersect(elems1.drop(k), elems2.drop(1), substs1, substs2)) {
                        return true
                    }
                    substs2.tBindings.remove(tVar)
                }
                return false
            }
        }

        return false
    }

    private fun matchOneElement(
        elem1: PatternElement,
        elem2: PatternElement,
        substs1: Substitution,
        substs2: Substitution
    ): Boolean {
        if (elem1 is PatternLiteral && elem2 is PatternLiteral) {
            return elem1.value == elem2.value
        }

        if (elem1 is PatternNumber && elem2 is PatternNumber) {
            return elem1.value == elem2.value
        }

        if (elem1 is PatternStringVal && elem2 is PatternStringVal)
            return elem1.value == elem2.value

        if (elem1 is PatternSymbol && elem2 is PatternSymbol)
            return elem1 == elem2

        if (isSVar(elem1) && (elem2.javaClass in listOfSimpleTypes)) {
            return unifySVarWithAtom(elem1 as PatternVariable, elem2, substs1)
        }

        if (isSVar(elem2) && (elem1.javaClass in listOfSimpleTypes)) {
            return unifySVarWithAtom(elem2 as PatternVariable, elem1, substs2)
        }

        if (isSVar(elem1) && isSVar(elem2)) {
            return unifyVarVar(elem1 as PatternVariable, elem2 as PatternVariable, substs1, substs2, substs1.sBindings, substs2.sBindings)
        }

        if (elem1 is PatternParenStructure && elem2 is PatternParenStructure) {
            return intersect(elem1.elements, elem2.elements, substs1, substs2)
        }

        return false
    }

    private fun unifyVarVar(
        v1: PatternVariable,
        v2: PatternVariable,
        substs1: Substitution,
        substs2: Substitution,
        map1: MutableMap<PatternVariable, List<PatternElement>>,
        map2: MutableMap<PatternVariable, List<PatternElement>>
    ): Boolean {
        val old1 = map1[v1]
        val old2 = map2[v2]

        if (old1 == null && old2 == null) {
            map1[v1] = listOf(v2)
            map2[v2] = listOf(v1)
            return true
        }

        if (old1 != null && old2 == null) {
            if (old1.size == 1 && old1[0] is PatternVariable) {
                val subVar = old1[0] as PatternVariable
                return unifyVarVar(subVar, v2, substs1, substs2, map1, map2)
            } else if (old1.size == 1) {
                val single = old1[0]
                val old2Binding = map2[v2]
                if (old2Binding == null) {
                    map2[v2] = listOf(single)
                    return true
                } else if (old2Binding.size == 1) {
                    val single2 = old2Binding[0]
                    return singleEquivalentToAtom(single, single2)
                } else {
                    return false
                }
            }
            return false
        }

        if (old1 == null && old2 != null) {
            if (old2.size == 1 && old2[0] is PatternVariable) {
                val subVar = old2[0] as PatternVariable
                return unifyVarVar(v1, subVar, substs1, substs2, map1, map2)
            } else if (old2.size == 1) {
                val single2 = old2[0]
                val old1Binding = map1[v1]
                if (old1Binding == null) {
                    map1[v1] = listOf(single2)
                    return true
                } else if (old1Binding.size == 1) {
                    return singleEquivalentToAtom(old1Binding[0], single2)
                } else {
                    return false
                }
            }
            return false
        }

        if (old1 != null && old2 != null) {
            if (old1.size == 1 && old1[0] is PatternVariable) {
                val v1sub = old1[0] as PatternVariable
                return unifyVarVar(v1sub, v2, substs1, substs2, map1, map2)
            }

            if (old2.size == 1 && old2[0] is PatternVariable) {
                val v2sub = old2[0] as PatternVariable
                return unifyVarVar(v1, v2sub, substs1, substs2, map1, map2)
            }

            if (old1.size == 1 && old2.size == 1) {
                return singleEquivalentToAtom(old1[0], old2[0])
            }
            return false
        }

        return false
    }



    private fun unifySVarWithAtom(
        sVar: PatternVariable,
        atom: PatternElement,
        substs: Substitution
    ): Boolean {
        val old = substs.sBindings[sVar]
        if (old != null) {
            if (old.size != 1) return false
            val single = old[0]
            return singleEquivalentToAtom(single, atom)
        } else {
            substs.sBindings[sVar] = listOf(atom)
            return true
        }
    }

    private fun singleEquivalentToAtom(a: PatternElement, b: PatternElement): Boolean {
        if (a is PatternLiteral && b is PatternLiteral) return a.value == b.value
        if (a is PatternNumber && b is PatternNumber) return a.value == b.value
        if (a is PatternStringVal && b is PatternStringVal) return a.value == b.value
        if (a is PatternSymbol && b is PatternSymbol) return a.text == b.text
        return false
    }


    private fun canEsSwallow(
        elems: List<PatternElement>,
        substs: Substitution
    ): Boolean {
        for (e in elems) {
            if (!isEVar(e)) return false
            val eVar = e as PatternVariable
            val bound = substs.eBindings[eVar]
            if (bound.isNullOrEmpty()) {
                substs.eBindings[eVar] = emptyList()
            } else {
                if (bound.isNotEmpty()) return false
            }
        }
        return true
    }

    private fun prefixMatches(
        segment: List<PatternElement>,
        elems: List<PatternElement>,
        substs1: Substitution,
        substs2: Substitution
    ): Boolean {
        if (segment.size > elems.size) return false
        for (i in segment.indices) {
            if (!matchOneElement(segment[i], elems[i], substs1, substs2)) {
                return false
            }
        }
        return true
    }

    private fun takeOneTerm(elems: List<PatternElement>): OneTermResult? {
        if (elems.isEmpty()) return null
        return when (val first = elems[0]) {
            is PatternLiteral, is PatternNumber -> {
                OneTermResult(listOf(first), 1)
            }
            is PatternVariable -> {
                if (first.type == "t")
                    OneTermResult(listOf(first), 1)
                else null
            }
            is PatternStringVal -> {
                if (first.value.length == 1)
                    OneTermResult(listOf(first), 1)
                 null
            }

            is PatternParenStructure -> {
                OneTermResult(listOf(first), 1)
            }

            else -> {
                null
            }
        }
    }

    private fun isEVar(e: PatternElement): Boolean {
        return e is PatternVariable && e.type == "e"
    }

    private fun isTVar(e: PatternElement): Boolean {
        return e is PatternVariable && e.type == "t"
    }

    private fun isSVar(e: PatternElement): Boolean {
        return e is PatternVariable && e.type == "s"
    }

    private data class OneTermResult(
        val term: List<PatternElement>,
        val length: Int
    )
}
