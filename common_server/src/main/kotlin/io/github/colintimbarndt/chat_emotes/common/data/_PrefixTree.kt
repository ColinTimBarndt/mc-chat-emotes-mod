@file:JvmName("PrefixTreeKt")
@file:Suppress("NOTHING_TO_INLINE")

package io.github.colintimbarndt.chat_emotes.common.data

import io.netty.util.collection.CharObjectHashMap

@JvmInline
value class AliasPrefixTree(
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

    private inline fun addAlias(_alias: String) {
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

class EmojiPrefixTree {
    private val charData = CharObjectHashMap<PrefixTreeNode>(1024)
    private val data = HashMap<String, PrefixTreeNode>()

    internal fun load(bundles: List<EmoteDataBundle>) {
        charData.clear()
        data.clear()
        bundles.asSequence()
            .flatMap { it }
            .mapNotNull { it.emoji }
            .forEach(::addEmoji)
    }

    private inline fun addEmoji(str: String) {
        if (str.isEmpty()) return
        val first = str[0]
        if (str.length == 1) {
            charData[first] = PrefixTreeNode.Valid
        } else {
            charData.putIfAbsent(first, PrefixTreeNode.Indeterminate)
            data[str] = PrefixTreeNode.Valid
            var len = str.length
            while (--len > 0) {
                data.putIfAbsent(str.substring(0, len), PrefixTreeNode.Indeterminate)
            }
        }
    }

    operator fun get(emoji: String) = when (emoji.length) {
        0 -> PrefixTreeNode.Invalid
        1 -> charData[emoji[0]] ?: PrefixTreeNode.Invalid
        else -> data[emoji] ?: PrefixTreeNode.Invalid
    }

    operator fun get(emoji: Char) = charData[emoji] ?: PrefixTreeNode.Invalid
}

enum class PrefixTreeNode {
    Indeterminate,
    Valid,
    Invalid,
}

private inline fun String.substringBeforeAlmostLast(delimiter: Char): String {
    val idx = lastIndexOf(delimiter)
    return if (idx < 0) this
    else substring(0, idx - 1)
}