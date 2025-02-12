data class Token(
    val type: String,
    val value: String,
    val line: Int,
    val column: Int
)

class Lexer(private val code: String) {

    private data class TokenSpec(val type: String, val pattern: String) {
        val regex: Regex = Regex(pattern)
    }

    private val tokenSpecs = listOf(
        // Многострочные комментарии (/* ... */ или (* ... *))
        TokenSpec(TokenType.COMMENT,  "(?s)(/\\*.*?\\*/|\\(\\*.*?\\*\\))"),

        // Однострочные "звёздочные" комментарии в начале строки
        TokenSpec(TokenType.STAR_LINE, "(?m)^\\*.*"),

        // Пробелы / переводы строк
        TokenSpec(TokenType.WHITESPACE, "\\s+"),

        // Директивы: $ENTRY, $EXTERN
        TokenSpec(TokenType.ENTRY,  "\\\$ENTRY"),
        TokenSpec(TokenType.EXTERN, "\\\$EXTERN"),

        // Переменные s.X, t.X, e.X
        TokenSpec(TokenType.S_VAR, "s\\.[a-zA-Z0-9_]+"),
        TokenSpec(TokenType.T_VAR, "t\\.[a-zA-Z0-9_]+"),
        TokenSpec(TokenType.E_VAR, "e\\.[a-zA-Z0-9_]+"),

        // Имена (NAME)
        TokenSpec(TokenType.NAME, "[a-zA-Z_][a-zA-Z0-9_]*"),
        TokenSpec(TokenType.STRING, "\"[^\"]*\""), // Строки в двойных кавычках
        TokenSpec(TokenType.LITERAL, "'(\\\\.|[^'])*'"), // Литералы в одинарных кавычках
        TokenSpec(TokenType.NUMBER, "\\d+"),

        TokenSpec(TokenType.UNARY_MINUS, "-"),

        // Скобки и прочие одиночные
        TokenSpec(TokenType.LPAREN, "\\("),
        TokenSpec(TokenType.RPAREN, "\\)"),
        TokenSpec(TokenType.LBRACE, "\\{"),
        TokenSpec(TokenType.RBRACE, "\\}"),
        TokenSpec(TokenType.ANGLE_L, "<"),
        TokenSpec(TokenType.ANGLE_R, ">"),
        TokenSpec(TokenType.COMMA, ","),
        TokenSpec(TokenType.COLON, ":"),
        TokenSpec(TokenType.EQUAL, "="),
        TokenSpec(TokenType.SEMICOLON, ";"),

        TokenSpec(TokenType.UNKNOWN, ".")
    )

    private var line = 1
    private var column = 1

    private val bracketStack = mutableListOf<Pair<String, Int>>()
    private val matchingBrackets = mapOf(
        TokenType.LPAREN to TokenType.RPAREN,
        TokenType.LBRACE to TokenType.RBRACE,
        TokenType.ANGLE_L to TokenType.ANGLE_R
    )

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        var pos = 0

        while (pos < code.length) {
            var matchResult: MatchResult? = null
            var matchedType: String? = null

            for (spec in tokenSpecs) {
                val found = spec.regex.find(code, pos)
                if (found != null && found.range.first == pos) {
                    matchResult = found
                    matchedType = spec.type
                    break
                }
            }

            if (matchResult == null) {
                throw SyntaxError("Unexpected character '${code[pos]}' at line $line, column $column")
            }

            val value = matchResult.value
            val type = matchedType!!

            when (type) {
                TokenType.COMMENT -> {
                    if (!(value.endsWith("*/") || value.endsWith("*)"))) {
                        throw SyntaxError("Unclosed comment at line $line, column $column")
                    }
                }
                TokenType.STAR_LINE -> {
                    // Строка со звёздочкой — считаем коммент, пропускаем
                }
                TokenType.WHITESPACE -> {
                    // Пробелы пропускаем
                }

                TokenType.LPAREN, TokenType.LBRACE, TokenType.ANGLE_L -> {
                    bracketStack.add(type to line)
                    tokens.add(Token(type, value, line, column))
                }
                TokenType.RPAREN, TokenType.RBRACE, TokenType.ANGLE_R -> {
                    checkMatchingBracket(type)
                    tokens.add(Token(type, value, line, column))
                }

                TokenType.LITERAL -> {
                    tokens.add(Token(type, value, line, column))
                }

                else -> {
                    // Любые прочие типы
                    tokens.add(Token(type, value, line, column))
                }
            }

            updatePosition(value)
            pos = matchResult.range.last + 1
        }

        // Проверяем незакрытые скобки
        if (bracketStack.isNotEmpty()) {
            val (unclosed, openedLine) = bracketStack.last()
            throw SyntaxError("Unclosed bracket '$unclosed' opened at line $openedLine")
        }

        return tokens
    }

    private fun checkMatchingBracket(closingType: String) {
        if (bracketStack.isEmpty()) {
            throw SyntaxError("Unmatched closing bracket '$closingType' at line $line, column $column")
        }
        val (openingType, openingLine) = bracketStack.removeAt(bracketStack.size - 1)
        val expected = matchingBrackets[openingType]
        if (expected != closingType) {
            throw SyntaxError(
                "Mismatched closing bracket '$closingType' at line $line, column $column; " +
                        "was expecting '$expected' for bracket opened at line $openingLine"
            )
        }
    }

    private fun updatePosition(text: String) {
        for (ch in text) {
            if (ch == '\n') {
                line++
                column = 1
            } else {
                column++
            }
        }
    }
}
