package com.webtoapp.core.ai

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AiConfigurationAdvisorTest {

    @Test
    fun `parses valid recommendation`() {
        val result = AiConfigurationAdvisor.parse(
            """{"appName":"Example","orientation":"landscape","themeColor":"#12AbEF","fullscreen":true}"""
        )

        assertThat(result).isInstanceOf(AiConfigurationParseResult.Success::class.java)
        val recommendation = (result as AiConfigurationParseResult.Success).recommendation
        assertThat(recommendation.appName).isEqualTo("Example")
        assertThat(recommendation.orientation).isEqualTo("landscape")
        assertThat(recommendation.themeColor).isEqualTo("#12AbEF")
        assertThat(recommendation.fullscreen).isTrue()
    }

    @Test
    fun `rejects invalid values but keeps valid partial recommendation`() {
        val result = AiConfigurationAdvisor.parse(
            """{"appName":"","orientation":"square","themeColor":"red","fullscreen":false}"""
        )

        assertThat(result).isInstanceOf(AiConfigurationParseResult.Success::class.java)
        val recommendation = (result as AiConfigurationParseResult.Success).recommendation
        assertThat(recommendation.appName).isNull()
        assertThat(recommendation.orientation).isNull()
        assertThat(recommendation.themeColor).isNull()
        assertThat(recommendation.fullscreen).isFalse()
    }

    @Test
    fun `rejects malformed response`() {
        assertThat(AiConfigurationAdvisor.parse("not json"))
            .isInstanceOf(AiConfigurationParseResult.Error::class.java)
    }

    @Test
    fun `rejects response with no valid recommendations`() {
        assertThat(AiConfigurationAdvisor.parse("""{"orientation":"square"}"""))
            .isInstanceOf(AiConfigurationParseResult.Error::class.java)
    }

    @Test
    fun `accepts fenced json response`() {
        val result = AiConfigurationAdvisor.parse(
            "```json\n{\"orientation\":\"portrait\"}\n```"
        )

        assertThat(result).isInstanceOf(AiConfigurationParseResult.Success::class.java)
    }
}
