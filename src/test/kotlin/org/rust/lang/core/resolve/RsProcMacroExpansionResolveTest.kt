/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve

import com.intellij.openapi.util.Disposer
import org.rust.MinRustcVersion
import org.rust.cargo.RsWithToolchainTestBase
import org.rust.ide.experiments.RsExperiments
import org.rust.lang.core.macros.MacroExpansionScope
import org.rust.lang.core.macros.macroExpansionManager
import org.rust.lang.core.psi.RsMethodCall
import org.rust.openapiext.RsPathManager
import org.rust.openapiext.runWithEnabledFeature

class RsProcMacroExpansionResolveTest : RsWithToolchainTestBase() {
    fun `test simple`() = runWithEnabledFeature(RsExperiments.EVALUATE_BUILD_SCRIPTS, RsExperiments.PROC_MACROS) {
        if (RsPathManager.nativeHelper() == null) return@runWithEnabledFeature

        Disposer.register(
            earlyTestRootDisposable,
            project.macroExpansionManager.setUnitTestExpansionModeAndDirectory(MacroExpansionScope.ALL, name)
        )

        buildProject {
            toml("Cargo.toml", """
                [workspace]
                members = ["my_proc_macro", "mylib"]
            """)
            dir("my_proc_macro") {
                toml("Cargo.toml", """
                    [package]
                    name = "my_proc_macro"
                    version = "1.0.0"
                    edition = "2018"

                    [lib]
                    proc-macro = true

                    [dependencies]
                """)
                dir("src") {
                    rust("lib.rs", """
                        extern crate proc_macro;
                        use proc_macro::TokenStream;

                        #[proc_macro]
                        pub fn my_macro(input: TokenStream) -> TokenStream {
                            return input;
                        }
                    """)
                }
            }
            dir("mylib") {
                toml("Cargo.toml", """
                    [package]
                    name = "mylib"
                    version = "1.0.0"
                    edition = "2018"

                    [dependencies]
                    my_proc_macro = { path = "../my_proc_macro" }
                """)
                dir("src") {
                    rust("lib.rs", """
                        use my_proc_macro::my_macro;

                        struct Foo;
                        impl Foo {
                            fn bar(&self) {}
                        }     //X

                        my_macro! {
                            fn foo() -> Foo { Foo }
                        }

                        fn main() {
                            foo().bar()
                        }       //^
                    """)
                }
            }
        }.checkReferenceIsResolved<RsMethodCall>("mylib/src/lib.rs")
    }

    private val earlyTestRootDisposable = Disposer.newDisposable()

    override fun tearDown() {
        Disposer.dispose(earlyTestRootDisposable)
        super.tearDown()
    }
}
