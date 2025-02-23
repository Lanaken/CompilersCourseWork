package main.kotlin

import main.kotlin.TokenType.ENTRY
import main.kotlin.TokenType.EXTERN

//Program → ProgramElement Program | ε
//
//ProgramElement → ExternDeclaration | FunctionDefinition
//
//ExternDeclaration → $EXTERN NameList ;
//
//FunctionDefinition → ($ENTRY NAME)? NAME { SentenceList }
//
//SentenceList → Sentence SentenceList | ε
//
//Sentence → Pattern = Result ;
//
//Pattern → PatternElement Pattern | ε
//Result → ResultElement Result | ε
//
//PatternElement → Variable | Literal | ParenStructure | Symbol
//ResultElement → Variable | Literal | AngleStructure | ParenStructure | Symbol
//
//Variable → S_VAR | T_VAR | E_VAR
//Literal → STRING | NUMBER | NAME
//Symbol → COMMA | COLON | UNARY_MINUS
//
//AngleStructure → < NAME PatternList >
//ParenStructure → ( PatternList )
//
//PatternList → PatternElement PatternList | ε



class Parser(private val tokens: List<Token>) {
    private var pos = 0

    sealed class ProgramElement {
        data class ExternDeclaration(val names: List<String>) : ProgramElement()
        data class FunctionDefinition(
            val name: String,
            val isEntryPoint: Boolean,
            val body: List<Sentence>
        ) : ProgramElement()
    }

    data class Program(val elements: List<ProgramElement>)

    data class Sentence(val pattern: Pattern, val result: Result)

    data class Pattern(val elements: List<PatternElement>)
    data class Result(val elements: List<ResultElement>)

    sealed class PatternElement {
        data class Variable(val type: String, val name: String) : PatternElement()
        data class Literal(val value: String) : PatternElement()
        data class ParenStructure(val elements: List<PatternElement>) : PatternElement()
        data class Number(val value: Int) : PatternElement()
        data class StringVal(val value: String): PatternElement()

        data class Symbol(val text: String) : PatternElement()
    }

    sealed class ResultElement {
        data class Variable(val type: String, val name: String) : ResultElement()
        data class Literal(val value: String) : ResultElement()
        data class AngleStructure(val constructor: String, val elements: List<ResultElement>) : ResultElement()
        data class ParenStructure(val elements: List<ResultElement>) : ResultElement()
        data class Number(val value: Int) : ResultElement()
        data class StringVal(val value: String): ResultElement()


        data class Symbol(val text: String) : ResultElement()
    }

    fun parse(): Program {
        val elements = mutableListOf<ProgramElement>()
        while (!isAtEnd()) {
            skipWhitespaceAndComments()
            if (isAtEnd()) break

            if (check(EXTERN)) {
                elements.add(parseExternDeclaration())
            } else {
                elements.add(parseFunctionDefinition())
            }
        }
        return Program(elements)
    }

    // ---------------------------------
    // parseExternDeclaration: $EXTERN F, G, ...
    // ---------------------------------
    private fun parseExternDeclaration(): ProgramElement.ExternDeclaration {
        advance() // съели $EXTERN
        val names = mutableListOf<String>()
        do {
            skipWhitespaceAndComments()
            val nameTok = consume(TokenType.NAME, "Expected name after '$EXTERN'")
            names.add(nameTok.value)
            skipWhitespaceAndComments()
        } while (match(TokenType.COMMA))

        consume(TokenType.SEMICOLON, "Expected ';' after EXTERN declaration")
        return ProgramElement.ExternDeclaration(names)
    }

    private fun parseFunctionDefinition(): ProgramElement.FunctionDefinition {
        skipWhitespaceAndComments()

        var isEntry = false
        val funcName: String

        if (check(ENTRY)) {
            // $ENTRY
            advance()  // съели $ENTRY
            isEntry = true
            // ждём NAME("Go")
            val tokGo = consume(TokenType.NAME, "Expected 'Go' after '$ENTRY'")
            if (tokGo.value != "Go") {
                throw syntaxError("Expected 'Go' after '$ENTRY'")
            }
            funcName = tokGo.value
        } else {
            // Обычная функция
            val nameTok = consume(TokenType.NAME, "Expected function name")
            funcName = nameTok.value
        }

        consume(TokenType.LBRACE, "Expected '{' after function name")

        val sentences = mutableListOf<Sentence>()
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            skipWhitespaceAndComments()
            if (check(TokenType.RBRACE)) break
            sentences.add(parseSentence())
        }

