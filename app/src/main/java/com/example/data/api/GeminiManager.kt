package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiManager {
    private const val TAG = "GeminiManager"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Executes the viral content package generation pipeline in a single structured schema call.
     */
    suspend fun generateViralPackage(
        topic: String,
        platform: String,
        niche: String,
        tone: String,
        language: String,
        duration: String,
        detailLevel: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API key is missing. Please add your GEMINI_API_KEY in the AI Studio Secrets panel.")
        }

        // 1. Build System Instruction for Pipeline Execution
        val systemInstruction = """
            You are "ReelForge Ultra", an elite cinematic short-form content producer and director.
            Your task is to take a simple video ideas and engineer an ultra-viral content production package.
            
            You must execute a multi-step AI orchestration pipeline internally:
            1. Topic analysis and Viral niche detection
            2. Hook optimization (generate exactly 5 highly clickable, human-like hooks: Curiosity, Fear, Emotional, Shocking, Storytelling)
            3. Retention-focused pacing & scene sequencing
            4. Cinematic image prompt construction (High-grade styling: Include precise Camera angle, Lens, Lighting, Mood, Rendering Quality, Atmosphere, 9:16 aspect ratio framing, ultra detailed environment details, cinematic direction)
            5. Advanced animation guides (Camera movement, environmental motion, zoom speed style, transition style)
            6. SEO content formulation tailored to the platform ($platform)
            7. Cross-linguistic localized transcription for the script (in ${language}).
            
            The script must sound extremely natural, creator-friendly, punchy, avoiding generic robotic words.
            For platform $platform, format accordingly (YouTube Shorts relies on keywords; Instagram Reels on emotional intrigue and relatability).
            
            Based on the prompt detail level '$detailLevel', the image prompts must scale in technical complexity:
            - Basic: Solid clean descriptive prompts.
            - Detailed: Adds lens details, lighting composition and exact atmosphere.
            - Cinematic: Adds cinematic render cameras (Arri Alexa, Red V-Raptor), Unreal Engine references, raytracing.
            - Ultra Cinematic: Deep depth of field, anamorphic lenses, precise color grading LUT profiles, atmospheric volumetric smoke, heavy dust particle rendering, and hyper-detailed realism.
            
            You MUST return ONLY a valid raw JSON object matching the requested schema. Do not wrap it in ```json blocks or include any extra text.
        """.trimIndent()

        // 2. Define standard JSON Schema structure to force structured outputs
        val systemPrompt = """
            Generate a full viral short-form production package for the topic: "$topic".
            Platform: $platform
            Niche: $niche
            Tone: $tone Style
            Language: $language
            Target Duration: $duration
            Detail Level: $detailLevel
            
            Return a JSON object that adheres strictly to this layout:
            {
              "topic": "String",
              "platform": "String",
              "niche": "String",
              "tone": "String",
              "language": "String",
              "duration": "String",
              "viralityScore": Integer (80 to 99),
              "retentionScore": Integer (80 to 98),
              "storyboardingConcept": "String outlining the overall visual scheme to retain views",
              "hooks": [
                {
                  "type": "Curiosity",
                  "text": "String - Extremely human-sounding, punchy click-hook"
                },
                {
                  "type": "Fear",
                  "text": "String"
                },
                {
                  "type": "Emotional",
                  "text": "String"
                },
                {
                  "type": "Shocking",
                  "text": "String"
                },
                {
                  "type": "Storytelling",
                  "text": "String"
                }
              ],
              "scenes": [
                {
                  "sceneNumber": Integer,
                  "duration": "String (e.g. 3s)",
                  "purpose": "String (e.g. Hook, Build-up, Suspense, Reveal, CTA)",
                  "emotionalIntensity": Integer (1 to 10),
                  "retentionGoal": "String explaining how it keeps people watching",
                  "narration": "String - The spoken script lines in $language. Make it sound native, flowing, and conversational",
                  "imagePrompt": "String - Ultra-detailed cinematic image creation prompt as defined in system specs",
                  "animationPrompt": "String - Advanced cinematic camera movement and dynamic animation details"
                }
              ],
              "seo": {
                "title": "String - Viral SEO Title",
                "description": "String - Detailed Description with secondary keywords",
                "caption": "String - Platform-optimized, engagement-focused short caption",
                "hashtags": "String - List of 10 relevant tags separated by spaces",
                "ctas": "String - Powerful calls to action"
              }
            }
        """.trimIndent()

        // 3. Assemble Gemini REST API Request Body
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemPrompt) })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.75)
            })
        }

        val requestUrl = "$BASE_URL?key=$apiKey"
        val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(requestUrl)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful) {
                    val errorMsg = bodyString ?: "Unknown API response error"
                    Log.e(TAG, "Gemini call failed: $errorMsg")
                    throw Exception("APIFailure: $errorMsg")
                }

                if (bodyString.isNullOrEmpty()) {
                    throw Exception("APIFailure: Received empty response from Gemini API.")
                }

                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                val textResponse = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")

                if (textResponse.isNullOrEmpty()) {
                    throw Exception("APIFailure: Response text is missing or unparseable.")
                }

                textResponse
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateViralPackage: ${e.message}", e)
            throw e
        }
    }

    /**
     * Regenerates a single specific aspect (e.g. hook or animation prompt) to avoid re-generating the whole package.
     */
    suspend fun regenerateSection(
        topic: String,
        platform: String,
        tone: String,
        language: String,
        sectionType: String,
        contextInfo: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API key is missing.")
        }

        val promptText = """
            You are ReelForge AI assistant.
            The user wants to regenerate a specific component of their short-form viral package:
            Section to Regenerate: $sectionType
            Topic: $topic
            Platform: $platform
            Tone: $tone
            Language: $language
            Context contextInfo: $contextInfo
            
            Perform this regeneration perfectly. 
            Keep it punchy, creative, optimized for short-form retention, and highly human-sounding.
            If regenerating a script hook, return ONLY the hook string itself.
            If regenerating an animation prompt or image prompt, return ONLY the cinematic action description itself.
            Do not enclose in markdown blocks. Return only the flat regenerated string.
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", promptText) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.85)
            })
        }

        val requestUrl = "$BASE_URL?key=$apiKey"
        val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(requestUrl)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw Exception("APIFailure: $bodyString")
            }

            val responseJson = JSONObject(bodyString)
            val candidates = responseJson.optJSONArray("candidates")
            val textResponse = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")

            textResponse?.trim() ?: "Generation failed"
        }
    }
}
