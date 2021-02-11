/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import org.rust.lang.core.macros.decl.FragmentKind
import java.io.PrintWriter
import java.io.StringWriter

sealed class MacroExpansionAndParsingError<E> {
    data class ExpansionError<E>(val error: E) : MacroExpansionAndParsingError<E>()
    class ParsingError<E>(
        val expansionText: CharSequence,
        val context: MacroExpansionContext
    ) : MacroExpansionAndParsingError<E>()
}

sealed class MacroExpansionError

sealed class DeclMacroExpansionError : MacroExpansionError() {
    data class Matching(val errors: List<MacroMatchingError>) : DeclMacroExpansionError()
    object DefSyntax : DeclMacroExpansionError()
}

sealed class MacroMatchingError(macroCallBody: PsiBuilder) {
    val offsetInCallBody: Int = macroCallBody.currentOffset

    class PatternSyntax(macroCallBody: PsiBuilder) : MacroMatchingError(macroCallBody) {
        override fun toString(): String = "PatternSyntax"
    }

    class ExtraInput(macroCallBody: PsiBuilder) : MacroMatchingError(macroCallBody) {
        override fun toString(): String = "ExtraInput"
    }

    class UnmatchedToken(macroCallBody: PsiBuilder, node: ASTNode) : MacroMatchingError(macroCallBody) {
        val expectedToken: IElementType = node.elementType
        val expectedText: String = node.text
        val actualToken: IElementType? = macroCallBody.tokenType
        val actualText: String? = macroCallBody.tokenText

        override fun toString(): String = "UnmatchedToken($expectedToken(`$expectedText`) != $actualToken(`$actualText`))"
    }

    class FragmentNotParsed(macroCallBody: PsiBuilder, val kind: FragmentKind) : MacroMatchingError(macroCallBody) {
        override fun toString(): String = "FragmentNotParsed($kind)"
    }

    class EmptyGroup(macroCallBody: PsiBuilder) : MacroMatchingError(macroCallBody) {
        override fun toString(): String = "EmptyGroup"
    }

    class TooFewGroupElements(macroCallBody: PsiBuilder) : MacroMatchingError(macroCallBody) {
        override fun toString(): String = "TooFewGroupElements"
    }

    class Nesting(macroCallBody: PsiBuilder, val variableName: String) : MacroMatchingError(macroCallBody) {
        override fun toString(): String = "Nesting($variableName)"
    }
}

sealed class ProcMacroExpansionError : MacroExpansionError() {
    data class Expansion(val message: String) : ProcMacroExpansionError() {
        override fun toString(): String = "ProcMacroExpansionError.Expansion(message = \"$message\")"
    }

    data class ExceptionThrown(val cause: Exception) : ProcMacroExpansionError() {
        override fun toString(): String = StringWriter().also { writer ->
            writer.write("ProcMacroExpansionError.ExceptionThrown(\n")
            cause.printStackTrace(PrintWriter(writer))
            writer.write("\n)")
        }.toString()
    }

    object Timeout : ProcMacroExpansionError() {
        override fun toString(): String = "ProcMacroExpansionError.Timeout"
    }

    object CantRunExpander : ProcMacroExpansionError() {
        override fun toString(): String = "ProcMacroExpansionError.CantRunExpander"
    }

    object ExecutableNotFound : ProcMacroExpansionError() {
        override fun toString(): String = "ProcMacroExpansionError.ExecutableNotFound"
    }

    object MacroCallSyntax : ProcMacroExpansionError() {
        override fun toString(): String = "ProcMacroExpansionError.ExecutableNotFound"
    }
}
