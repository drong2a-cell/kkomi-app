package com.kkomi.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tvGreeting: TextView
    private lateinit var tvStatus: TextView
    private lateinit var etInput: EditText
    private lateinit var btnMicrophone: Button
    private lateinit var btnSettings: ImageButton
    private lateinit var ivKkomi: ImageView
    private lateinit var llRecentTasks: LinearLayout

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var kkomiAnimationManager: KkomiAnimationManager
    private lateinit var deviceControlManager: DeviceControlManager
    private lateinit var safetyManager: SafetyManager
    private lateinit var database: KkomiDatabase
    private lateinit var tensorFlowLiteManager: TensorFlowLiteManager
    private lateinit var perplexitySearchManager: PerplexitySearchManager
    private lateinit var voiceProfileManager: VoiceProfileManager

    private var isListening = false
    private var isSpeaking = false

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 100
        private const val REQUEST_CODE_SPEECH_INPUT = 101
        private const val PERPLEXITY_API_KEY = "your_api_key_here" // 환경변수에서 로드

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI 요소 초기화
        initializeUI()

        // 매니저 초기화
        initializeManagers()

        // 권한 확인
        checkPermissions()

        // TTS 초기화
        textToSpeech = TextToSpeech(this, this)

        // 이벤트 리스너 설정
        setupListeners()

        // 인사말
        speakResponse("안녕하세요! 저는 꼬미입니다. 무엇을 도와드릴까요?")
    }

    private fun initializeUI() {
        tvGreeting = findViewById(R.id.tv_greeting)
        tvStatus = findViewById(R.id.tv_status)
        etInput = findViewById(R.id.et_input)
        btnMicrophone = findViewById(R.id.btn_microphone)
        btnSettings = findViewById(R.id.btn_settings)
        ivKkomi = findViewById(R.id.iv_kkomi)
        llRecentTasks = findViewById(R.id.ll_recent_tasks)
    }

    private fun initializeManagers() {
        kkomiAnimationManager = KkomiAnimationManager(ivKkomi)
        deviceControlManager = DeviceControlManager(this)
        safetyManager = SafetyManager(this)
        database = KkomiDatabase(this)
        tensorFlowLiteManager = TensorFlowLiteManager(this)
        perplexitySearchManager = PerplexitySearchManager(this, database)
        voiceProfileManager = VoiceProfileManager(this, database)

        // 초기 애니메이션
        kkomiAnimationManager.changeState(KkomiAnimationManager.AnimationState.IDLE)
    }

    private fun setupListeners() {
        btnMicrophone.setOnClickListener {
            if (!isListening) {
                startSpeechRecognition()
            }
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                val userInput = etInput.text.toString().trim()
                if (userInput.isNotEmpty()) {
                    processUserCommand(userInput)
                    etInput.text.clear()
                }
                true
            } else {
                false
            }
        }
    }

    private fun checkPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                Toast.makeText(this, "필수 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startSpeechRecognition() {
        isListening = true
        tvStatus.text = "듣고 있어요..."
        kkomiAnimationManager.changeState(KkomiAnimationManager.AnimationState.LISTENING)
        btnMicrophone.isEnabled = false

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "말씀해주세요...")
        }

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } catch (e: Exception) {
            Toast.makeText(this, "음성 인식을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
            isListening = false
            btnMicrophone.isEnabled = true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val userSpeech = results[0]
                etInput.setText(userSpeech)
                processUserCommand(userSpeech)
            }
        }

        isListening = false
        tvStatus.text = "무엇을 도와드릴까요?"
        kkomiAnimationManager.changeState(KkomiAnimationManager.AnimationState.IDLE)
        btnMicrophone.isEnabled = true
    }

    private fun processUserCommand(userInput: String) {
        tvStatus.text = "생각 중이에요..."
        kkomiAnimationManager.changeState(KkomiAnimationManager.AnimationState.PROCESSING)

        // 1. TensorFlow Lite로 Intent 분류
        val intentResult = tensorFlowLiteManager.classifyIntent(userInput)
        val intent = intentResult.intent
        val entities = intentResult.entities

        // 2. 음성 프로필 학습
        voiceProfileManager.learnUserPreferences(intent.name, true)

        // 3. 명령 실행
        when (intent) {
            TensorFlowLiteManager.CommandIntent.CALL -> {
                val phoneNumber = entities["phone_number"] ?: entities["contact_name"] ?: ""
                if (phoneNumber.isNotEmpty()) {
                    safetyManager.showConfirmationDialog(
                        "전화 걸기",
                        "$phoneNumber 로 전화하시겠어요?",
                        { deviceControlManager.makeCall(phoneNumber) { success ->
                            if (success) speakResponse("전화를 걸고 있어요.")
                        } },
                        { speakResponse("전화 걸기를 취소했어요.") }
                    )
                } else {
                    speakResponse("전화번호를 말씀해주세요.")
                }
            }

            TensorFlowLiteManager.CommandIntent.SMS -> {
                val phoneNumber = entities["phone_number"] ?: entities["contact_name"] ?: ""
                if (phoneNumber.isNotEmpty()) {
                    safetyManager.showConfirmationDialog(
                        "문자 보내기",
                        "$phoneNumber 로 문자를 보내시겠어요?",
                        { deviceControlManager.sendSms(phoneNumber, userInput) { success ->
                            if (success) speakResponse("문자를 보냈어요.")
                        } },
                        { speakResponse("문자 보내기를 취소했어요.") }
                    )
                } else {
                    speakResponse("전화번호를 말씀해주세요.")
                }
            }

            TensorFlowLiteManager.CommandIntent.MUSIC -> {
                val songTitle = entities["song_title"] ?: userInput
                deviceControlManager.playMusic(songTitle)
                speakResponse("음악을 재생하고 있어요.")
            }

            TensorFlowLiteManager.CommandIntent.ALARM -> {
                val time = entities["time"] ?: "오전 7시"
                deviceControlManager.setAlarm(7, 0)
                speakResponse("알람을 설정했어요.")
            }

            TensorFlowLiteManager.CommandIntent.TIMER -> {
                val minutes = entities["minutes"]?.toIntOrNull() ?: 5
                deviceControlManager.setTimer(minutes)
                speakResponse("$minutes 분 타이머를 설정했어요.")
            }

            TensorFlowLiteManager.CommandIntent.VOLUME -> {
                val percentage = entities["percentage"]?.toIntOrNull() ?: 50
                val limitedVolume = safetyManager.checkAndLimitVolume(percentage)
                deviceControlManager.adjustVolume(limitedVolume)
                speakResponse("볼륨을 ${limitedVolume}%로 설정했어요.")
            }

            TensorFlowLiteManager.CommandIntent.BRIGHTNESS -> {
                val percentage = entities["percentage"]?.toIntOrNull() ?: 50
                deviceControlManager.adjustBrightness(percentage)
                speakResponse("화면 밝기를 ${percentage}%로 설정했어요.")
            }

            TensorFlowLiteManager.CommandIntent.SEARCH -> {
                val query = entities["query"] ?: userInput
                lifecycleScope.launch {
                    val result = perplexitySearchManager.search(query, PERPLEXITY_API_KEY)
                    if (result.success) {
                        speakResponse(result.result)
                    } else {
                        speakResponse("검색 중 오류가 발생했어요. ${result.result}")
                    }
                }
            }

            TensorFlowLiteManager.CommandIntent.GREETING -> {
                val personalizedResponse = voiceProfileManager.generatePersonalizedResponse(
                    "안녕하세요! 저는 꼬미입니다."
                )
                speakResponse(personalizedResponse)
            }

            TensorFlowLiteManager.CommandIntent.UNKNOWN -> {
                speakResponse("죄송해요. 그 명령어는 아직 지원하지 않아요.")
            }
        }

        tvStatus.text = "무엇을 도와드릴까요?"
        kkomiAnimationManager.changeState(KkomiAnimationManager.AnimationState.IDLE)

        // 최근 작업에 추가
        addRecentTask(userInput)

        // 데이터베이스에 기록
        database.saveConversation(userInput, "", intent.name, intentResult.confidence)
    }

    private fun addRecentTask(task: String) {
        val taskView = TextView(this).apply {
            text = "• $task"
            textSize = 14f
            setPadding(8, 4, 8, 4)
        }
        llRecentTasks.addView(taskView, 0)

        if (llRecentTasks.childCount > 5) {
            llRecentTasks.removeViewAt(llRecentTasks.childCount - 1)
        }
    }

    private fun speakResponse(response: String) {
        if (!isSpeaking) {
            isSpeaking = true
            kkomiAnimationManager.changeState(KkomiAnimationManager.AnimationState.EXECUTING)
            textToSpeech.speak(response, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.KOREAN
            textToSpeech.pitch = 1.3f
            textToSpeech.setSpeechRate(0.9f)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::textToSpeech.isInitialized) {
            textToSpeech.shutdown()
        }
        kkomiAnimationManager.release()
        tensorFlowLiteManager.release()
        database.close()
    }
}
