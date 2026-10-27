package com.webtoapp.core.ai

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.webtoapp.core.pwa.PwaAnalysisResult

data class AiConfigurationRecommendation(
    val appName: String? = null,
    val orientation: String? = null,
    val themeColor: String? = null,
    val fullscreen: Boolean? = null,
    val explanation: String = ""
)

sealed class AiConfigurationParseResult {
    data class Success(val recommendation: AiConfigurationRecommendation) : AiConfigurationParseResult()
    data class Error(val message: String) : AiConfigurationParseResult()
}

object AiConfigurationAdvisor {
    private val colorPattern = Regex("^#[0-9a-fA-F]{6}$")

    fun buildPrompt(url: String, pwa: PwaAnalysisResult?): String {
        val metadata = pwa?.let {
            "PWA=${it.isPwa}, name=${it.suggestedName}, display=${it.suggestedDisplay}, " +
                "orientation=${it.suggestedOrientation}, themeColor=${it.suggestedThemeColor}"
        } ?: "PWA analysis unavailable"
        return """
            Recommend Android web-app settings for this website.
            URL: ${url.trim().take(500)}
            Existing metadata: $metadata
            Return JSON only with these optional fields:
            {"appName":"string","orientation":"portrait|landscape","themeColor":"#RRGGBB",
            "fullscreen":true|false,"explanation":"brief reason"}
            Do not include markdown, code, URLs, or other fields. Never invent credentials or code.
        """.trimIndent()
    }

    fun parse(raw: String): AiConfigurationParseResult {
        val jsonText = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return try {
            val json = JsonParser.parseString(jsonText).asJsonObject
            val appName = json.stringValue("appName")?.takeIf { it.isNotBlank() && it.length <= 80 }
            val orientation = json.stringValue("orientation")?.lowercase()?.takeIf {
                it == "portrait" || it == "landscape"
            }
            val themeColor = json.stringValue("themeColor")?.takeIf { colorPattern.matches(it) }
            val fullscreen = json.booleanValue("fullscreen")
            val explanation = json.stringValue("explanation")?.take(240).orEmpty()
            if (appName == null && orientation == null && themeColor == null && fullscreen == null) {
                AiConfigurationParseResult.Error("No valid settings were returned")
            } else {
                AiConfigurationParseResult.Success(
                    AiConfigurationRecommendation(appName, orientation, themeColor, fullscreen, explanation)
                )
            }
        } catch (_: Exception) {
            AiConfigurationParseResult.Error("The AI response was not valid JSON")
        }
    }

    private fun JsonObject.stringValue(name: String): String? =
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    private fun JsonObject.booleanValue(name: String): Boolean? =
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean
}
