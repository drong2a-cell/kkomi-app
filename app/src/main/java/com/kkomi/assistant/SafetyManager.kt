package com.kkomi.assistant

import android.app.AlertDialog
import android.content.Context
import android.media.AudioManager
import android.telephony.TelephonyManager

/**
 * 안전장치 관리 클래스
 * 사용자 보호 및 오작동 방지 기능을 제공합니다
 */
class SafetyManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    /**
     * 전화/문자 발신 확인 다이얼로그
     */
    fun showConfirmationDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("네") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("아니오") { _, _ ->
                onCancel()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * 볼륨 제한 확인 (최대 70%)
     */
    fun checkAndLimitVolume(requestedVolume: Int): Int {
        return if (requestedVolume > 70) 70 else requestedVolume
    }

    /**
     * 현재 통화 중 여부 확인
     */
    fun isInCall(): Boolean {
        return telephonyManager.callState == TelephonyManager.CALL_STATE_OFFHOOK
    }

    /**
     * 통화 중 음성 명령 가능 여부 확인
     */
    fun canProcessVoiceCommand(): Boolean {
        return !isInCall()
    }

    /**
     * 알람/타이머 설정 시간 확인
     */
    fun validateAlarmTime(hour: Int, minute: Int): Pair<Boolean, String> {
        return when {
            hour < 0 || hour > 23 -> Pair(false, "시간은 0~23 사이여야 합니다")
            minute < 0 || minute > 59 -> Pair(false, "분은 0~59 사이여야 합니다")
            else -> Pair(true, "")
        }
    }

    /**
     * 타이머 시간 확인
     */
    fun validateTimerMinutes(minutes: Int): Pair<Boolean, String> {
        return when {
            minutes < 1 -> Pair(false, "타이머는 최소 1분 이상이어야 합니다")
            minutes > 1440 -> Pair(false, "타이머는 최대 24시간(1440분)입니다")
            else -> Pair(true, "")
        }
    }

    /**
     * 전화번호 유효성 확인
     */
    fun validatePhoneNumber(phoneNumber: String): Pair<Boolean, String> {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        return when {
            cleanNumber.isEmpty() -> Pair(false, "전화번호가 없습니다")
            cleanNumber.length < 10 -> Pair(false, "유효한 전화번호가 아닙니다")
            else -> Pair(true, "")
        }
    }

    /**
     * 문자 메시지 길이 확인
     */
    fun validateSmsMessage(message: String): Pair<Boolean, String> {
        return when {
            message.isEmpty() -> Pair(false, "메시지가 비어있습니다")
            message.length > 1000 -> Pair(false, "메시지가 너무 깁니다 (최대 1000자)")
            else -> Pair(true, "")
        }
    }

    /**
     * 볼륨 범위 확인
     */
    fun validateVolume(volume: Int): Pair<Boolean, String> {
        return when {
            volume < 0 -> Pair(false, "볼륨은 0% 이상이어야 합니다")
            volume > 100 -> Pair(false, "볼륨은 100% 이하여야 합니다")
            else -> Pair(true, "")
        }
    }

    /**
     * 밝기 범위 확인
     */
    fun validateBrightness(brightness: Int): Pair<Boolean, String> {
        return when {
            brightness < 0 -> Pair(false, "밝기는 0% 이상이어야 합니다")
            brightness > 100 -> Pair(false, "밝기는 100% 이하여야 합니다")
            else -> Pair(true, "")
        }
    }

    /**
     * 명령 실행 전 안전 확인
     */
    fun performSafetyCheck(commandType: String): Pair<Boolean, String> {
        // 통화 중 음성 명령 확인
        if (!canProcessVoiceCommand()) {
            return Pair(false, "통화 중에는 음성 명령을 사용할 수 없습니다")
        }

        return Pair(true, "")
    }
}
