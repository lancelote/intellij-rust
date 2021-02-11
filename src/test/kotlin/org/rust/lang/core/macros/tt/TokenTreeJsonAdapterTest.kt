/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.tt

import com.google.gson.GsonBuilder
import org.intellij.lang.annotations.Language
import org.rust.RsTestBase
import org.rust.lang.core.macros.proc.Request
import org.rust.lang.core.macros.proc.RequestJsonAdapter
import org.rust.lang.core.macros.proc.Response
import org.rust.lang.core.macros.proc.ResponseJsonAdapter
import org.rust.lang.core.parser.createRustPsiBuilder

class TokenTreeJsonAdapterTest : RsTestBase() {
    fun `test 1`() = doTest(".", """
        {
          "delimiter": null,
          "token_trees": [
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Alone",
                  "id": 0
                }
              }
            }
          ]
        }
    """)

    fun `test 2`() = doTest("..", """
        {
          "delimiter": null,
          "token_trees": [
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Joint",
                  "id": 0
                }
              }
            },
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Alone",
                  "id": 1
                }
              }
            }
          ]
        }
    """)

    fun `test 3`() = doTest(".foo", """
        {
          "delimiter": null,
          "token_trees": [
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Alone",
                  "id": 0
                }
              }
            },
            {
              "Leaf": {
                "Ident": {
                  "text": "foo",
                  "id": 1
                }
              }
            }
          ]
        }
    """)

    fun `test 4`() = doTest(":::", """
        {
          "delimiter": null,
          "token_trees": [
            {
              "Leaf": {
                "Punct": {
                  "char": ":",
                  "spacing": "Joint",
                  "id": 0
                }
              }
            },
            {
              "Leaf": {
                "Punct": {
                  "char": ":",
                  "spacing": "Joint",
                  "id": 1
                }
              }
            },
            {
              "Leaf": {
                "Punct": {
                  "char": ":",
                  "spacing": "Alone",
                  "id": 2
                }
              }
            }
          ]
        }
    """)

    fun `test 5`() = doTest(". asd .. \"asd\" ...", """
        {
          "delimiter": null,
          "token_trees": [
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Alone",
                  "id": 0
                }
              }
            },
            {
              "Leaf": {
                "Ident": {
                  "text": "asd",
                  "id": 1
                }
              }
            },
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Joint",
                  "id": 2
                }
              }
            },
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Alone",
                  "id": 3
                }
              }
            },
            {
              "Leaf": {
                "Literal": {
                  "text": "\"asd\"",
                  "id": 4
                }
              }
            },
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Joint",
                  "id": 5
                }
              }
            },
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Joint",
                  "id": 6
                }
              }
            },
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Alone",
                  "id": 7
                }
              }
            }
          ]
        }
    """)

    fun `test 6`() = doTest("{}", """
        {
          "delimiter": {
            "id": 0,
            "kind": "Brace"
          },
          "token_trees": []
        }
    """)

    fun `test 7`() = doTest("[]", """
        {
          "delimiter": {
            "id": 0,
            "kind": "Bracket"
          },
          "token_trees": []
        }
    """)

    fun `test 8`() = doTest("()", """
        {
          "delimiter": {
            "id": 0,
            "kind": "Parenthesis"
          },
          "token_trees": []
        }
    """)

    fun `test 9`() = doTest("(..)", """
        {
          "delimiter": {
            "id": 0,
            "kind": "Parenthesis"
          },
          "token_trees": [
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Joint",
                  "id": 1
                }
              }
            },
            {
              "Leaf": {
                "Punct": {
                  "char": ".",
                  "spacing": "Joint",
                  "id": 2
                }
              }
            }
          ]
        }
    """)

    fun `test 10`() = doTest("([{.}])", """
        {
          "delimiter": {
            "id": 0,
            "kind": "Parenthesis"
          },
          "token_trees": [
            {
              "Subtree": {
                "delimiter": {
                  "id": 1,
                  "kind": "Bracket"
                },
                "token_trees": [
                  {
                    "Subtree": {
                      "delimiter": {
                        "id": 2,
                        "kind": "Brace"
                      },
                      "token_trees": [
                        {
                          "Leaf": {
                            "Punct": {
                              "char": ".",
                              "spacing": "Joint",
                              "id": 3
                            }
                          }
                        }
                      ]
                    }
                  }
                ]
              }
            }
          ]
        }
    """)

    fun doTest(code: String, @Language("Json") expectedJson: String) {
        val subtree = project.createRustPsiBuilder(code).parseSubtree().subtree
        val gson = GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(Request::class.java, RequestJsonAdapter())
            .registerTypeAdapter(Response::class.java, ResponseJsonAdapter())
            .registerTypeAdapter(TokenTree::class.java, TokenTreeJsonAdapter())
            .setPrettyPrinting()
            .create()
        val actualJson = gson.toJson(subtree)
        assertEquals(expectedJson.trimIndent(), actualJson)
        assertEquals(gson.fromJson(actualJson, TokenTree.Subtree::class.java), subtree)
    }
}