        consume(TokenType.RBRACE, "Expected '}' after function body")
        return ProgramElement.FunctionDefinition(funcName, isEntry, sentences)
    }

    private fun parseSentence(): Sentence {
        val pattern = parsePattern()
        consume(TokenType.EQUAL, "Expected '=' in sentence")
        val result = parseResult()
        consume(TokenType.SEMICOLON, "Expected ';' at end of sentence")
        return Sentence(pattern, result)
    }

    private fun parsePattern(): Pattern {
        val elems = mutableListOf<PatternElement>()
        while (!check(TokenType.EQUAL) && !isAtEnd()) {
            skipWhitespaceAndComments()
            if (check(TokenType.EQUAL)) break
            elems.add(parsePatternElement())
        }
        return Pattern(elems)
    }

    private fun parsePatternElement(): PatternElement {
        skipWhitespaceAndComments()

        if (match(TokenType.COMMA)) {
            return PatternElement.Symbol(",")
        }
        if (match(TokenType.COLON)) {
            return PatternElement.Symbol(":")
        }

        return when {
            match(TokenType.S_VAR, TokenType.T_VAR, TokenType.E_VAR) -> {
                val tk = previous()
                PatternElement.Variable(tk.type, tk.value)
            }
            match(TokenType.LITERAL) -> {
                val tk = previous()
                PatternElement.Literal(tk.value)
            }
            match(TokenType.NUMBER) -> {
                val tk = previous()
                PatternElement.Number(tk.value.toInt())
            }
            match(TokenType.NAME, TokenType.STRING) -> {
                val tk = previous()
                PatternElement.StringVal(tk.value)
            }
            match(TokenType.LPAREN) -> {
                val subs = mutableListOf<PatternElement>()
                while (!check(TokenType.RPAREN) && !isAtEnd()) {
                    skipWhitespaceAndComments()
                    if (check(TokenType.RPAREN)) break
                    subs.add(parsePatternElement())
                }
                consume(TokenType.RPAREN, "Expected ')' to close parenthesis")
                PatternElement.ParenStructure(subs)
            }
            else -> {
                throw syntaxError("Unexpected token in pattern: ${peek().value}")
            }
        }
    }

    private fun parseResult(): Result {
        val elems = mutableListOf<ResultElement>()
        while (!check(TokenType.SEMICOLON) && !isAtEnd()) {
            skipWhitespaceAndComments()
            if (check(TokenType.SEMICOLON)) break
            elems.add(parseResultElement())
        }
        return Result(elems)
    }

    private fun parseResultElement(): ResultElement {
        skipWhitespaceAndComments()

        if (match(TokenType.COMMA)) {
            return ResultElement.Symbol(",")
        }
        if (match(TokenType.COLON)) {
            return ResultElement.Symbol(":")
        }

        // Унарный минус
        if (match(TokenType.UNARY_MINUS)) {
            val operand = parseResultElement()
            if (operand !is ResultElement.Literal) {
                throw syntaxError("Unary minus applies only to literal values for now")
            }
            return ResultElement.Literal("-${operand.value}")
        }

        return when {
            match(TokenType.S_VAR, TokenType.T_VAR, TokenType.E_VAR) -> {
                val tk = previous()
                ResultElement.Variable(tk.type, tk.value)
            }
            match(TokenType.LITERAL) -> {
                val tk = previous()
                ResultElement.Literal(tk.value)
            }
            match(TokenType.NUMBER) -> {
                val tk = previous()
                ResultElement.Number(tk.value.toInt())
            }
            match(TokenType.NAME, TokenType.STRING) -> {
                val tk = previous()
                ResultElement.StringVal(tk.value)
            }
            match(TokenType.ANGLE_L) -> {
                val ctorTok = consume(TokenType.NAME, "Expected constructor name after '<'")
                val subs = mutableListOf<ResultElement>()
                while (!check(TokenType.ANGLE_R) && !isAtEnd()) {
                    skipWhitespaceAndComments()
                    if (check(TokenType.ANGLE_R)) break
                    subs.add(parseResultElement())
                }
                consume(TokenType.ANGLE_R, "Expected '>' to close structure")
                ResultElement.AngleStructure(ctorTok.value, subs)
            }
            match(TokenType.LPAREN) -> {
                val subs = mutableListOf<ResultElement>()
                while (!check(TokenType.RPAREN) && !isAtEnd()) {
                    skipWhitespaceAndComments()
                    if (check(TokenType.RPAREN)) break
                    subs.add(parseResultElement())
                }
                consume(TokenType.RPAREN, "Expected ')' to close parenthesis")
                ResultElement.ParenStructure(subs)
            }
            else -> {
                throw syntaxError("Unexpected token in result: ${peek().value}")
            }
        }
    }

    private fun skipWhitespaceAndComments() {
        while (!isAtEnd() && (check(TokenType.WHITESPACE) || check(TokenType.COMMENT) || check(TokenType.STAR_LINE))) {
            advance()
        }
    }

    private fun match(vararg types: String): Boolean {
        if (isAtEnd()) return false
        if (types.contains(peek().type)) {
            advance()
            return true
        }
        return false
    }

    private fun consume(type: String, msg: String): Token {
        if (check(type)) return advance()
        throw syntaxError(msg)
    }

    private fun check(vararg types: String): Boolean {
        if (isAtEnd()) return false
        return types.contains(peek().type)
    }

    private fun check(type: String): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun advance(): Token {
        if (!isAtEnd())
            pos++
        return previous()
    }

    private fun isAtEnd() = pos >= tokens.size

    private fun peek() = tokens[pos]

    private fun previous() = tokens[pos - 1]


    private fun syntaxError(msg: String): SyntaxError {
        val token = if (isAtEnd()) {
            if (tokens.isNotEmpty()) tokens.last() else Token("UNKNOWN","",0,0)
        } else {
            peek()
        }
        return SyntaxError("$msg at line ${token.line}, column ${token.column}")
    }
}
