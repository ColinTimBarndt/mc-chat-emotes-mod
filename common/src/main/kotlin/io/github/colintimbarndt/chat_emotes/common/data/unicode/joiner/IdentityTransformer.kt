package io.github.colintimbarndt.chat_emotes.common.data.unicode.joiner

import io.github.colintimbarndt.chat_emotes.common.data.unicode.pattern.MatchResult
import java.util.*

object IdentityTransformer : MatchTransformer {

    override fun transform(r: MatchResult, result: StringBuilder) {
        result.append(Objects.requireNonNull(r.name))
    }

    override fun acceptsWidth(w: Int) = true

    override val acceptedWidth = 0

    override val requiresName = true

}