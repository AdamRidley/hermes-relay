package com.hermesandroid.relay.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Decoding of the labels the gateway attaches to tool events and history rows. */
class ToolLabelTest {

    private val json = Json

    @Test
    fun parsesTheLabelsArrayFromAToolEvent() {
        val payload = json.parseToJsonElement(
            """[{"kind":"mcp","app":"Home Assistant","action":"ha search",
                "emoji":"\uD83D\uDD0C","text":"Home Assistant · ha search"}]""",
        )

        val labels = parseToolLabels(payload)

        assertEquals(1, labels.size)
        assertEquals("Home Assistant · ha search", labels.first().display)
        assertEquals("🔌", labels.first().emoji)
    }

    @Test
    fun readsTheBridgeCallLabelsFromAHistoryRow() {
        val row = json.parseToJsonElement(
            """{"call_1":[{"app":"Home Assistant","action":"ha search",
                "text":"Home Assistant · ha search"}]}""",
        )

        assertEquals(
            listOf("Home Assistant · ha search"),
            parseToolCallLabels(row, "call_1").map { it.display },
        )
        assertEquals(emptyList<ToolLabel>(), parseToolCallLabels(row, "call_missing"))
        assertEquals(emptyList<ToolLabel>(), parseToolCallLabels(row, null))
    }

    @Test
    fun labelsWithoutServerTextFallBackToAppAndAction() {
        val label = ToolLabel(app = "Home Assistant", action = "ha search")

        assertEquals("Home Assistant · ha search", label.display)
    }

    @Test
    fun mcpNamesReadAsServerAndToolWithoutALabel() {
        assertEquals(
            "Home Assistant · ha search",
            mcpDisplayName("mcp__Home_Assistant__ha_search"),
        )
        assertNull(mcpDisplayName("terminal"))
        assertNull(mcpDisplayName("mcp__Home_Assistant__"))
    }

    @Test
    fun aToolCallPrefersItsServerLabel() {
        val labelled = ToolCall(
            name = "tool_call",
            args = null,
            result = null,
            success = null,
            labels = listOf(ToolLabel(app = "Home Assistant", action = "ha search", emoji = "🔌")),
        )

        assertEquals("🔌 Home Assistant · ha search", labelled.displayNameOrNull())
        assertEquals(
            "Home Assistant · ha search",
            ToolCall(name = "mcp__Home_Assistant__ha_search", args = null, result = null, success = null)
                .displayNameOrNull(),
        )
    }
}
