package com.hermesandroid.relay.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * A display label the gateway appends to a tool event
 * (`tools/tool_labels.py` on the host). It exists because a name alone is not
 * always enough: an MCP call is `mcp__Home_Assistant__ha_search` and the bridge
 * that carries it is called `tool_call`, so the host classifies the call once
 * and every client renders the same thing.
 *
 * [text] is the server's own joined form, e.g. "Home Assistant · ha search".
 */
data class ToolLabel(
    val app: String = "",
    val action: String = "",
    val emoji: String? = null,
    val preview: String? = null,
    val text: String = "",
) {
    /** What to show for this call: the server's text, else app and action joined. */
    val display: String
        get() = text.ifBlank {
            when {
                app.isBlank() -> action
                action.isBlank() -> app
                else -> "$app · $action"
            }
        }

    /** Prefix for the label, when the host supplied one. */
    fun withEmoji(): String = emoji?.takeIf { it.isNotBlank() }?.let { "$it $display" } ?: display
}

/**
 * Parse the `labels` array attached to `tool.start` / `tool.complete` payloads
 * and the `tool_call_labels` map returned with history rows.
 */
fun parseToolLabels(element: JsonElement?): List<ToolLabel> {
    val array = element as? JsonArray ?: return emptyList()

    return array.mapNotNull { item ->
        val obj = item as? JsonObject ?: return@mapNotNull null
        val label = ToolLabel(
            app = obj.string("app"),
            action = obj.string("action"),
            emoji = obj.string("emoji").takeIf { it.isNotBlank() },
            preview = obj.string("preview").takeIf { it.isNotBlank() },
            text = obj.string("text"),
        )

        label.takeIf { it.display.isNotBlank() }
    }
}

/** Labels for one call id out of a history row's `tool_call_labels` map. */
fun parseToolCallLabels(element: JsonElement?, callId: String?): List<ToolLabel> {
    if (callId.isNullOrBlank()) return emptyList()
    val map = element as? JsonObject ?: return emptyList()

    return parseToolLabels(map[callId])
}

private fun JsonObject.string(key: String): String =
    (this[key] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() } ?: ""

/**
 * Readable form of an MCP tool name, for the paths that carry no label (the SSE
 * transport, or a host older than the labels payload):
 * `mcp__Home_Assistant__ha_search` -> "Home Assistant · ha search".
 */
fun mcpDisplayName(name: String): String? {
    if (!name.startsWith("mcp__")) return null
    val rest = name.removePrefix("mcp__")
    val server = rest.substringBefore("__")
    val tool = rest.substringAfter("__", "")
    if (tool.isBlank()) return null
    return "${humanizeSlug(server)} · ${tool.replace('_', ' ').trim()}"
}

private fun humanizeSlug(slug: String): String =
    slug.replace('-', ' ').replace('_', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

/** What to call this tool in the UI: the host's label, else a readable MCP name. */
fun ToolCall.displayNameOrNull(): String? =
    serverLabel?.let { if (it.emoji.isNullOrBlank()) it.display else it.withEmoji() }
        ?: mcpDisplayName(name)
