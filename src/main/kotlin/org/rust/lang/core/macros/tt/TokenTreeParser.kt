/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.tt

import com.intellij.lang.PsiBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.TokenType.WHITE_SPACE
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.rust.lang.core.macros.tt.TokenTree.Leaf
import org.rust.lang.core.macros.tt.TokenTree.Subtree
import org.rust.lang.core.parser.RustParserUtil
import org.rust.lang.core.psi.*
import org.rust.lang.core.psi.RsElementTypes.*

fun PsiBuilder.parseSubtree(): MappedSubtree {
    return TokenTreeParser(this).parse()
}

private class TokenTreeParser(
    private val lexer: PsiBuilder
) {
    private val tokenMap = mutableListOf<TokenTextRange>()

    fun parse(): MappedSubtree {
        val result = mutableListOf<TokenTree>()

        while (true) {
            val tokenType = lexer.tokenType ?: break
            val offset = lexer.currentOffset

            parseLeafOrSubtree(offset, tokenType, result)
        }

        if (result.size == 1 && result.single() is Subtree) {
            return MappedSubtree((result.single() as Subtree), TokenMap(tokenMap))
        }

        return MappedSubtree(Subtree(null, result), TokenMap(tokenMap))
    }

    private fun parseLeafOrSubtree(offset: Int, tokenType: IElementType, result: MutableList<TokenTree>) {
        val delimKind = MacroBraces.fromOpenToken(tokenType)
        if (delimKind != null) {
            parseSubtree(offset, delimKind, result)
        } else {
            parseLeaf(offset, tokenType, result)
        }

        lexer.advanceLexer()
    }

    private fun parseSubtree(offset: Int, delimKind: MacroBraces, result: MutableList<TokenTree>) {
        val delimLeaf = Delimiter(allocDelimId(offset), delimKind)
        val subtreeResult = mutableListOf<TokenTree>()

        lexer.advanceLexer()

        while (true) {
            val tokenType = lexer.tokenType

            if (tokenType == null) {
                result += Leaf.Punct(delimKind.openText, Spacing.Alone, allocId(offset, 1))
                result += subtreeResult
                return
            }

            if (tokenType == delimKind.closeToken) break

            parseLeafOrSubtree(lexer.currentOffset, tokenType, subtreeResult)
        }

        closeDelim(delimLeaf.id, lexer.currentOffset)

        result += Subtree(delimLeaf, subtreeResult)
    }

    private fun parseLeaf(offset: Int, tokenType: IElementType, result: MutableList<TokenTree>) {
        val tokenText = lexer.tokenText!!
        when (tokenType) {
            INTEGER_LITERAL -> {
                val tokenText2 = if (RustParserUtil.parseFloatLiteral(lexer, 0)) {
                    lexer.originalText.substring(offset, lexer.currentOffset)
                } else {
                    tokenText
                }
                result += Leaf.Literal(tokenText2, allocId(offset, tokenText2.length))
            }
            in RS_LITERALS -> result += Leaf.Literal(tokenText, allocId(offset, tokenText.length))
            in RS_IDENTIFIER_TOKENS -> result += Leaf.Ident(tokenText, allocId(offset, tokenText.length))
            QUOTE_IDENTIFIER -> {
                result += Leaf.Punct(tokenText[0].toString(), Spacing.Joint, allocId(offset, 1))
                result += Leaf.Ident(tokenText.substring(1), allocId(offset, tokenText.length - 1))
            }
            else -> {
                for (i in tokenText.indices) {
                    val isLastChar = i == tokenText.lastIndex
                    val char = tokenText[i].toString()
                    val spacing = if (!isLastChar) {
                        Spacing.Joint
                    } else {
                        when (lexer.rawLookup(1)) {
                            null -> Spacing.Alone // The last token is always alone
                            in NEXT_TOKEN_ALONE_SET -> Spacing.Alone
                            else -> Spacing.Joint
                        }
                    }
                    result += Leaf.Punct(char, spacing, allocId(offset + i, 1))
                }
            }
        }
    }

    private fun allocId(startOffset: Int, length: Int): Int {
        val id = tokenMap.size
        tokenMap += TokenTextRange.Token(TextRange(startOffset, startOffset + length))
        return id
    }

    private fun allocDelimId(openOffset: Int): Int {
        val id = tokenMap.size
        tokenMap += TokenTextRange.Delimiter(openOffset, -1)
        return id
    }

    private fun closeDelim(tokeId: Int, closeOffset: Int) {
        tokenMap[tokeId] = (tokenMap[tokeId] as TokenTextRange.Delimiter).copy(closeOffset = closeOffset)
    }
}

private val NEXT_TOKEN_ALONE_SET = TokenSet.orSet(
    tokenSetOf(WHITE_SPACE),
    RS_COMMENTS,
    tokenSetOf(LBRACK, LBRACE, LPAREN, QUOTE_IDENTIFIER),
    RS_LITERALS,
    RS_IDENTIFIER_TOKENS,
)
