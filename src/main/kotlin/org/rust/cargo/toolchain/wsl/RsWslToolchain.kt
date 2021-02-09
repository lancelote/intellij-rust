/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.wsl

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.wsl.WSLCommandLineOptions
import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WSLDistributionWithRoot
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.isFile
import org.rust.cargo.toolchain.RsToolchain
import org.rust.cargo.toolchain.RsToolchainProvider
import java.io.File
import java.nio.file.Path

class RsWslToolchainProvider : RsToolchainProvider {

    override fun isApplicable(homePath: Path): Boolean =
        homePath.toString().startsWith(WSLDistribution.UNC_PREFIX)

    override fun getToolchain(homePath: Path): RsToolchain? {
        val (wslPath, distribution) = parseUncPath(homePath) ?: return null
        return RsWslToolchain(wslPath, distribution)
    }
}

class RsWslToolchain(
    private val wslLocation: Path,
    distribution: WSLDistribution
) : RsToolchain(distribution.toUncPath(wslLocation)) {
    private val distribution = WSLDistributionWithRoot(distribution)

    override val fileSeparator: String = "/"

    override fun <T : GeneralCommandLine> patchCommandLine(commandLine: T): T {
        val parameters = commandLine.parametersList.list.map { toRemotePath(it) }
        commandLine.parametersList.clearAll()
        commandLine.parametersList.addAll(parameters)

        commandLine.environment.forEach { (k, v) ->
            val paths = v.split(File.pathSeparatorChar)
            commandLine.environment[k] = paths.joinToString(":") { toRemotePath(it) }
        }

        commandLine.workDirectory?.let {
            if (it.path.startsWith(fileSeparator)) {
                commandLine.workDirectory = File(toLocalPath(it.path))
            }
        }

        val remoteWorkDir = commandLine.workDirectory?.absolutePath
            ?.let { toRemotePath(it) }
        val options = WSLCommandLineOptions()
            .setRemoteWorkingDirectory(remoteWorkDir)
            .addInitCommand(". ~/.profile")
        return distribution.patchCommandLine(commandLine, null, options)
    }

    override fun startProcess(commandLine: GeneralCommandLine): ProcessHandler = RsWslProcessHandler(commandLine)

    override fun toLocalPath(remotePath: String): String =
        distribution.getWindowsPath(FileUtil.toSystemIndependentName(remotePath)) ?: remotePath

    override fun toRemotePath(localPath: String): String =
        distribution.getWslPath(localPath) ?: localPath

    override fun expandUserHome(remotePath: String): String =
        distribution.expandUserHome(remotePath)

    override fun getExecutableName(toolName: String): String = toolName

    override fun pathToExecutable(toolName: String): Path = wslLocation.pathToExecutable(toolName)

    override fun hasExecutable(exec: String): Boolean =
        distribution.toUncPath(pathToExecutable(exec)).isFile()

    override fun hasCargoExecutable(exec: String): Boolean =
        distribution.toUncPath(pathToCargoExecutable(exec)).isFile()
}
