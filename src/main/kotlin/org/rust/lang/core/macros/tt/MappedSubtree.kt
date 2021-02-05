/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.tt

import com.intellij.openapi.util.TextRange
import com.intellij.util.SmartList
import org.rust.lang.core.macros.MappedTextRange
import org.rust.lang.core.macros.RangeMap
import org.rust.lang.core.macros.mergeAdd

data class MappedSubtree(val subtree: TokenTree.Subtree, val tokenMap: TokenMap)

class TokenMap(val map: List<TokenTextRange>) {
    fun get(id: Int): TokenTextRange? = map.getOrNull(id)
}

sealed class TokenTextRange {
    data class Token(val range: TextRange): TokenTextRange()
    data class Delimiter(val openOffset: Int, val closeOffset: Int): TokenTextRange()
}

fun MappedSubtree.toMappedText(): Pair<CharSequence, RangeMap> {
    return SubtreeTextBuilder(subtree, tokenMap).toText()
}

private class SubtreeTextBuilder(
    private val subtree: TokenTree.Subtree,
    private val tokenMap: TokenMap
) {
    private val sb = StringBuilder()
    private val ranges = SmartList<MappedTextRange>()

    fun toText(): Pair<CharSequence, RangeMap> {
        subtree.appendSubtree()
        return sb to RangeMap.from(ranges)
    }

    private fun TokenTree.Subtree.appendSubtree() {
        delimiter?.let { appendOpenDelim(it) }
        for (tokenTree in tokenTrees) {
            when (tokenTree) {
                is TokenTree.Leaf -> tokenTree.appendLeaf()
                is TokenTree.Subtree -> tokenTree.appendSubtree()
            }
        }
        delimiter?.let { appendCloseDelim(it) }
    }

    private fun TokenTree.Leaf.appendLeaf() {
        when (this) {
            is TokenTree.Leaf.Literal -> append(text, id)
            is TokenTree.Leaf.Ident -> {
                append(text, id)
                sb.append(" ")
            }
            is TokenTree.Leaf.Punct -> {
                append(char, id)
                if (spacing == Spacing.Alone) {
                    sb.append(" ")
                }
            }
        }
    }

    private fun append(text: CharSequence, id: Int) {
        val range = (tokenMap.get(id) as? TokenTextRange.Token)?.range
        if (range != null) {
            ranges.mergeAdd(MappedTextRange(range.startOffset, sb.length, text.length))
        }
        sb.append(text)
    }

    private fun appendOpenDelim(delimiter: Delimiter) {
        val offset = (tokenMap.get(delimiter.id) as? TokenTextRange.Delimiter)?.openOffset
        if (offset != null) {
            ranges.mergeAdd(MappedTextRange(offset, sb.length, 1))
        }
        sb.append(delimiter.kind.openText)
    }

    private fun appendCloseDelim(delimiter: Delimiter) {
        val offset = (tokenMap.get(delimiter.id) as? TokenTextRange.Delimiter)?.closeOffset
        if (offset != null && offset != -1) {
            ranges.mergeAdd(MappedTextRange(offset, sb.length, 1))
        }
        sb.append(delimiter.kind.closeText)
    }
}
