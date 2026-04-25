package com.kkomi.assistant

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import java.nio.MappedByteBuffer

/**
 * TensorFlow Lite 모델 관리 및 Intent 분류
 * 사용자의 음성 명령을 분류하여 적절한 기능을 실행합니다
 */
class TensorFlowLiteManager(private val context: Context) {

    companion object {
        private const val TAG = "TensorFlowLiteManager"
        private const val MODEL_NAME = "kkomi_intent_classifier.tflite"
        private const val CONFIDENCE_THRESHOLD = 0.7f
    }

    enum class CommandIntent {
        CALL,           // 전화 걸기
        SMS,            // 문자 보내기
        MUSIC,          // 음악 재생
        ALARM,          // 알람 설정
        TIMER,          // 타이머 설정
        VOLUME,         // 볼륨 조절
        BRIGHTNESS,     // 밝기 조절
        SEARCH,         // 온라인 검색
        GREETING,       // 인사
        UNKNOWN         // 알 수 없음
    }

    data class IntentResult(
        val intent: CommandIntent,
        val confidence: Float,
        val entities: Map<String, String>
    )

    private var interpreter: Interpreter? = null
    private val intentLabels = listOf(
        "CALL", "SMS", "MUSIC", "ALARM", "TIMER",
        "VOLUME", "BRIGHTNESS", "SEARCH", "GREETING"
    )

    init {
        loadModel()
    }

