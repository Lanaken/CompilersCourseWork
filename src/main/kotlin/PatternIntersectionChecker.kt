import main.kotlin.Parser

class PatternIntersectionChecker {
    /**
     * Проверяет, существует ли непустое пересечение, задаваемых паттернами p1 и p2.
     */
    fun hasIntersection(
        p1: Parser.Pattern,
        p2: Parser.Pattern
    ): Boolean {
        // Запускаем рекурсивный процесс с начальной подстановкой (пустой) и без остатка.
        return checkIntersection(p1.elements, p2.elements, mutableMapOf(), mutableMapOf())
    }

    /**
     * Рекурсивная функция для проверки пересечения двух последовательностей элементов паттернов.
     * @param elems1 оставшиеся элементы из первого паттерна
     * @param elems2 оставшиеся элементы из второго паттерна
     * @param forwardMap карта сопоставлений переменных из первого паттерна ко второму
     * @param reverseMap обратная карта сопоставлений
     * @return true, если существует способ сопоставления оставшихся элементов, иначе false.
     */
    private fun checkIntersection(
        elems1: List<Parser.PatternElement>,
        elems2: List<Parser.PatternElement>,
        forwardMap: MutableMap<Pair<String, String>, String>,
        reverseMap: MutableMap<Pair<String, String>, String>
    ): Boolean {
        if (elems1.isEmpty() && elems2.isEmpty()) return true

        // Если один из списков пуст, необходимо проверить, могут ли оставшиеся переменные быть сопоставлены с пустотой.
        if (elems1.isEmpty() || elems2.isEmpty()) {
            // Например, если остаются только e‑переменные, допускающие пустое значение.
            return canMatchEmpty(elems1) && canMatchEmpty(elems2)
        }

        val first1 = elems1.first()
        val first2 = elems2.first()

        // Попробуем сопоставить первый элемент каждого списка.
        return when {
            first1 is Parser.PatternElement.Literal && first2 is Parser.PatternElement.Literal ->
                if (first1.value == first2.value)
                    checkIntersection(elems1.drop(1), elems2.drop(1), forwardMap, reverseMap)
                else
                    false
            first1 is Parser.PatternElement.Variable && first2 !is Parser.PatternElement.Variable ->
                // Если один элемент – переменная, а другой – конкретное значение,
                // то переменная может сопоставиться с этим значением, если тип позволяет.
                checkIntersection(elems1.drop(1), elems2.drop(1), forwardMap, reverseMap)
            first1 !is Parser.PatternElement.Variable && first2 is Parser.PatternElement.Variable ->
                checkIntersection(elems1.drop(1), elems2.drop(1), forwardMap, reverseMap)
            first1 is Parser.PatternElement.Variable && first2 is Parser.PatternElement.Variable ->
                // Если оба элемента – переменные, то допускаем сопоставление, если они одного типа.
                if (first1.type == first2.type) {
                    // Устанавливаем сопоставление, если ещё не установлено.
                    val key1 = Pair(first1.type, first1.name)
                    val key2 = Pair(first2.type, first2.name)
                    if (!forwardMap.containsKey(key1) && !reverseMap.containsKey(key2)) {
                        forwardMap[key1] = first2.name
                        reverseMap[key2] = first1.name
                    }
                    checkIntersection(elems1.drop(1), elems2.drop(1), forwardMap, reverseMap)
                } else {
                    false
                }
            first1 is Parser.PatternElement.ParenStructure && first2 is Parser.PatternElement.ParenStructure ->
                // Рекурсивно сравниваем содержимое скобок
                if (checkIntersection(first1.elements, first2.elements, mutableMapOf(), mutableMapOf()))
                    checkIntersection(elems1.drop(1), elems2.drop(1), forwardMap, reverseMap)
                else
                    false
            else ->
                // Для прочих случаев (например, символы) сравниваем на равенство.
                if (first1 is Parser.PatternElement.Symbol && first2 is Parser.PatternElement.Symbol &&
                    first1.text == first2.text
                )
                    checkIntersection(elems1.drop(1), elems2.drop(1), forwardMap, reverseMap)
                else
                    false
        }
    }

    /**
     * Проверяет, могут ли все элементы списка быть сопоставлены с пустой последовательностью.
     * Обычно это true для e‑переменных, а для s‑или t‑переменных – false.
     */
    private fun canMatchEmpty(elems: List<Parser.PatternElement>): Boolean {
        return elems.all { elem ->
            when (elem) {
                is Parser.PatternElement.Variable -> elem.type == "e" // только e‑переменные допускают пустое соответствие
                else -> false
            }
        }
    }
}
