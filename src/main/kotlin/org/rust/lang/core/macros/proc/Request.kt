/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.proc

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import org.rust.lang.core.macros.tt.TokenTree
import java.lang.reflect.Type

sealed class Request {
    data class ExpansionMacro(
        @SerializedName("macro_body")
        val macroBody: TokenTree.Subtree,
        @SerializedName("macro_name")
        val macroName: String,
        val attributes: TokenTree.Subtree?,
        val lib: String,
    ) : Request()
}

class RequestJsonAdapter : JsonSerializer<Request> {
    override fun serialize(json: Request, type: Type, context: JsonSerializationContext): JsonElement {
        return when (json) {
            is Request.ExpansionMacro -> JsonObject().apply {
                add("ExpansionMacro", context.serialize(json, json.javaClass))
            }
        }
    }
}
