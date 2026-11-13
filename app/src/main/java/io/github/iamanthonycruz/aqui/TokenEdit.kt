package io.github.iamanthonycruz.aqui

/**
 * The message box shows {place}, {minutes} and {map} as single pills, so they must also edit like
 * one character: a delete or a selection that cuts into one takes the whole token, and typing
 * inside one lands after it. No Android code, so it is unit-tested.
 */
data class TokenEdit(val text: String, val cursor: Int) {
    companion object {
        private val ALL = listOf(MessageTemplate.PLACE, MessageTemplate.MINUTES, MessageTemplate.MAP)

        /** Where each token sits in [text], in order. */
        fun tokens(text: String): List<IntRange> =
            Regex(ALL.joinToString("|") { Regex.escape(it) }).findAll(text).map { it.range }.toList()

        /** The edit [old] -> [new] (cursor at [cursor] in [new]), widened so no token is left half-cut. */
        fun fix(old: String, new: String, cursor: Int): TokenEdit {
            if (old == new) return TokenEdit(new, cursor)
            // The changed stretch: [start, oldEnd) in old became [start, newEnd) in new.
            val maxCommon = minOf(old.length, new.length)
            var start = 0
            while (start < maxCommon && old[start] == new[start]) start++
            var suffix = 0
            while (suffix < maxCommon - start && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
            var oldEnd = old.length - suffix
            val inserted = new.substring(start, new.length - suffix)

            val cut = tokens(old).filter { it.first < oldEnd && it.last + 1 > start || it.first < start && start <= it.last }
            if (cut.isEmpty()) return TokenEdit(new, cursor)

            if (start == oldEnd) {
                // Pure typing inside a token: put it after the token.
                val t = cut.first()
                val text = old.substring(0, t.last + 1) + inserted + old.substring(t.last + 1)
                return TokenEdit(text, t.last + 1 + inserted.length)
            }
            start = minOf(start, cut.minOf { it.first })
            oldEnd = maxOf(oldEnd, cut.maxOf { it.last + 1 })
            return TokenEdit(old.substring(0, start) + inserted + old.substring(oldEnd), start + inserted.length)
        }
    }
}
