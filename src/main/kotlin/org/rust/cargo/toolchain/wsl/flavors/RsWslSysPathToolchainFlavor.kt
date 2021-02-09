/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.wsl.flavors

import com.intellij.execution.wsl.WSLUtil
import com.intellij.openapi.project.ProjectManager
import org.rust.cargo.toolchain.wsl.fetchEnvironmentVariable
import org.rust.stdext.toPath
import java.io.File
import java.nio.file.Path

class RsWslSysPathToolchainFlavor : RsWslToolchainFlavor() {
    @Suppress("UnstableApiUsage")
    override fun getHomePathCandidates(): List<Path> {
        val paths = mutableListOf<Path>()
        val project = ProjectManager.getInstance().defaultProject
        for (distro in WSLUtil.getAvailableDistributions()) {
            val sysPath = distro.fetchEnvironmentVariable(project, "PATH") ?: continue
            val uncRoot = distro.uncRoot
            for (root in sysPath.split(":")) {
                if (root.isEmpty()) continue
                val file = File(uncRoot, root)
                if (!file.isDirectory) continue
                paths.add(file.absolutePath.toPath())
            }
        }
        return paths
    }
}
