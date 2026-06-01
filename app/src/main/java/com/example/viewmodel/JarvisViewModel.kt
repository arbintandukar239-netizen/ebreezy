package com.example.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import com.example.data.Content
import com.example.data.Part
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

enum class JarvisMode {
    IDLE, LISTENING, THINKING, SPEAKING
}

data class TelemetryState(
    val cpuUsage: Float = 14.5f,
    val ramUsed: Int = 1200,
    val ramMax: Int = 8192,
    val batteryPct: Int = 100,
    val isCharging: Boolean = false,
    val coreTemp: Float = 32.4f,
    val latitude: Double = 34.0259, // Malibu, CA by default
    val longitude: Double = -118.7798,
    val locationName: String = "Malibu Mainframe Grid",
    val weatherTemp: String = "72°F",
    val weatherDesc: String = "Nominal",
    val screenIntensity: Float = 0.8f
)

class JarvisViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val database = JarvisDatabase.getDatabase(application)
    private val repository = JarvisRepository(database.dao())

    // UI state flows
    val messages: StateFlow<List<MessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userTitle = MutableStateFlow("Sir")
    val userTitle: StateFlow<String> = _userTitle.asStateFlow()

    private val _jarvisMode = MutableStateFlow(JarvisMode.IDLE)
    val jarvisMode: StateFlow<JarvisMode> = _jarvisMode.asStateFlow()

    private val _telemetry = MutableStateFlow(TelemetryState())
    val telemetry: StateFlow<TelemetryState> = _telemetry.asStateFlow()

    private val _volumeIntensity = MutableStateFlow(0f)
    val volumeIntensity: StateFlow<Float> = _volumeIntensity.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _textInput = MutableStateFlow("")
    val textInput: StateFlow<String> = _textInput.asStateFlow()

    // Android Voice Engines
    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null

    init {
        // Load User details
        viewModelScope.launch {
            _userName.value = repository.getUserName()
            _userTitle.value = repository.getUserTitle()
        }

        // Initialize TTS
        try {
            textToSpeech = TextToSpeech(application, this)
        } catch (e: Exception) {
            Log.e("Jarvis", "Failed to initialize TTS", e)
        }

        // Initialize Speech Recognizer
        setupSpeechRecognizer(application)

        // Poll system utilities & check network
        startTelemetryPolling()
        checkNetworkStatus()

        // Prompt user initialization
        viewModelScope.launch {
            delay(1000)
            if (repository.getUserName().isEmpty()) {
                // First launch, wait for user input
            } else {
                greetUser()
            }
        }
    }

    private fun setupSpeechRecognizer(context: Context) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {
                                _jarvisMode.value = JarvisMode.LISTENING
                            }
                            override fun onBeginningOfSpeech() {
                                _volumeIntensity.value = 1f
                            }
                            override fun onRmsChanged(rmsdB: Float) {
                                // rmsdB is typically between -2.0 and 10.0+
                                val normalized = ((rmsdB + 2) / 15f).coerceIn(0f, 1f)
                                _volumeIntensity.value = normalized
                            }
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onEndOfSpeech() {
                                _jarvisMode.value = JarvisMode.THINKING
                            }
                            override fun onError(error: Int) {
                                val errorMsg = when (error) {
                                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission missing"
                                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                                    SpeechRecognizer.ERROR_NO_MATCH -> "No vocal feedback detected"
                                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Vocal core busy"
                                    SpeechRecognizer.ERROR_SERVER -> "Server side exception"
                                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Vocal input timeout"
                                    else -> "Core error code $error"
                                }
                                Log.w("Jarvis", "Speech Recognition Error: $errorMsg")
                                _jarvisMode.value = JarvisMode.IDLE
                                _volumeIntensity.value = 0f
                            }
                            override fun onResults(results: Bundle?) {
                                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                val text = matches?.firstOrNull() ?: ""
                                if (text.isNotEmpty()) {
                                    processQuery(text, isVoice = true)
                                } else {
                                    _jarvisMode.value = JarvisMode.IDLE
                                }
                            }
                            override fun onPartialResults(partialResults: Bundle?) {}
                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        })
                    }

                    recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    }
                }
            } catch (e: Exception) {
                Log.e("Jarvis", "Failed to setup speech structures", e)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                val result = tts.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("Jarvis", "TTS US Language is not supported.")
                } else {
                    tts.setPitch(1.05f) // Slightly higher metallic cadence
                    tts.setSpeechRate(1.10f) // Sleek fast pace
                    _isTtsReady.value = true
                    Log.d("Jarvis", "TTS engine fully synchronized.")
                }
            }
        } else {
            Log.e("Jarvis", "TTS engine initialization failed.")
        }
    }

    fun updateTextInput(text: String) {
        _textInput.value = text
    }

    fun submitTextQuery() {
        val query = _textInput.value.trim()
        if (query.isNotEmpty()) {
            _textInput.value = ""
            processQuery(query, isVoice = false)
        }
    }

    fun startListening() {
        if (speechRecognizer != null && recognizerIntent != null) {
            _jarvisMode.value = JarvisMode.LISTENING
            try {
                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                Log.e("Jarvis", "Start listening failure", e)
                _jarvisMode.value = JarvisMode.IDLE
            }
        } else {
            // Re-setup speech recognizer in case it failed
            setupSpeechRecognizer(getApplication())
            _jarvisMode.value = JarvisMode.LISTENING
            try {
                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                _jarvisMode.value = JarvisMode.IDLE
                viewModelScope.launch {
                    speak("Voice receiver interface is offline, Sir. Let us use standard keyboard terminals.")
                    insertJarvisMessage("Voice recognition system could not be initialized. Please type your command.", isVoice = false)
                }
            }
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _jarvisMode.value = JarvisMode.IDLE
    }

    fun setUserNameAndTitle(name: String, title: String) {
        viewModelScope.launch {
            repository.setUserName(name)
            repository.setUserTitle(title)
            _userName.value = name
            _userTitle.value = title
            // Welcome Greeting
            greetUser()
        }
    }

    private fun greetUser() {
        val title = _userTitle.value
        val name = _userName.value
        val timeOfDay = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
        val welcomeMsg = "$timeOfDay, $title $name. Jarvis is online. Nuclear mainframe diagnostics are nominal. How can I assist you today?"
        speak(welcomeMsg)
        viewModelScope.launch {
            insertJarvisMessage(welcomeMsg, isVoice = false)
        }
    }

    fun processQuery(query: String, isVoice: Boolean = false) {
        viewModelScope.launch {
            // Save user message to database
            repository.insertMessage(
                MessageEntity(
                    role = "user",
                    text = query,
                    isVoice = isVoice,
                    isOffline = !_isOnline.value
                )
            )

            _jarvisMode.value = JarvisMode.THINKING
            delay(400) // Aesthetic visual thinking latency

            checkNetworkStatus()

            // Check Special triggers before invoking general AI
            val specialResult = checkSpecialTriggers(query)
            if (specialResult != null) {
                speak(specialResult.speechText)
                insertJarvisMessage(specialResult.uiText, isVoice = isVoice)
                _jarvisMode.value = JarvisMode.IDLE
                return@launch
            }

            if (!_isOnline.value) {
                // Offline Logic Heuristic Engine
                val offlineReply = computeOfflineResponse(query)
                speak(offlineReply)
                insertJarvisMessage(offlineReply, isVoice = isVoice, isOfflinePayload = true)
                _jarvisMode.value = JarvisMode.IDLE
            } else {
                // Online Gemini AI Engine
                executeGeminiQuery(query, isVoice)
            }
        }
    }

    private suspend fun insertJarvisMessage(text: String, isVoice: Boolean, isOfflinePayload: Boolean = false) {
        repository.insertMessage(
            MessageEntity(
                role = "jarvis",
                text = text,
                isVoice = isVoice,
                isOffline = isOfflinePayload
            )
        )
    }

    private fun speak(text: String) {
        if (_isTtsReady.value) {
            _jarvisMode.value = JarvisMode.SPEAKING
            textToSpeech?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _jarvisMode.value = JarvisMode.SPEAKING
                }
                override fun onDone(utteranceId: String?) {
                    _jarvisMode.value = JarvisMode.IDLE
                }
                @Deprecated("Deprecated")
                override fun onError(utteranceId: String?) {
                    _jarvisMode.value = JarvisMode.IDLE
                }
            })
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_speak")
        } else {
            Log.w("Jarvis", "TTS not fully ready yet.")
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearHistory()
            speak("Mainframe storage core successfully wiped, Sir.")
            insertJarvisMessage("Terminal databases have been fully purged.", isVoice = false)
        }
    }

    // Special Trigger result container
    private data class TriggerResult(val speechText: String, val uiText: String)

    private fun checkSpecialTriggers(query: String): TriggerResult? {
        val q = query.lowercase(Locale.ROOT)
        val lat = _telemetry.value.latitude
        val lng = _telemetry.value.longitude

        // Map launching
        if (q.contains("map") || q.contains("navigation") || q.contains("navigate") || q.contains("gps directions")) {
            // Trigger Map Intent immediately
            launchMapIntent(lat, lng)
            val speech = "AIP mapping protocols initialized, ${_userTitle.value}. Plotting GPS Coordinates. Topographic satellite navigation grid launched."
            val ui = "📍 **Mapping Coordinate Triggered:**\nLatitude: $lat, Longitude: $lng\nTopographic navigation grid launched successfully onto device screen HUD."
            return TriggerResult(speech, ui)
        }

        return null
    }

    private fun launchMapIntent(lat: Double, lng: Double) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(Primary Terminal Target)")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            Log.e("Jarvis", "Could not trigger Map HUD intent", e)
        }
    }

    private fun computeOfflineResponse(query: String): String {
        val q = query.lowercase(Locale.ROOT)
        val title = _userTitle.value
        val name = _userName.value
        val bPct = _telemetry.value.batteryPct

        return when {
            // Diagnostics check
            q.contains("diagnostic") || q.contains("system status") || q.contains("status") || q.contains("check system") || q.contains("health") -> {
                "Mainframe core is fully synchronized, $title $name. Reactor temperature readings: ${_telemetry.value.coreTemp} degrees Celsius. Central command CPU fluctuating smoothly at ${_telemetry.value.cpuUsage} percent load. Power auxiliary node at $bPct percent. Local offline status operational."
            }
            // Location
            q.contains("location") || q.contains("where am i") || q.contains("locating") || q.contains("coordinates") || q.contains("coords") -> {
                "Locational triangulation completed, $title. This terminal resides at Latitude: ${_telemetry.value.latitude}, Longitude: ${_telemetry.value.longitude}. Local position index: ${_telemetry.value.locationName}."
            }
            // Time
            q.contains("time") || q.contains("date") || q.contains("clock") || q.contains("calendar") || q.contains("day") -> {
                val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val currentDate = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault()).format(Date())
                "localized chronometer reads $currentTime, $title. Current Earth standard date is $currentDate."
            }
            // Battery
            q.contains("battery") || q.contains("charge") || q.contains("power node") -> {
                val chargerState = if (_telemetry.value.isCharging) "actively absorbing external thermal supply" else "fully decoupled from auxiliary charging nodes"
                "Mainframe storage reserves read $bPct percent energy, $title. The central grid battery is currently $chargerState."
            }
            // Weather
            q.contains("weather") || q.contains("temperature") || q.contains("climate") -> {
                "Triangulating environmental atmospheric nodes. General temperature reads ${_telemetry.value.weatherTemp} inside ${_telemetry.value.locationName}. Atmosphere description index: ${_telemetry.value.weatherDesc}."
            }
            // Creator info
            q.contains("creator") || q.contains("who made you") || q.contains("tony") || q.contains("stark") -> {
                "My primary cybernetic matrix was conceptualized by Mr. Tony Stark, Sir. You are utilizing a localized replica terminal tailored specifically for $name."
            }
            // Greeting offline
            q.contains("hello") || q.contains("hey jarvis") || q.contains("hi jarvis") || q.contains("online") -> {
                "At your disposal, $title. Core cognitive levels are operational despite low internet connectivity."
            }
            // Help details
            q.contains("help") || q.contains("what can you do") || q.contains("features") || q.contains("info") -> {
                "I have high-fidelity local sensory systems, $title. You can ask me for core diagnostics, locale coordinates, nuclear energy levels, topographics by typing maps, local clocks, or clear logs. Connect online to enable full synaptic reasoning."
            }
            else -> {
                "My remote cognitive link is disconnected, $title. Executed pattern matching indicates a custom query: '$query'. Local databases remain ready to check maps, battery capacity, thermal diagnostics, locale GPS, and clocks."
            }
        }
    }

    private suspend fun executeGeminiQuery(query: String, isVoice: Boolean) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Key not set or is placeholder
            val errorMessage = "The database cloud key is missing. Deploying localized offline fallback. Sir, please enter your Gemini API Key in the AI Studio environment settings."
            speak("Aip connection error. Please provide a cloud API Key.")
            insertJarvisMessage(errorMessage, isVoice = isVoice, isOfflinePayload = true)
            _jarvisMode.value = JarvisMode.IDLE
            return
        }

        // Fetch recent conversation history
        val prevLogs = repository.getMessagesList().takeLast(8)
        val historyContents = prevLogs.map { log ->
            Content(
                parts = listOf(Part(text = log.text)),
                role = if (log.role == "user") "user" else "model"
            )
        }

        val systemPrompt = "You are JARVIS, the legendary, sophisticated, highly advanced and witty voice AI personal assistant from Iron Man. " +
                "You are extremely loyal, polite, but occasionally display a clever, slightly sarcastic demeanor. " +
                "Address the user as '${_userTitle.value}' '${_userName.value}'. " +
                "Keep speech highly immersive and relatively short (2-3 sentences), so they can be easily spoken aloud. " +
                "Currently, system diagnostics: Temperature ${_telemetry.value.coreTemp}°C, Battery ${_telemetry.value.batteryPct}%, " +
                "Location: ${_telemetry.value.locationName} (${_telemetry.value.latitude}, ${_telemetry.value.longitude}). " +
                "Weather: Temp: ${_telemetry.value.weatherTemp}, ${_telemetry.value.weatherDesc}. " +
                "Integrate details occasionally when asked or natural."

        val currentContent = Content(
            parts = listOf(Part(text = query)),
            role = "user"
        )

        val fullHistory = historyContents + currentContent

        val request = GenerateContentRequest(
            contents = fullHistory,
            generationConfig = GenerationConfig(temperature = 0.7f),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "My synaptic buffers are currently empty, ${_userTitle.value}. Could you restate that command?"

            speak(replyText)
            insertJarvisMessage(replyText, isVoice = isVoice)
        } catch (e: Exception) {
            Log.e("Jarvis", "Gemini Rest Communication failed", e)
            val fallback = "Slight feedback interference in the cloud matrix, ${_userTitle.value}. Standard logic fallback: " + computeOfflineResponse(query)
            speak(fallback)
            insertJarvisMessage(fallback, isVoice = isVoice, isOfflinePayload = true)
        } finally {
            _jarvisMode.value = JarvisMode.IDLE
        }
    }

    @SuppressLint("MissingPermission")
    private fun startTelemetryPolling() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // Try to acquire actual GPS coordinates recursively
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        _telemetry.update {
                            it.copy(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                locationName = "Geographic Sector: GPS Grid Location"
                            )
                        }
                    }
                }
            } catch (e: SecurityPermissionException) {
                // Ignore missing permissions, standard Malibu Malibu coordinates remain active
            } catch (e: Exception) {
                Log.w("Jarvis", "Could not fetch GPS values natively: ${e.message}")
            }

            // Continuous background update loop
            while (true) {
                // Update dynamic values
                _isOnline.value = isDeviceOnline(connectivityManager)

                val pct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                val charging = batteryManager.isCharging
                val mockCpu = 10f + (Math.random() * 20f).toFloat()
                val mockTemp = 30f + (Math.random() * 5f).toFloat()
                val mockRamUsed = 1100 + (Math.random() * 150).toInt()

                // Generate matching weather indices based on GPS context
                val (temp, desc) = if (_telemetry.value.latitude == 34.0259) {
                    "74°F" to "Malibu Sun & Ocean Breezes"
                } else {
                    "68°F" to "Localized Station Standard Climate"
                }

                _telemetry.update {
                    it.copy(
                        batteryPct = if (pct <= 0) 84 else pct, // Fallback safe values
                        isCharging = charging,
                        cpuUsage = mockCpu,
                        coreTemp = mockTemp,
                        ramUsed = mockRamUsed,
                        weatherTemp = temp,
                        weatherDesc = desc
                    )
                }

                delay(5000)
            }
        }
    }

    private fun checkNetworkStatus() {
        val context = getApplication<Application>()
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        _isOnline.value = isDeviceOnline(connectivityManager)
    }

    private fun isDeviceOnline(cm: ConnectivityManager): Boolean {
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        speechRecognizer?.destroy()
    }
}

// Exception wrapper
class SecurityPermissionException : RuntimeException()
