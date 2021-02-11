/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros

import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.psi.RsMacro
import org.rust.lang.core.psi.ext.*
import org.rust.lang.core.resolve2.DeclMacroDefInfo
import org.rust.lang.core.resolve2.MacroDefInfo
import org.rust.lang.core.resolve2.ProcMacroDefInfo
import org.rust.stdext.HashCode

class RsMacroDataWithHash<T: RsMacroData>(
    val data: T,
    val bodyHash: HashCode?
) {
    companion object {
        fun fromDeclOrProcMacroPsi(def: RsNamedElement): RsMacroDataWithHash<RsMacroData>? {
            return when {
                def is RsMacro -> RsMacroDataWithHash(RsDeclMacroData(def), def.bodyHash)
                def is RsFunction && def.isProcMacroDef -> {
                    val name = def.procMacroName ?: return null
                    val procMacro = def.containingCrate?.cargoTarget?.pkg?.procMacroArtifact ?: return null
                    val hash = HashCode.mix(procMacro.hash, HashCode.compute(name))
                    RsMacroDataWithHash(RsProcMacroData(name, procMacro), hash)
                }
                else -> null
            }
        }

        fun fromDefInfo(def: MacroDefInfo): RsMacroDataWithHash<RsMacroData>? {
            return when (def) {
                is DeclMacroDefInfo -> RsMacroDataWithHash(RsDeclMacroData(def.body), def.bodyHash)
                is ProcMacroDefInfo -> {
                    val name = def.path.name
                    val procMacroArtifact = def.procMacroArtifact ?: return null
                    val hash = HashCode.mix(procMacroArtifact.hash, HashCode.compute(name))
                    RsMacroDataWithHash(RsProcMacroData(name, procMacroArtifact), hash)
                }
            }
        }
    }
}
