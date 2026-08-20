package io.github.andersontizaias.caos.core

/**
 * Parseia arquivos de schema YAML v1 do Caos.
 * Usa apenas a stdlib do Kotlin — sem dependências de terceiros.
 */
public object CaosParser {
    private const val SUPPORTED_VERSION = 1

    /**
     * Parseia o conteúdo YAML e retorna um [CaosSchema].
     * Lança [CaosParseException] se o conteúdo for inválido.
     */
    @Suppress("ThrowsCount")
    public fun parse(content: String): CaosSchema {
        val lines = CaosYamlParser.splitLines(content)
        val cursor = YamlCursor(0)

        @Suppress("UNCHECKED_CAST")
        val root =
            CaosYamlParser.parseMapping(lines, cursor, 0) as? Map<String, Any?>
                ?: throw CaosParseException(CaosError.InvalidYaml(line = 0, reason = "Root must be a mapping"))

        val version = root["version"] as? Int ?: throw CaosParseException(CaosError.MissingVersion)
        if (version != SUPPORTED_VERSION) {
            throw CaosParseException(CaosError.UnsupportedVersion(version))
        }

        @Suppress("UNCHECKED_CAST")
        val screensRaw = root["screens"] as? List<Map<String, Any?>> ?: emptyList()
        val screens = screensRaw.map(::parseScreen)
        return CaosSchema(version = version, screens = screens)
    }

    private fun parseScreen(dict: Map<String, Any?>): CaosScreen {
        val id = dict["id"] as? String ?: ""

        @Suppress("UNCHECKED_CAST")
        val containerDict = dict["container"] as? Map<String, Any?>
        val containerConfig = containerDict?.let(::parseContainer) ?: CaosContainer()

        @Suppress("UNCHECKED_CAST")
        val shardsRaw = dict["shards"] as? List<Map<String, Any?>> ?: emptyList()
        val shardList = shardsRaw.map(::parseShard)

        return CaosScreen(id = id, containerConfig = containerConfig, shardList = shardList)
    }

    private fun parseContainer(containerDict: Map<String, Any?>): CaosContainer {
        val type = containerDict["type"] as? String ?: "vertical"
        val spacing = doubleOf(containerDict["spacing"])

        @Suppress("UNCHECKED_CAST")
        val paddingDict = containerDict["padding"] as? Map<String, Any?>
        val padding =
            if (paddingDict != null) {
                CaosEdgeInsets(
                    top = doubleOf(paddingDict["top"]),
                    left = doubleOf(paddingDict["leading"]),
                    bottom = doubleOf(paddingDict["bottom"]),
                    right = doubleOf(paddingDict["trailing"]),
                )
            } else {
                CaosEdgeInsets.Zero
            }
        return CaosContainer(type = type, spacing = spacing, padding = padding)
    }

    private fun parseShard(dict: Map<String, Any?>): CaosShard {
        val type = dict["type"] as? String ?: ""
        val id = dict["id"] as? String ?: ""

        @Suppress("UNCHECKED_CAST")
        val props = CaosProps(dict["props"] as? Map<String, Any?> ?: emptyMap())
        return CaosShard(type = type, id = id, props = props)
    }

    private fun doubleOf(value: Any?): Double =
        when (value) {
            is Double -> value
            is Int -> value.toDouble()
            else -> 0.0
        }
}
