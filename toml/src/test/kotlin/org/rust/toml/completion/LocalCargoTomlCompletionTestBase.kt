/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion

import org.intellij.lang.annotations.Language
import org.rust.ide.experiments.RsExperiments
import org.rust.openapiext.runWithEnabledFeature
import org.rust.toml.crates.local.CargoRegistryCrate
import org.rust.toml.crates.local.CargoRegistryCrateVersion
import org.rust.toml.crates.local.withMockedCrates

abstract class LocalCargoTomlCompletionTestBase : CargoTomlCompletionTestBase() {
    protected fun crate(name: String, vararg versions: String): Crate =
        Crate(name, CargoRegistryCrate(versions.toList().map {
            CargoRegistryCrateVersion(it, false, listOf())
        }))

    protected data class Crate(val name: String, val crate: CargoRegistryCrate)

    protected fun doSingleCompletion(
        @Language("TOML") code: String,
        @Language("TOML") after: String,
        vararg crates: Crate
    ) {
        val crateMap = crates.toList().associate { it.name to it.crate }

        runWithEnabledFeature(RsExperiments.CRATES_LOCAL_INDEX) {
            withMockedCrates(crateMap) {
                completionFixture.doSingleCompletion(code, after)
            }
        }
    }

    protected fun checkNoCompletion(@Language("TOML") code: String, vararg crates: Crate) {
        val crateMap = crates.toList().associate { it.name to it.crate }

        runWithEnabledFeature(RsExperiments.CRATES_LOCAL_INDEX) {
            withMockedCrates(crateMap) {
                completionFixture.checkNoCompletion(code)
            }
        }
    }
}
