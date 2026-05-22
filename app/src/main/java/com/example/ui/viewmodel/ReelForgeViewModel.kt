package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiManager
import com.example.data.local.AppDatabase
import com.example.data.local.ViralPackage
import com.example.data.repository.PackageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed interface GenerationState {
    object Idle : GenerationState
    data class Loading(val progress: Float, val status: String) : GenerationState
    data class Success(val packageId: Int, val data: JSONObject) : GenerationState
    data class Error(val message: String) : GenerationState
}

class ReelForgeViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ReelForgeViewModel"
    private val database = AppDatabase.getDatabase(application)
    private val repository = PackageRepository(database.viralPackageDao())

    // UI Input States
    val topic = MutableStateFlow("")
    val platform = MutableStateFlow("YouTube Shorts")
    val niche = MutableStateFlow("Entertainment")
    val tone = MutableStateFlow("Cinematic")
    val language = MutableStateFlow("English")
    val duration = MutableStateFlow("30s")
    val detailLevel = MutableStateFlow("Cinematic")

    // Search & Filter state for Saved History
    val historySearchQuery = MutableStateFlow("")

    // Generation UI status
    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    // Currently opened history package detail state (nullable)
    private val _selectedPackage = MutableStateFlow<ViralPackage?>(null)
    val selectedPackage: StateFlow<ViralPackage?> = _selectedPackage.asStateFlow()

    // Observe Saved Packages flow from Repository
    val savedPackages: StateFlow<List<ViralPackage>> = repository.allPackages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onTopicChanged(value: String) {
        topic.value = value
    }

    fun onPlatformChanged(value: String) {
        platform.value = value
    }

    fun onNicheChanged(value: String) {
        niche.value = value
    }

    fun onToneChanged(value: String) {
        tone.value = value
    }

    fun onLanguageChanged(value: String) {
        language.value = value
    }

    fun onDurationChanged(value: String) {
        duration.value = value
    }

    fun onDetailLevelChanged(value: String) {
        detailLevel.value = value
    }

    /**
     * Executes the heavy ReelForge generation process.
     * Sequenced step simulation on the UI for high-end SaaS feel.
     */
    fun forgePackage() {
        val promptText = topic.value.trim()
        if (promptText.isEmpty()) {
            _generationState.value = GenerationState.Error("Please enter your viral video idea.")
            return
        }

        val sPlatform = platform.value
        val sNiche = niche.value
        val sTone = tone.value
        val sLang = language.value
        val sDuration = duration.value
        val sDetail = detailLevel.value

        viewModelScope.launch {
            try {
                // Give keyboard hide & focus removal and layout resize animations 200ms to complete safely
                delay(200)
                // Simulate and progress of multi-step AI pipeline
                _generationState.value = GenerationState.Loading(0.05f, "Step 1/6: Analyzing Video Idea & Viral Niche...")
                delay(800)
                
                _generationState.value = GenerationState.Loading(0.15f, "Step 2/6: Planning High-Retention Timeline Sequence...")
                delay(600)

                _generationState.value = GenerationState.Loading(0.30f, "Step 3/6: Activating Gemini Engine for Script Writing...")
                
                // Perform real API call to Gemini
                val resultBody = GeminiManager.generateViralPackage(
                    topic = promptText,
                    platform = sPlatform,
                    niche = sNiche,
                    tone = sTone,
                    language = sLang,
                    duration = sDuration,
                    detailLevel = sDetail
                )

                _generationState.value = GenerationState.Loading(0.65f, "Step 4/6: Localizing Slang & Storytelling Codes...")
                delay(500)

                _generationState.value = GenerationState.Loading(0.80f, "Step 5/6: Enhancing Cinematic Camera & Art Prompts...")
                delay(500)

                _generationState.value = GenerationState.Loading(0.95f, "Step 6/6: final Index Tuning & Content Serialization...")
                
                // Parse standard JSONObject to verify correct structures
                val parsedJson = JSONObject(resultBody)

                // Save to Room Database local history persistence
                val rawEntity = ViralPackage(
                    title = promptText,
                    platform = sPlatform,
                    niche = sNiche,
                    tone = sTone,
                    language = sLang,
                    duration = sDuration,
                    detailLevel = sDetail,
                    contentJson = resultBody
                )

                val newId = repository.insert(rawEntity)
                _generationState.value = GenerationState.Success(newId.toInt(), parsedJson)
            } catch (e: Exception) {
                Log.e(TAG, "Failed in forgePackage API sequence", e)
                val userErrorMessage = when {
                    e.message?.contains("APIKey", ignoreCase = true) == true -> 
                        "Missing configuration key. Please add your GEMINI_API_KEY in the AI Studio Secrets panel."
                    e.message?.contains("APIFailure", ignoreCase = true) == true ->
                        "Gemini generation error: ${e.localizedMessage ?: "Please check connection & inputs."}"
                    else -> 
                        "Generation pipeline failed: ${e.localizedMessage ?: "Please verify your network and retry."}"
                }
                _generationState.value = GenerationState.Error(userErrorMessage)
            }
        }
    }

    /**
     * Regenerates a single specific hook index inside the active generation state.
     */
    fun regenerateHook(hookIndex: Int, hookType: String) {
        val currentState = _generationState.value
        if (currentState !is GenerationState.Success) return

        viewModelScope.launch {
            try {
                // Fetch context
                val data = currentState.data
                val sTopic = data.optString("topic", topic.value)
                val sPlatform = data.optString("platform", platform.value)
                val sTone = data.optString("tone", tone.value)
                val sLang = data.optString("language", language.value)

                val updatedHookText = GeminiManager.regenerateSection(
                    topic = sTopic,
                    platform = sPlatform,
                    tone = sTone,
                    language = sLang,
                    sectionType = "$hookType Hook",
                    contextInfo = "Please write a new optimized hook variation of type $hookType."
                )

                if (updatedHookText.isNotEmpty() && !updatedHookText.contains("APIFailure")) {
                    val hooks = data.optJSONArray("hooks")
                    if (hooks != null && hookIndex < hooks.length()) {
                        val hookObj = hooks.getJSONObject(hookIndex)
                        hookObj.put("text", updatedHookText)
                        
                        // Update UI and DB
                        val updatedJsonStr = data.toString()
                        repository.insert(
                            ViralPackage(
                                id = currentState.packageId,
                                title = sTopic,
                                platform = sPlatform,
                                niche = data.optString("niche", niche.value),
                                tone = sTone,
                                language = sLang,
                                duration = data.optString("duration", duration.value),
                                detailLevel = detailLevel.value,
                                contentJson = updatedJsonStr
                            )
                        )
                        _generationState.value = GenerationState.Success(currentState.packageId, JSONObject(updatedJsonStr))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to regenerate hook $hookType", e)
            }
        }
    }

    /**
     * Regenerates a single specific scene animation prompt inside the active generation state.
     */
    fun regenerateAnimationPrompt(sceneIndex: Int) {
        val currentState = _generationState.value
        if (currentState !is GenerationState.Success) return

        viewModelScope.launch {
            try {
                val data = currentState.data
                val scenes = data.optJSONArray("scenes") ?: return@launch
                if (sceneIndex >= scenes.length()) return@launch

                val sceneObj = scenes.getJSONObject(sceneIndex)
                val purpose = sceneObj.optString("purpose", "Scene")
                val narration = sceneObj.optString("narration", "")

                val updatedAnimPrompt = GeminiManager.regenerateSection(
                    topic = data.optString("topic", topic.value),
                    platform = data.optString("platform", platform.value),
                    tone = data.optString("tone", tone.value),
                    language = data.optString("language", language.value),
                    sectionType = "Animation camera prompt for Scene Purpose: $purpose",
                    contextInfo = "The scene has narration: $narration. Generate a highly detailed camera movement / animation instruction."
                )

                if (updatedAnimPrompt.isNotEmpty() && !updatedAnimPrompt.contains("APIFailure")) {
                    sceneObj.put("animationPrompt", updatedAnimPrompt)

                    val updatedJsonStr = data.toString()
                    repository.insert(
                        ViralPackage(
                            id = currentState.packageId,
                            title = data.optString("topic", topic.value),
                            platform = data.optString("platform", platform.value),
                            niche = data.optString("niche", niche.value),
                            tone = data.optString("tone", tone.value),
                            language = data.optString("language", language.value),
                            duration = data.optString("duration", duration.value),
                            detailLevel = detailLevel.value,
                            contentJson = updatedJsonStr
                        )
                    )
                    _generationState.value = GenerationState.Success(currentState.packageId, JSONObject(updatedJsonStr))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to regenerate scene animation prompt", e)
            }
        }
    }

    /**
     * Deletes a package from history.
     */
    fun deletePackage(packageId: Int) {
        viewModelScope.launch {
            repository.deleteById(packageId)
            // If the deleted package was active or selected, clear it
            val currentSelected = _selectedPackage.value
            if (currentSelected != null && currentSelected.id == packageId) {
                _selectedPackage.value = null
            }

            val currentState = _generationState.value
            if (currentState is GenerationState.Success && currentState.packageId == packageId) {
                _generationState.value = GenerationState.Idle
            }
        }
    }

    /**
     * Opens a saved historical package and renders it on Detail overlay view.
     */
    fun selectPackage(viralPackage: ViralPackage?) {
        _selectedPackage.value = viralPackage
        if (viralPackage != null) {
            // Load this historical package back into the Generator outcome so the user can edit/view it!
            try {
                _generationState.value = GenerationState.Success(viralPackage.id, JSONObject(viralPackage.contentJson))
            } catch (e: Exception) {
                Log.e(TAG, "Failed parsing saved content JSON", e)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
            _generationState.value = GenerationState.Idle
            _selectedPackage.value = null
        }
    }

    fun resetState() {
        _generationState.value = GenerationState.Idle
        _selectedPackage.value = null
    }
}
