/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.proc

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Disposer
import org.rust.ide.experiments.RsExperiments
import org.rust.openapiext.isFeatureEnabled

@Service
class ProcMacroApplicationService : Disposable {

    private var sharedServer: ProcMacroServer? = null

    @Synchronized
    fun getServer(): ProcMacroServer? {
        if (!isFeatureEnabled(RsExperiments.PROC_MACROS)) return null

        var server = sharedServer
        if (server == null) {
            server = ProcMacroServer.tryCreate()
            if (server != null) {
                Disposer.register(this, server)
            }
            sharedServer = server
        }
        return server
    }

    override fun dispose() {}

    companion object {
        fun getInstance(): ProcMacroApplicationService = service()
    }
}
