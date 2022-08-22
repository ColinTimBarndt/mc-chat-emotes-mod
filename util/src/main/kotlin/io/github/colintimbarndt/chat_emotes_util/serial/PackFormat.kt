package io.github.colintimbarndt.chat_emotes_util.serial

import io.github.colintimbarndt.chat_emotes_util.streamAsset

class PackFormat private constructor(val resourceFormat: Int, val dataFormat: Int, private val label: String) {
    companion object {
        val values: List<PackFormat> by lazy {
            CsvReader.parse(streamAsset("/assets/packFormats.csv")!!) {
                PackFormat(it["resourceFormat"].toInt(), it["dataFormat"].toInt(), it["label"])
            }.toList()
        }

        val latest inline get() = values.last()
    }

    enum class Kind {
        Resource,
        Data;
    }

    override fun toString() = label

    operator fun get(kind: Kind) = when (kind) {
        Kind.Resource -> resourceFormat
        Kind.Data -> dataFormat
    }
}