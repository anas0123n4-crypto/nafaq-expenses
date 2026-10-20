package com.nafaq.expenses.data.ai

import com.nafaq.expenses.data.NafaqSettings
import com.nafaq.expenses.data.SettingsStore
import com.nafaq.expenses.data.db.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AiManager(private val settings: SettingsStore) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun listModels(): Result<List<AiModel>> = withContext(Dispatchers.IO) {
        runCatching {
            val current = settings.state.value
            when (current.providerId) {
                AiProviders.GEMINI -> listGeminiModels(current)
                AiProviders.OPENROUTER -> listOpenRouterModels(current)
                else -> listCustomModels(current)
            }
        }
    }

    suspend fun parse(text: String, categories: List<CategoryEntity>): Result<AiSuggestion> =
        withContext(Dispatchers.IO) {
            runCatching {
                val current = settings.state.value
                when (current.providerId) {
                    AiProviders.GEMINI -> parseWithGemini(current, text, categories)
                    AiProviders.OPENROUTER -> parseWithOpenRouter(current, text, categories)
                    else -> parseWithCustom(current, text, categories)
                }
            }
        }

    private fun listGeminiModels(settings: NafaqSettings): List<AiModel> {
        val body = get("https://generativelanguage.googleapis.com/v1beta/models?key=${settings.apiKey}")
        val root = json.parseToJsonElement(body).asObject() ?: return emptyList()
        val models = root.asArray("models") ?: return emptyList()
        return models.mapNotNull { element ->
            val obj = element.asObject() ?: return@mapNotNull null
            val methods = obj.asArray("supportedGenerationMethods")
                ?.mapNotNull { it.asPrimitive()?.contentOrNull }
                .orEmpty()
            if (methods.isNotEmpty() && !methods.contains("generateContent")) return@mapNotNull null
            val name = obj.asPrimitive("name")?.contentOrNull ?: return@mapNotNull null
            val id = name.substringAfterLast('/')
            val label = obj.asPrimitive("displayName")?.contentOrNull ?: id
            AiModel(id, label)
        }.sortedBy { it.id }
    }

    private fun listOpenRouterModels(settings: NafaqSettings): List<AiModel> {
        val body = get("https://openrouter.ai/api/v1/models", mapOf("Authorization" to "Bearer ${settings.apiKey}"))
        val root = json.parseToJsonElement(body).asObject() ?: return emptyList()
        val models = root.asArray("data") ?: return emptyList()
        return models.mapNotNull { element ->
            val obj = element.asObject() ?: return@mapNotNull null
            val id = obj.asPrimitive("id")?.contentOrNull ?: return@mapNotNull null
            val label = obj.asPrimitive("name")?.contentOrNull ?: id
            AiModel(id, label)
        }.sortedBy { it.id }
    }

    private fun listCustomModels(settings: NafaqSettings): List<AiModel> {
        val base = settings.customBaseUrl.trimEnd('/')
        val body = get("$base/models", mapOf("Authorization" to "Bearer ${settings.apiKey}"))
        val root = json.parseToJsonElement(body).asObject() ?: return emptyList()
        val models = root.asArray("data") ?: return emptyList()
        return models.mapNotNull { element ->
            val obj = element.asObject() ?: return@mapNotNull null
            val id = obj.asPrimitive("id")?.contentOrNull ?: return@mapNotNull null
            AiModel(id, id)
        }.sortedBy { it.id }
    }

    private fun parseWithGemini(
        settings: NafaqSettings,
        text: String,
        categories: List<CategoryEntity>
    ): AiSuggestion {
        val model = settings.modelId.ifBlank { "gemini-2.0-flash" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${settings.apiKey}"
        val payload = buildJsonObject {
            put(
                "contents",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put(
                                "parts",
                                buildJsonArray {
                                    add(buildJsonObject { put("text", buildPrompt(text, categories)) })
                                }
                            )
                        }
                    )
                }
            )
            put(
                "generationConfig",
                buildJsonObject {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.0)
                }
            )
        }
        val raw = post(url, payload.toString())
        val root = json.parseToJsonElement(raw).asObject()
            ?: throw IllegalStateException("رد غير مفهوم من Gemini")
        val content = root.asArray("candidates")
            ?.firstOrNull()
            ?.asObject()
            ?.asObject("content")
            ?.asArray("parts")
            ?.firstOrNull()
            ?.asObject()
            ?.asPrimitive("text")
            ?.contentOrNull
            ?: throw IllegalStateException("رد غير مفهوم من Gemini")
        return suggestionFrom(content)
    }

    private fun parseWithOpenRouter(
        settings: NafaqSettings,
        text: String,
        categories: List<CategoryEntity>
    ): AiSuggestion {
        val model = settings.modelId.ifBlank { "openai/gpt-4o-mini" }
        val url = "https://openrouter.ai/api/v1/chat/completions"
        val headers = mapOf(
            "Authorization" to "Bearer ${settings.apiKey}",
            "HTTP-Referer" to "https://github.com/nafaq",
            "X-Title" to "Nafaq"
        )
        val payload = buildJsonObject {
            put("model", model)
            put(
                "messages",
                buildJsonArray {
                    add(buildJsonObject { put("role", "system"); put("content", systemPrompt(categories)) })
                    add(buildJsonObject { put("role", "user"); put("content", text) })
                }
            )
            put("temperature", 0.0)
            put("response_format", buildJsonObject { put("type", "json_object") })
        }
        val content = try {
            val raw = post(url, payload.toString(), headers)
            extractChatContent(raw)
        } catch (error: Throwable) {
            val fallback = buildJsonObject {
                put("model", model)
                put(
                    "messages",
                    buildJsonArray {
                        add(buildJsonObject { put("role", "system"); put("content", systemPrompt(categories)) })
                        add(buildJsonObject { put("role", "user"); put("content", text) })
                    }
                )
                put("temperature", 0.0)
            }
            val raw = post(url, fallback.toString(), headers)
            extractChatContent(raw)
        }
        return suggestionFrom(content)
    }

    private fun parseWithCustom(
        settings: NafaqSettings,
        text: String,
        categories: List<CategoryEntity>
    ): AiSuggestion {
        val base = settings.customBaseUrl.trimEnd('/')
        val payload = buildJsonObject {
            put("model", settings.modelId.ifBlank { "gpt-4o-mini" })
            put(
                "messages",
                buildJsonArray {
                    add(buildJsonObject { put("role", "system"); put("content", systemPrompt(categories)) })
                    add(buildJsonObject { put("role", "user"); put("content", text) })
                }
            )
            put("temperature", 0.0)
        }
        val raw = post("$base/chat/completions", payload.toString(), mapOf("Authorization" to "Bearer ${settings.apiKey}"))
        return suggestionFrom(extractChatContent(raw))
    }

    private fun extractChatContent(raw: String): String {
        val root = json.parseToJsonElement(raw).asObject()
            ?: throw IllegalStateException("رد غير مفهوم من المزوّد")
        return root.asArray("choices")
            ?.firstOrNull()
            ?.asObject()
            ?.asObject("message")
            ?.asPrimitive("content")
            ?.contentOrNull
            ?: throw IllegalStateException("رد غير مفهوم من المزوّد")
    }

    private fun suggestionFrom(raw: String): AiSuggestion {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        val cleaned = if (start >= 0 && end > start) raw.substring(start, end + 1) else raw
        val obj = json.parseToJsonElement(cleaned).asObject()
            ?: throw IllegalStateException("رد غير صالح")
        return AiSuggestion(
            itemName = obj.asPrimitive("item")?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() },
            categoryId = obj.asPrimitive("category_id")?.contentOrNull?.trim()?.lowercase()
                ?.takeIf { it.isNotEmpty() && it != "unknown" && it != "غير معروف" },
            amount = obj.asPrimitive("amount")?.contentOrNull?.trim()?.toDoubleOrNull(),
            currency = obj.asPrimitive("currency")?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() },
            note = obj.asPrimitive("note")?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    private fun systemPrompt(categories: List<CategoryEntity>): String {
        val list = categories.joinToString(" | ") { "${it.id}=${it.nameAr}" }
        return "أنت مساعد عربي لتسجيل المصروفات. أجب بـ JSON فقط بدون أي كلام إضافي، " +
            "وبالتالي: {\"item\":\"اسم الصنف بالعربية\",\"category_id\":\"أحد المعرفات التالية أو unknown\"," +
            "\"amount\":رقم السعر,\"currency\":\"العملة\",\"note\":\"ملاحظة اختيارية\"}. " +
            "المعرفات المتاحة: $list"
    }

    private fun buildPrompt(text: String, categories: List<CategoryEntity>): String =
        systemPrompt(categories) + "\nالجملة: " + text

    private fun get(url: String, headers: Map<String, String> = emptyMap()): String {
        val builder = Request.Builder().url(url).get()
        headers.forEach { (key, value) -> builder.header(key, value) }
        client.newCall(builder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("خطأ ${response.code}: ${body.take(240)}")
            }
            return body
        }
    }

    private fun post(url: String, body: String, headers: Map<String, String> = emptyMap()): String {
        val builder = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
        headers.forEach { (key, value) -> builder.header(key, value) }
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("خطأ ${response.code}: ${text.take(240)}")
            }
            return text
        }
    }
}

private fun JsonElement.asObject(): JsonObject? = this as? JsonObject

private fun JsonElement.asArray(key: String): JsonArray? = (this as? JsonObject)?.get(key) as? JsonArray

private fun JsonElement.asObject(key: String): JsonObject? = (this as? JsonObject)?.get(key) as? JsonObject

private fun JsonElement.asPrimitive(): JsonPrimitive? = this as? JsonPrimitive

private fun JsonElement.asPrimitive(key: String): JsonPrimitive? = (this as? JsonObject)?.get(key) as? JsonPrimitive
