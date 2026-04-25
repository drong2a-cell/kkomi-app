package com.kkomi.assistant

import android.content.Context
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlin.math.sqrt

/**
 * 사용자 음성 학습 및 개인화 기능
 * 사용자의 음성 특징을 분석하고 학습하여 개인화된 경험을 제공합니다
 */
class VoiceProfileManager(
    private val context: Context,
    private val database: KkomiDatabase
) {

    companion object {
        private const val TAG = "VoiceProfileManager"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = android.media.AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = android.media.AudioFormat.ENCODING_PCM_16BIT
        private const val MIN_SAMPLES_FOR_PROFILE = 5
    }

    data class VoiceProfile(
        val pitch: Float,           // 음성 음높이 (Hz)
        val speechRate: Float,      // 음성 속도 (단어/초)
        val accent: String,         // 방언/억양
        val confidence: Float,      // 신뢰도
        val sampleCount: Int        // 수집된 샘플 수
    )

    /**
     * 음성 프로필 생성 및 학습
     */
    fun analyzeVoiceProfile(audioData: ByteArray): VoiceProfile {
        try {
            // 음성 특징 추출
            val pitch = extractPitch(audioData)
            val speechRate = estimateSpeechRate(audioData)
            val accent = detectAccent(audioData)
            val confidence = calculateConfidence(audioData)

            val profile = VoiceProfile(
                pitch = pitch,
                speechRate = speechRate,
                accent = accent,
                confidence = confidence,
                sampleCount = getSampleCount() + 1
            )

            // 프로필 저장
            saveVoiceProfile(profile, audioData)

            Log.d(TAG, "음성 프로필 분석 완료: Pitch=$pitch, SpeechRate=$speechRate")
            return profile
        } catch (e: Exception) {
            Log.e(TAG, "음성 프로필 분석 실패: ${e.message}")
            return VoiceProfile(0f, 0f, "unknown", 0f, 0)
        }
    }

    /**
     * 음성 음높이 추출 (기본 주파수)
     */
    private fun extractPitch(audioData: ByteArray): Float {
        try {
            // PCM 데이터를 short 배열로 변환
            val shortData = ByteArray(audioData.size)
            for (i in audioData.indices step 2) {
                shortData[i / 2] = audioData[i].toInt().toShort().toByte()
            }

            // 에너지 기반 음높이 추정
            val energy = calculateEnergy(audioData)
            val zcrRate = calculateZeroCrossingRate(audioData)

            // 음높이 범위: 80Hz ~ 400Hz (일반적인 음성 범위)
            val basePitch = 100f + (energy * 200f)
            val adjustedPitch = basePitch * (1f + zcrRate)

            return adjustedPitch.coerceIn(80f, 400f)
        } catch (e: Exception) {
            Log.e(TAG, "음높이 추출 실패: ${e.message}")
            return 150f // 기본값
        }
    }

    /**
     * 에너지 계산
     */
    private fun calculateEnergy(audioData: ByteArray): Float {
        var energy = 0f
        for (i in audioData.indices step 2) {
            val sample = (audioData[i].toInt() or (audioData[i + 1].toInt() shl 8)).toShort()
            energy += (sample * sample).toFloat()
        }
        return energy / audioData.size
    }

    /**
     * Zero Crossing Rate 계산
     */
    private fun calculateZeroCrossingRate(audioData: ByteArray): Float {
        var zeroCrossingCount = 0
        for (i in 1 until audioData.size - 1 step 2) {
            val sample1 = (audioData[i - 1].toInt() or (audioData[i].toInt() shl 8)).toShort()
            val sample2 = (audioData[i + 1].toInt() or (audioData[i + 2].toInt() shl 8)).toShort()

            if ((sample1 < 0 && sample2 >= 0) || (sample1 >= 0 && sample2 < 0)) {
                zeroCrossingCount++
            }
        }
        return zeroCrossingCount.toFloat() / (audioData.size / 2)
    }

    /**
     * 음성 속도 추정
     */
    private fun estimateSpeechRate(audioData: ByteArray): Float {
        try {
            // 음성 활동 감지 (VAD)
            val voiceActivity = detectVoiceActivity(audioData)
            
            // 음성 구간의 길이 계산
            val voiceFrames = voiceActivity.count { it }
            val totalFrames = voiceActivity.size
            val voiceRatio = voiceFrames.toFloat() / totalFrames

            // 음성 속도 추정 (단어/초)
            // 일반적인 음성 속도: 2.5 ~ 4.5 단어/초
            val baseRate = 3.5f
            val adjustedRate = baseRate * voiceRatio

            return adjustedRate.coerceIn(2.5f, 4.5f)
        } catch (e: Exception) {
            Log.e(TAG, "음성 속도 추정 실패: ${e.message}")
            return 3.5f // 기본값
        }
    }

    /**
     * 음성 활동 감지 (Voice Activity Detection)
     */
    private fun detectVoiceActivity(audioData: ByteArray): BooleanArray {
        val frameSize = SAMPLE_RATE / 100 // 10ms 프레임
        val frameCount = audioData.size / (frameSize * 2)
        val voiceActivity = BooleanArray(frameCount)

        val energyThreshold = calculateEnergyThreshold(audioData)

        for (i in 0 until frameCount) {
            val frameStart = i * frameSize * 2
            val frameEnd = (frameStart + frameSize * 2).coerceAtMost(audioData.size)
            val frameEnergy = calculateFrameEnergy(audioData, frameStart, frameEnd)
            voiceActivity[i] = frameEnergy > energyThreshold
        }

        return voiceActivity
    }

    /**
     * 프레임 에너지 계산
     */
    private fun calculateFrameEnergy(audioData: ByteArray, start: Int, end: Int): Float {
        var energy = 0f
        for (i in start until end step 2) {
            if (i + 1 < audioData.size) {
                val sample = (audioData[i].toInt() or (audioData[i + 1].toInt() shl 8)).toShort()
                energy += (sample * sample).toFloat()
            }
        }
        return energy / ((end - start) / 2)
    }

    /**
     * 에너지 임계값 계산
     */
    private fun calculateEnergyThreshold(audioData: ByteArray): Float {
        val totalEnergy = calculateEnergy(audioData)
        return totalEnergy * 0.3f // 전체 에너지의 30%를 임계값으로 설정
    }

    /**
     * 방언/억양 감지
     */
    private fun detectAccent(audioData: ByteArray): String {
        try {
            val pitch = extractPitch(audioData)
            val speechRate = estimateSpeechRate(audioData)

            // 음높이와 속도 조합으로 억양 분류
            return when {
                pitch > 200f && speechRate > 3.8f -> "높고빠름"
                pitch > 200f && speechRate < 3.0f -> "높고느림"
                pitch < 150f && speechRate > 3.8f -> "낮고빠름"
                pitch < 150f && speechRate < 3.0f -> "낮고느림"
                else -> "표준"
            }
        } catch (e: Exception) {
            Log.e(TAG, "억양 감지 실패: ${e.message}")
            return "unknown"
        }
    }

    /**
     * 신뢰도 계산
     */
    private fun calculateConfidence(audioData: ByteArray): Float {
        try {
            val sampleCount = getSampleCount()
            val baseConfidence = 0.5f + (sampleCount * 0.05f) // 샘플당 5% 증가
            
            // 신뢰도 범위: 0.5 ~ 0.95
            return baseConfidence.coerceIn(0.5f, 0.95f)
        } catch (e: Exception) {
            Log.e(TAG, "신뢰도 계산 실패: ${e.message}")
            return 0.5f
        }
    }

    /**
     * 음성 프로필 저장
     */
    private fun saveVoiceProfile(profile: VoiceProfile, audioData: ByteArray) {
        try {
            val db = database.writableDatabase
            val values = android.content.ContentValues().apply {
                put("voice_feature_vector", audioData)
                put("pitch", profile.pitch)
                put("speech_rate", profile.speechRate)
                put("accent", profile.accent)
                put("confidence", profile.confidence)
            }
            db.insert(KkomiDatabase.TABLE_VOICE_PROFILES, null, values)
            Log.d(TAG, "음성 프로필 저장 완료")
        } catch (e: Exception) {
            Log.e(TAG, "음성 프로필 저장 실패: ${e.message}")
        }
    }

    /**
     * 저장된 샘플 수 조회
     */
    private fun getSampleCount(): Int {
        return try {
            val db = database.readableDatabase
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM ${KkomiDatabase.TABLE_VOICE_PROFILES}",
                null
            )
            cursor.use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "샘플 수 조회 실패: ${e.message}")
            0
        }
    }

    /**
     * 사용자 프로필 조회
     */
    fun getUserProfile(): VoiceProfile? {
        return try {
            val db = database.readableDatabase
            val cursor = db.query(
                KkomiDatabase.TABLE_VOICE_PROFILES,
                arrayOf("pitch", "speech_rate", "accent", "confidence"),
                null,
                null,
                null,
                null,
                "recorded_at DESC",
                "1"
            )

            cursor.use {
                if (it.moveToFirst()) {
                    VoiceProfile(
                        pitch = it.getFloat(0),
                        speechRate = it.getFloat(1),
                        accent = it.getString(2),
                        confidence = it.getFloat(3),
                        sampleCount = getSampleCount()
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "사용자 프로필 조회 실패: ${e.message}")
            null
        }
    }

    /**
     * 사용자 선호도 학습
     */
    fun learnUserPreferences(command: String, result: Boolean) {
        try {
            val db = database.writableDatabase
            val cursor = db.query(
                KkomiDatabase.TABLE_COMMAND_HISTORY,
                arrayOf("COUNT(*) as count"),
                "command_type = ?",
                arrayOf(command),
                null,
                null,
                null
            )

            val frequency = cursor.use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }

            database.saveUserPreference(
                "command_$command",
                "${frequency + 1}",
                "command_frequency"
            )

            Log.d(TAG, "사용자 선호도 학습: $command (빈도: ${frequency + 1})")
        } catch (e: Exception) {
            Log.e(TAG, "선호도 학습 실패: ${e.message}")
        }
    }

    /**
     * 개인화된 응답 생성
     */
    fun generatePersonalizedResponse(baseResponse: String): String {
        return try {
            val userProfile = getUserProfile()
            if (userProfile != null && userProfile.confidence > 0.7f) {
                // 사용자 프로필을 기반으로 응답 커스터마이징
                val accent = when (userProfile.accent) {
                    "높고빠름" -> "활발한"
                    "높고느림" -> "차분한"
                    "낮고빠름" -> "신나는"
                    "낮고느림" -> "부드러운"
                    else -> "친근한"
                }
                "$accent 톤으로: $baseResponse"
            } else {
                baseResponse
            }
        } catch (e: Exception) {
            Log.e(TAG, "개인화 응답 생성 실패: ${e.message}")
            baseResponse
        }
    }
}
