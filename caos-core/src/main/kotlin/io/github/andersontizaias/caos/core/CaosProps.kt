package io.github.andersontizaias.caos.core

/**
 * Typed dictionary of a shard's properties.
 *
 * `double`/`bool` accept a numeric/boolean string as a conversion fallback.
 */
public data class CaosProps(
    val data: Map<String, Any?> = emptyMap(),
) {
    public fun string(key: String): String? = data[key] as? String

    public fun int(key: String): Int? = data[key] as? Int

    public fun double(key: String): Double {
        (data[key] as? Double)?.let { return it }
        (data[key] as? Int)?.let { return it.toDouble() }
        (data[key] as? String)?.toDoubleOrNull()?.let { return it }
        return 0.0
    }

    public fun bool(key: String): Boolean? {
        (data[key] as? Boolean)?.let { return it }
        (data[key] as? String)?.let { return it == "true" }
        return null
    }

    public fun nested(key: String): CaosProps? {
        @Suppress("UNCHECKED_CAST")
        val map = data[key] as? Map<String, Any?> ?: return null
        return CaosProps(map)
    }

    public fun array(key: String): List<CaosProps>? {
        @Suppress("UNCHECKED_CAST")
        val list = data[key] as? List<Map<String, Any?>> ?: return null
        return list.map { CaosProps(it) }
    }

    /**
     * Validates that the key holds a valid hex color string (#RGB, #RRGGBB, #AARRGGBB).
     * Converting it to `androidx.compose.ui.graphics.Color` is the `caos-compose` module's
     * responsibility.
     */
    public fun hexColor(key: String): String? {
        val hex = data[key] as? String ?: return null
        val cleaned = hex.removePrefix("#")
        if (cleaned.length !in VALID_HEX_LENGTHS) return null
        if (!cleaned.all(::isHexDigit)) return null
        return hex
    }

    private companion object {
        val VALID_HEX_LENGTHS = setOf(3, 6, 8)

        fun isHexDigit(char: Char): Boolean = char in '0'..'9' || char in 'a'..'f' || char in 'A'..'F'
    }
}
