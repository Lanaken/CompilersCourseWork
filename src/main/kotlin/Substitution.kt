import Parser.PatternElement
import Parser.PatternElement.PatternVariable

data class Substitution(
    val eBindings: MutableMap<PatternVariable, List<PatternElement>> = mutableMapOf(),
    val tBindings: MutableMap<PatternVariable, List<PatternElement>> = mutableMapOf(),
    val sBindings: MutableMap<PatternVariable, List<PatternElement>> = mutableMapOf()
) {
    fun deepCopy(): Substitution {
        val eCopy = eBindings.toMutableMap()
        val tCopy = tBindings.toMutableMap()
        val sCopy = sBindings.toMutableMap()
        return Substitution(eCopy, tCopy, sCopy)
    }
}