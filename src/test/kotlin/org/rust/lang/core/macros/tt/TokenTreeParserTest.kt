/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.tt

import org.rust.RsTestBase
import org.rust.lang.core.parser.createRustPsiBuilder

class TokenTreeParserTest : RsTestBase() {
    fun `test 0`() = doTest(".", """
        SUBTREE $
          PUNCH   . [alone] 0
    """)

    fun `test 1`() = doTest("..", """
        SUBTREE $
          PUNCH   . [joint] 0
          PUNCH   . [alone] 1
    """)

    fun `test 2`() = doTest("...", """
        SUBTREE $
          PUNCH   . [joint] 0
          PUNCH   . [joint] 1
          PUNCH   . [alone] 2
    """)

    fun `test 3`() = doTest(".. .", """
        SUBTREE $
          PUNCH   . [joint] 0
          PUNCH   . [alone] 1
          PUNCH   . [alone] 2
    """)

    fun `test 4`() = doTest(".foo", """
        SUBTREE $
          PUNCH   . [alone] 0
          IDENT   foo 1
    """)

    fun `test 5`() = doTest(":::", """
        SUBTREE $
          PUNCH   : [joint] 0
          PUNCH   : [joint] 1
          PUNCH   : [alone] 2
    """)

    fun `test 6`() = doTest("()", """
        SUBTREE () 0
    """)

    fun `test 7`() = doTest("{}", """
        SUBTREE {} 0
    """)

    fun `test 8`() = doTest("[]", """
        SUBTREE [] 0
    """)

    fun `test 9`() = doTest("""."foo"""", """
        SUBTREE $
          PUNCH   . [alone] 0
          LITERAL "foo" 1
    """)

    fun `test 10`() = doTest(""".r"foo"""", """
        SUBTREE $
          PUNCH   . [alone] 0
          LITERAL r"foo" 1
    """)

    fun `test 11`() = doTest(""".r#"foo"#""", """
        SUBTREE $
          PUNCH   . [alone] 0
          LITERAL r#"foo"# 1
    """)

    fun `test 12`() = doTest("1", """
        SUBTREE $
          LITERAL 1 0
    """)

    fun `test 13`() = doTest("-1", """
        SUBTREE $
          PUNCH   - [alone] 0
          LITERAL 1 1
    """)

    fun `test 14`() = doTest("1i32", """
        SUBTREE $
          LITERAL 1i32 0
    """)

    fun `test 15`() = doTest("1f32", """
        SUBTREE $
          LITERAL 1f32 0
    """)

    fun `test 16`() = doTest("1.2", """
        SUBTREE $
          LITERAL 1.2 0
    """)

    fun `test 17`() = doTest("1.2f64", """
        SUBTREE $
          LITERAL 1.2f64 0
    """)

    fun `test 18`() = doTest("1.2e-1", """
        SUBTREE $
          LITERAL 1.2e-1 0
    """)

    fun `test 19`() = doTest("'f'", """
        SUBTREE $
          LITERAL 'f' 0
    """)

    fun `test 20`() = doTest("'foo", """
        SUBTREE $
          PUNCH   ' [joint] 0
          IDENT   foo 1
    """)

    fun `test 21`() = doTest(".(.).", """
        SUBTREE $
          PUNCH   . [alone] 0
          SUBTREE () 1
            PUNCH   . [joint] 2
          PUNCH   . [alone] 3
    """)

    fun `test 22`() = doTest(".(.{.[].}.)", """
        SUBTREE $
          PUNCH   . [alone] 0
          SUBTREE () 1
            PUNCH   . [alone] 2
            SUBTREE {} 3
              PUNCH   . [alone] 4
              SUBTREE [] 5
              PUNCH   . [joint] 6
            PUNCH   . [joint] 7
    """)

    fun `test 23`() = doTest(". ( . { . [ ] . } . ) .", """
        SUBTREE $
          PUNCH   . [alone] 0
          SUBTREE () 1
            PUNCH   . [alone] 2
            SUBTREE {} 3
              PUNCH   . [alone] 4
              SUBTREE [] 5
              PUNCH   . [alone] 6
            PUNCH   . [alone] 7
          PUNCH   . [alone] 8
    """)

    fun `test 24`() = doTest("(", """
        SUBTREE $
          PUNCH   ( [alone] 1
    """)

    fun `test 25`() = doTest("(.", """
        SUBTREE $
          PUNCH   ( [alone] 2
          PUNCH   . [alone] 1
    """)

    fun `test 26`() = doTest(")", """
        SUBTREE $
          PUNCH   ) [alone] 0
    """)

    fun `test 27`() = doTest(".)", """
        SUBTREE $
          PUNCH   . [joint] 0
          PUNCH   ) [alone] 1
    """)

    fun `test 28`() = doTest("{(.}", """
        SUBTREE $
          PUNCH   { [alone] 5
          PUNCH   ( [alone] 4
          PUNCH   . [joint] 2
          PUNCH   } [alone] 3
    """)

    fun `test 29`() = doTest("{.)}", """
        SUBTREE {} 0
          PUNCH   . [joint] 1
          PUNCH   ) [joint] 2
    """)

    fun `test 30`() = doTest(".'foo", """
        SUBTREE $
          PUNCH   . [alone] 0
          PUNCH   ' [joint] 1
          IDENT   foo 2
    """)

    fun doTest(code: String, expected: String) {
        val subtree = project.createRustPsiBuilder(code).parseSubtree().subtree
        assertEquals(expected.trimIndent(), subtree.toDebugString())
    }
}
