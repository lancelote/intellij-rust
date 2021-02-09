/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.wsl.flavors

import com.intellij.execution.wsl.WSLUtil
import org.rust.cargo.toolchain.tools.Rustup
import org.rust.cargo.toolchain.wsl.expandUserHome
import org.rust.stdext.toPath
import java.io.File
import java.nio.file.Path

class RsWslRustupToolchainFlavor : RsWslToolchainFlavor() {

    @Suppress("UnstableApiUsage")
    override fun getHomePathCandidates(): List<Path> {
        val paths = mutableListOf<Path>()
        for (distro in WSLUtil.getAvailableDistributions()) {
            val cargoBin = distro.expandUserHome("~/.cargo/bin")
            val file = File(distro.uncRoot, cargoBin)
            if (!file.isDirectory) continue
            paths.add(file.absolutePath.toPath())
        }
        return paths
    }

    override fun isValidToolchainPath(path: Path): Boolean =
        super.isValidToolchainPath(path) && hasExecutable(path, Rustup.NAME)
}
