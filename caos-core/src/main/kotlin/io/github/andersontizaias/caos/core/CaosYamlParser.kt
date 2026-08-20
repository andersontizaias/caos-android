package io.github.andersontizaias.caos.core

/**
 * Mutable line cursor, shared across recursive calls of [CaosYamlParser] — each function
 * advances [index] as it consumes lines.
 */
internal class YamlCursor(
    var index: Int = 0,
)

/**
 * Internal recursive, hand-rolled YAML parser — no third-party dependencies. Supports the
 * subset of YAML used by the Caos v1 schema: mappings, sequences, scalars (string/int/double/
 * bool/null), `#` comments, and single/double-quoted strings.
 *
 * `null`/`~` is represented by Kotlin's `null` literal, because `Map<String, Any?>` already
 * distinguishes "key absent" from "key present with a null value" natively.
 */
internal object CaosYamlParser {
    /** Entry point used only by direct parser tests. */
    fun parse(content: String): Any {
        val lines = splitLines(content)
        val cursor = YamlCursor(0)
        return parseBlock(lines, cursor, 0) ?: emptyMap<String, Any?>()
    }

    fun splitLines(content: String): List<String> = content.split("\n").map { it.removeSuffix("\r") }

    /** Returns a mapping ([Map]) or a sequence ([List] of [Map]), or `null`. */
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
     * Parses a YAML mapping at exactly `indent` spaces. Returns `Map<String, Any?>`.
     *
     * The line-by-line scanner needs multiple `continue`/`break` to handle dedent, malformed
     * indent, transitioning into a sequence, and comments in the same loop; splitting this into
     * smaller functions would obscure the logic rather than simplify it.
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

            // Dedent: this mapping level is done.
            if (indentLevel < indent) break

            // Deeper indent that isn't a continuation — malformed YAML, skip the line.
            if (indentLevel > indent) {
                cursor.index++
                continue
            }

            // A sequence item at this level — we've entered sequence context, stop.
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
                // Value lives on subsequent indented lines.
                val childIndent = nextContentIndent(lines, cursor.index)
                if (childIndent != null && childIndent > indent) {
                    val value = parseBlock(lines, cursor, childIndent)
                    if (value != null) result[key] = value
                }
                // No child content: empty/null value — key is ignored.
            } else {
                result[key] = parseScalar(stripInlineComment(rest))
            }
        }
        return result
    }

    /**
     * Parses a YAML sequence (a list of `-` items) at exactly `indent` spaces.
     * Same rationale as [parseMapping] for multiple `continue`/`break` in the same loop.
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

    /** Finds the `:` that delimits a YAML key (outside of quotes). */
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

    /** Strips an inline YAML comment (`#` preceded by whitespace, outside of quotes). */
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

    /** Indent of the next non-blank, non-comment line starting at `from`. */
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

    /** Converts a YAML scalar into an Int, Double, Boolean, String, or null. */
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

    /** Counts the leading spaces of a line. */
    fun leadingSpaces(line: String): Int = line.takeWhile { it == ' ' }.length

    private const val SEQUENCE_ITEM_INDENT_STEP = 2
    private const val MIN_QUOTED_LENGTH = 2
}
