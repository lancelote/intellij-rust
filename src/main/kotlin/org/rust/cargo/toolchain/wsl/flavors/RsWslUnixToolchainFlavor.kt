/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.wsl.flavors

import com.intellij.execution.wsl.WSLUtil
import org.rust.stdext.toPath
import java.io.File
import java.nio.file.Path

class RsWslUnixToolchainFlavor : RsWslToolchainFlavor() {
    @Suppress("UnstableApiUsage")
    override fun getHomePathCandidates(): List<Path> {
        val paths = mutableListOf<Path>()
        for (distro in WSLUtil.getAvailableDistributions()) {
            val uncRoot = distro.uncRoot
            for (root in listOf("/usr/local/bin", "/usr/bin")) {
                val file = File(uncRoot, root)
                if (!file.isDirectory) continue
                paths.add(file.absolutePath.toPath())
            }
        }
        return paths
    }
}
