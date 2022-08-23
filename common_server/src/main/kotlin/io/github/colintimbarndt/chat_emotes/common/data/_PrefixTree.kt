@file:JvmName("PrefixTreeKt")

package io.github.colintimbarndt.chat_emotes.common.data

@JvmInline
value class PrefixTree(
    private val data: HashMap<String, PrefixTreeNode>
) {
    internal fun load(bundles: List<EmoteDataBundle>, depthLimit: Int) {
        data.clear()
        val dblDepthLimit = depthLimit shl 1
        bundles.asSequence()
            .flatMap { it }
            .flatMap { it.aliasesWithInnerColons }
            .filter { alias -> alias.count { it == ':' } < dblDepthLimit }
            .forEach(::addAlias)
    }

    private fun addAlias(_alias: String) {
        var alias = _alias
        data[alias] = PrefixTreeNode.Valid
        var aliasOld = alias
        alias = alias.substringBeforeAlmostLast(':')
        if (alias != aliasOld) {
            do {
                data.putIfAbsent(alias, PrefixTreeNode.Indeterminate)
                aliasOld = alias
                alias = alias.substringBeforeAlmostLast(':')
            } while (alias != aliasOld)
        }
    }

    operator fun get(alias: String) = data[alias] ?: PrefixTreeNode.Invalid
}

enum class PrefixTreeNode {
    Indeterminate,
    Valid,
    Invalid,
}

@Suppress("NOTHING_TO_INLINE")
private inline fun String.substringBeforeAlmostLast(delimiter: Char): String {
    val idx = lastIndexOf(delimiter)
    return if (idx < 0) this
    else substring(0, idx - 1)
}