package io.github.andersontizaias.caos.core

/**
 * Cursor mutável de linha, compartilhado entre chamadas recursivas de [CaosYamlParser] — cada
 * função avança [index] conforme consome linhas.
 */
internal class YamlCursor(
    var index: Int = 0,
)

/**
 * Parser YAML interno, recursivo, hand-rolled — sem dependências de terceiros. Suporta o
 * subconjunto de YAML usado pelo schema Caos v1: mappings, sequences, escalares (string/int/
 * double/bool/null), comentários `#` e strings entre aspas simples/duplas.
 *
 * `null`/`~` é representado pelo `null` literal do Kotlin, porque `Map<String, Any?>` já
 * distingue "chave ausente" de "chave presente com valor nulo" nativamente.
 */
internal object CaosYamlParser {
    /** Ponto de entrada usado apenas em testes diretos do parser. */
    fun parse(content: String): Any {
        val lines = splitLines(content)
        val cursor = YamlCursor(0)
        return parseBlock(lines, cursor, 0) ?: emptyMap<String, Any?>()
    }

    fun splitLines(content: String): List<String> = content.split("\n").map { it.removeSuffix("\r") }

    /** Retorna um mapping ([Map]) ou uma sequence ([List] de [Map]), ou `null`. */
    fun parseBlock(
        lines: List<String>,
        cursor: YamlCursor,
        indent: Int,
    ): Any? {
        var peek = cursor.index
        while (peek < lines.size) {
            val trimmed = lines[peek].trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                peek++
                continue
            }
            val indentLevel = leadingSpaces(lines[peek])
            if (indentLevel < indent) return null
            return if (trimmed.startsWith("-")) {
                parseSequence(lines, cursor, indent)
            } else {
                parseMapping(lines, cursor, indent)
            }
        }
        return null
    }

    /**
     * Parseia um mapping YAML em exatamente `indent` espaços. Retorna `Map<String, Any?>`.
     *
     * O scanner linha-a-linha precisa de múltiplos `continue`/`break` para tratar dedent, indent
     * malformado, transição pra sequence e comentários no mesmo laço; separar isso em funções
     * menores obscureceria a lógica em vez de simplificar.
     */
    @Suppress("ReturnCount", "CyclomaticComplexMethod", "NestedBlockDepth", "LoopWithTooManyJumpStatements")
    fun parseMapping(
        lines: List<String>,
        cursor: YamlCursor,
        indent: Int,
    ): Any {
        val result = LinkedHashMap<String, Any?>()

        while (cursor.index < lines.size) {
            val line = lines[cursor.index]
            val trimmed = line.trim()

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                cursor.index++
                continue
            }

            val indentLevel = leadingSpaces(line)

            // Dedent: terminou este nível de mapping.
            if (indentLevel < indent) break

            // Indent mais profundo que não é continuação — YAML malformado, pula a linha.
            if (indentLevel > indent) {
                cursor.index++
                continue
            }

            // Item de sequence neste nível — entramos em contexto de sequence, para.
            if (trimmed.startsWith("-")) break

            val colonIndex = findKeyColon(trimmed)
            if (colonIndex == null) {
                cursor.index++
                continue
            }

            val key = trimmed.substring(0, colonIndex).trim()
            val rest = trimmed.substring(colonIndex + 1).trim()

            cursor.index++

            if (rest.isEmpty() || rest == "|" || rest == ">") {
                // Valor está em linhas subsequentes indentadas.
                val childIndent = nextContentIndent(lines, cursor.index)
                if (childIndent != null && childIndent > indent) {
                    val value = parseBlock(lines, cursor, childIndent)
                    if (value != null) result[key] = value
                }
                // Sem conteúdo filho: valor vazio/nulo — chave é ignorada.
            } else {
                result[key] = parseScalar(stripInlineComment(rest))
            }
        }
        return result
    }

    /**
     * Parseia uma sequence YAML (lista de itens `-`) em exatamente `indent` espaços.
     * Mesma justificativa de [parseMapping] pra múltiplos `continue`/`break` no mesmo laço.
     */
    @Suppress(
        "ReturnCount",
        "CyclomaticComplexMethod",
        "NestedBlockDepth",
        "LoopWithTooManyJumpStatements",
    )
    private fun parseSequence(
        lines: List<String>,
        cursor: YamlCursor,
        indent: Int,
    ): List<Map<String, Any?>> {
        val result = mutableListOf<Map<String, Any?>>()

        while (cursor.index < lines.size) {
            val line = lines[cursor.index]
            val trimmed = line.trim()

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                cursor.index++
                continue
            }

            val indentLevel = leadingSpaces(line)
            if (indentLevel < indent) break
            if (indentLevel > indent) {
                cursor.index++
                continue
            }
            if (!trimmed.startsWith("-")) break

            cursor.index++

            val afterDash = trimmed.drop(1).trim()
            val itemBodyIndent = indent + SEQUENCE_ITEM_INDENT_STEP
            val itemDict = LinkedHashMap<String, Any?>()

            if (afterDash.isNotEmpty() && !afterDash.startsWith("#")) {
                val colonIndex = findKeyColon(afterDash)
                if (colonIndex != null) {
                    val inlineKey = afterDash.substring(0, colonIndex).trim()
                    val inlineRest = afterDash.substring(colonIndex + 1).trim()
                    if (inlineRest.isEmpty()) {
                        val nestedIndent = nextContentIndent(lines, cursor.index)
                        if (nestedIndent != null && nestedIndent >= itemBodyIndent) {
                            val value = parseBlock(lines, cursor, nestedIndent)
                            if (value != null) itemDict[inlineKey] = value
                        }
                    } else {
                        itemDict[inlineKey] = parseScalar(stripInlineComment(inlineRest))
                    }
                }
            }

            @Suppress("UNCHECKED_CAST")
            val more = parseMapping(lines, cursor, itemBodyIndent) as? Map<String, Any?> ?: emptyMap()
            itemDict.putAll(more)

            result.add(itemDict)
        }
        return result
    }

    /** Encontra o `:` que delimita uma chave YAML (fora de aspas). */
    @Suppress("NestedBlockDepth")
    fun findKeyColon(line: String): Int? {
        var inSingleQuote = false
        var inDoubleQuote = false
        for (i in line.indices) {
            when (val char = line[i]) {
                '\'' -> if (!inDoubleQuote) inSingleQuote = !inSingleQuote
                '"' -> if (!inSingleQuote) inDoubleQuote = !inDoubleQuote
                ':' ->
                    if (!inSingleQuote && !inDoubleQuote) {
                        val next = i + 1
                        if (next == line.length || line[next] == ' ' || line[next] == '\t') return i
                    }
                else -> Unit
            }
        }
        return null
    }

    /** Remove comentário inline YAML (`#` precedido de espaço, fora de aspas). */
    fun stripInlineComment(line: String): String {
        var inSingle = false
        var inDouble = false
        var prev = ' '
        for (i in line.indices) {
            val char = line[i]
            when {
                char == '\'' && !inDouble -> inSingle = !inSingle
                char == '"' && !inSingle -> inDouble = !inDouble
                char == '#' && !inSingle && !inDouble && (prev == ' ' || prev == '\t') -> {
                    return line.substring(0, i).trim()
                }
            }
            prev = char
        }
        return line
    }

    /** Indent da próxima linha não vazia / não comentário a partir de `from`. */
    fun nextContentIndent(
        lines: List<String>,
        from: Int,
    ): Int? {
        var cursor = from
        while (cursor < lines.size) {
            val trimmed = lines[cursor].trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) return leadingSpaces(lines[cursor])
            cursor++
        }
        return null
    }

    /** Converte um escalar YAML em Int, Double, Boolean, String ou null. */
    @Suppress("ReturnCount")
    fun parseScalar(raw: String): Any? {
        if (raw == "[]") return emptyList<Map<String, Any?>>()
        if (raw == "{}") return emptyMap<String, Any?>()

        if (raw.length >= MIN_QUOTED_LENGTH) {
            val isDoubleQuoted = raw.startsWith("\"") && raw.endsWith("\"")
            val isSingleQuoted = raw.startsWith("'") && raw.endsWith("'")
            if (isDoubleQuoted || isSingleQuoted) return raw.substring(1, raw.length - 1)
        }
        if (raw == "true") return true
        if (raw == "false") return false
        if (raw == "null" || raw == "~") return null

        raw.toIntOrNull()?.let { return it }
        raw.toDoubleOrNull()?.let { return it }
        return raw
    }

    /** Conta os espaços à esquerda de uma linha. */
    fun leadingSpaces(line: String): Int = line.takeWhile { it == ' ' }.length

    private const val SEQUENCE_ITEM_INDENT_STEP = 2
    private const val MIN_QUOTED_LENGTH = 2
}