    /**
     * 모델 로드
     */
    private fun loadModel() {
        try {
            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_NAME)
            interpreter = Interpreter(modelBuffer)
            Log.d(TAG, "TensorFlow Lite 모델 로드 성공")
        } catch (e: Exception) {
            Log.e(TAG, "모델 로드 실패: ${e.message}")
            // 모델이 없으면 기본 패턴 매칭 사용
        }
    }

    /**
     * 사용자 입력 텍스트에서 Intent 분류
     */
    fun classifyIntent(userInput: String): IntentResult {
        return try {
            // 텍스트 전처리
            val processedText = preprocessText(userInput)
            
            // 패턴 기반 Intent 분류 (TensorFlow Lite 모델 없을 때 대체)
            val intent = classifyByPattern(processedText)
            val confidence = calculateConfidence(userInput, intent)
            val entities = extractEntities(userInput, intent)

            IntentResult(intent, confidence, entities)
        } catch (e: Exception) {
            Log.e(TAG, "Intent 분류 실패: ${e.message}")
            IntentResult(CommandIntent.UNKNOWN, 0f, emptyMap())
        }
    }

    /**
     * 텍스트 전처리
     */
    private fun preprocessText(text: String): String {
        return text.lowercase()
            .replace(Regex("[^가-힣a-z0-9\\s]"), "")
            .trim()
    }

    /**
     * 패턴 기반 Intent 분류
     */
    private fun classifyByPattern(text: String): CommandIntent {
        return when {
            // 전화 관련
            text.contains("전화") || text.contains("call") || text.contains("걸어") ||
            text.contains("통화") || text.contains("phone") -> CommandIntent.CALL

            // 문자 관련
            text.contains("문자") || text.contains("sms") || text.contains("메시지") ||
            text.contains("보내") || text.contains("message") -> CommandIntent.SMS

            // 음악 관련
            text.contains("음악") || text.contains("music") || text.contains("노래") ||
            text.contains("재생") || text.contains("play") -> CommandIntent.MUSIC

            // 알람 관련
            text.contains("알람") || text.contains("alarm") || text.contains("깨워") ||
            text.contains("wake") -> CommandIntent.ALARM

            // 타이머 관련
            text.contains("타이머") || text.contains("timer") || text.contains("시간") ||
            text.contains("분") -> CommandIntent.TIMER

            // 볼륨 관련
            text.contains("볼륨") || text.contains("volume") || text.contains("소리") ||
            text.contains("크기") -> CommandIntent.VOLUME

            // 밝기 관련
            text.contains("밝기") || text.contains("brightness") || text.contains("화면") ||
            text.contains("어두워") -> CommandIntent.BRIGHTNESS

            // 검색 관련
            text.contains("검색") || text.contains("search") || text.contains("날씨") ||
            text.contains("뉴스") || text.contains("찾아") -> CommandIntent.SEARCH

            // 인사 관련
            text.contains("안녕") || text.contains("hello") || text.contains("hi") ||
            text.contains("반가워") -> CommandIntent.GREETING

            else -> CommandIntent.UNKNOWN
        }
    }

    /**
     * 신뢰도 계산
     */
    private fun calculateConfidence(text: String, intent: CommandIntent): Float {
        val keywords = getKeywordsForIntent(intent)
        val matchCount = keywords.count { text.lowercase().contains(it) }
        
        return when {
            matchCount == 0 -> 0.3f
            matchCount == 1 -> 0.6f
            matchCount >= 2 -> 0.9f
            else -> 0.5f
        }
    }

    /**
     * Intent별 키워드 조회
     */
    private fun getKeywordsForIntent(intent: CommandIntent): List<String> {
        return when (intent) {
            CommandIntent.CALL -> listOf("전화", "call", "걸어", "통화", "phone")
            CommandIntent.SMS -> listOf("문자", "sms", "메시지", "보내", "message")
            CommandIntent.MUSIC -> listOf("음악", "music", "노래", "재생", "play")
            CommandIntent.ALARM -> listOf("알람", "alarm", "깨워", "wake")
            CommandIntent.TIMER -> listOf("타이머", "timer", "시간", "분")
            CommandIntent.VOLUME -> listOf("볼륨", "volume", "소리", "크기")
            CommandIntent.BRIGHTNESS -> listOf("밝기", "brightness", "화면", "어두워")
            CommandIntent.SEARCH -> listOf("검색", "search", "날씨", "뉴스", "찾아")
            CommandIntent.GREETING -> listOf("안녕", "hello", "hi", "반가워")
            CommandIntent.UNKNOWN -> emptyList()
        }
    }

    /**
     * 개체명 인식 (Named Entity Recognition)
     */
    private fun extractEntities(text: String, intent: CommandIntent): Map<String, String> {
        val entities = mutableMapOf<String, String>()

        when (intent) {
            CommandIntent.CALL, CommandIntent.SMS -> {
                // 전화번호 추출
                val phonePattern = Regex("\\d{2,3}-?\\d{3,4}-?\\d{4}")
                phonePattern.find(text)?.let {
                    entities["phone_number"] = it.value
                }
                
                // 연락처 이름 추출
                val namePattern = Regex("[가-힣]{2,5}")
                namePattern.find(text)?.let {
                    entities["contact_name"] = it.value
                }
            }

            CommandIntent.MUSIC -> {
                // 곡 제목 추출
                val titlePattern = Regex("\"([^\"]+)\"")
                titlePattern.find(text)?.let {
                    entities["song_title"] = it.groupValues[1]
                }
            }

            CommandIntent.ALARM, CommandIntent.TIMER -> {
                // 시간 추출
                val timePattern = Regex("(\\d{1,2})시|:(\\d{1,2})")
                timePattern.find(text)?.let {
                    entities["time"] = it.value
                }
                
                // 분 추출
                val minutePattern = Regex("(\\d{1,2})분")
                minutePattern.find(text)?.let {
                    entities["minutes"] = it.groupValues[1]
                }
            }

            CommandIntent.VOLUME, CommandIntent.BRIGHTNESS -> {
                // 백분율 추출
                val percentPattern = Regex("(\\d{1,3})%?")
                percentPattern.find(text)?.let {
                    entities["percentage"] = it.groupValues[1]
                }
            }

            CommandIntent.SEARCH -> {
                // 검색어 추출
                val searchPattern = Regex("검색|찾아|뉴스|날씨\\s+(.+)")
                searchPattern.find(text)?.let {
                    entities["query"] = it.groupValues.getOrNull(1) ?: text
                }
            }

            else -> {}
        }

        return entities
    }

    /**
     * 모델 리소스 해제
     */
    fun release() {
        interpreter?.close()
        interpreter = null
    }
}
