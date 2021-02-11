/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion

class LocalCargoTomlDependencyCompletionProviderTest : LocalCargoTomlCompletionTestBase() {
    fun `test basic completion`() = doSingleCompletion("""
        [dependencies]
        fo<caret>
    """, """
        [dependencies]
        foo = "1.0.0"<caret>
    """, crate("foo", "1.0.0"), crate("bar", "1.0.0"))

    fun `test no completion`() = checkNoCompletion("""
        [dependencies]
        fo<caret>
    """, crate("bar", "1.0.0"))

    fun `test hyphen-underscore normalization completion`() = doSingleCompletion("""
        [dependencies]
        foo-<caret>
    """, """
        [dependencies]
        foo_bar = "1.0.0"<caret>
    """, crate("foo_bar", "1.0.0"))

    fun `test subwords completion`() = doSingleCompletion("""
        [dependencies]
        f-ba<caret>
    """, """
        [dependencies]
        foo_bar = "1.0.0"<caret>
    """, crate("foo_bar", "1.0.0"))
}
