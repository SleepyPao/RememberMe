package com.int4074.wordduo.ui

import com.int4074.wordduo.BuildConfig
import com.int4074.wordduo.data.EssayIssue
import com.int4074.wordduo.data.EssayReviewResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object AiEssayService {
    fun review(text: String): EssayReviewResult {
        val baseUrl = BuildConfig.AI_BASE_URL.trim().trimEnd('/')
        val apiKey = BuildConfig.AI_API_KEY.trim()
        val model = BuildConfig.AI_MODEL.trim().ifBlank { "GPT5" }

        require(baseUrl.isNotBlank()) { "AI_BASE_URL 未配置" }
        require(apiKey.isNotBlank()) { "AI_API_KEY 未配置" }

        val connection = (URL("$baseUrl/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
        }

        val body = JSONObject().apply {
            put("model", model)
            put("temperature", 0.2)
            put("max_tokens", 2000)
            put("response_format", JSONObject().apply { put("type", "json_object") })
            put(
                "messages",
                JSONArray()
                    .put(
                        JSONObject().apply {
                            put("role", "system")
                            put(
                                "content",
                                "You are an English writing examiner. Return ONLY valid JSON. Use Chinese for explanation fields. JSON schema: {\"score\": integer, \"summary\": string, \"issues\": [{\"excerpt\": string, \"category\": string, \"diagnosis\": string, \"suggestion\": string}], \"improvedText\": string}."
                            )
                        }
                    )
                    .put(
                        JSONObject().apply {
                            put("role", "user")
                            put(
                                "content",
                                "请对下面这篇英文文章进行评分和纠错，重点检查语法、用词、搭配、句式，并给出润色版本。只返回 JSON，不要 markdown。\n\n$text"
                            )
                        }
                    )
            )
        }

        connection.outputStream.use { output ->
            output.write(body.toString().toByteArray(Charsets.UTF_8))
        }

        val responseCode = connection.responseCode
        val responseText = readResponse(connection, responseCode)
        if (responseCode !in 200..299) {
            throw IllegalStateException("AI 接口返回失败：$responseCode ${responseText.take(240)}")
        }

        return parseReviewResult(text, responseText)
    }

    private fun readResponse(connection: HttpURLConnection, responseCode: Int): String {
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        return stream?.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
        }.orEmpty()
    }

    private fun parseReviewResult(sourceText: String, responseText: String): EssayReviewResult {
        val root = JSONObject(responseText)
        val content = root.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            .orEmpty()
            .ifBlank { responseText }

        val jsonText = extractJson(content)
        val resultJson = JSONObject(jsonText)
        val wordCount = Regex("[A-Za-z']+").findAll(sourceText).count()
        val sentenceCount = Regex("[.!?]+").findAll(sourceText).count().coerceAtLeast(1)
        val issues = resultJson.optJSONArray("issues")?.toIssueList().orEmpty()

        return EssayReviewResult(
            score = resultJson.optInt("score", 75).coerceIn(0, 100),
            wordCount = resultJson.optInt("wordCount", wordCount),
            sentenceCount = resultJson.optInt("sentenceCount", sentenceCount),
            summary = resultJson.optString("summary").ifBlank { "AI 已完成作文评估。" },
            issues = issues,
            improvedText = resultJson.optString("improvedText").ifBlank { sourceText }
        )
    }

    private fun JSONArray.toIssueList(): List<EssayIssue> = buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                EssayIssue(
                    excerpt = item.optString("excerpt").ifBlank { "未标注片段" },
                    category = item.optString("category").ifBlank { "综合" },
                    diagnosis = item.optString("diagnosis").ifBlank { "建议进一步优化表达。" },
                    suggestion = item.optString("suggestion").ifBlank { "请根据上下文调整。" }
                )
            )
        }
    }

    private fun extractJson(content: String): String {
        val trimmed = content.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed

        if (trimmed.startsWith("```")) {
            val lines = trimmed.lines()
            if (lines.size >= 3) {
                val inner = lines.drop(1).dropLast(1).joinToString("\n").trim()
                if (inner.startsWith("{") && inner.endsWith("}")) return inner
            }
        }

        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1)

        throw IllegalStateException("AI 返回内容不是合法 JSON")
    }
}


