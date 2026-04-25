package com.kkomi.assistant

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * 안드로이드 기기 제어 매니저
 * 전화, 문자, 음악, 알람, 타이머, 볼륨, 밝기 등을 제어합니다
 */
class DeviceControlManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * 전화 걸기
     */
    fun makeCall(phoneNumber: String, onConfirm: (Boolean) -> Unit) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }

        try {
            ContextCompat.startActivity(context, intent, null)
            onConfirm(true)
        } catch (e: Exception) {
            Toast.makeText(context, "전화를 걸 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            onConfirm(false)
        }
    }

    /**
     * 문자 보내기
     */
    fun sendSms(phoneNumber: String, message: String, onConfirm: (Boolean) -> Unit) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            onConfirm(true)
        } catch (e: Exception) {
            Toast.makeText(context, "문자를 보낼 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            onConfirm(false)
        }
    }

    /**
     * 음악 재생
     */
    fun playMusic(query: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://music.youtube.com/search?q=$query")
            }
            ContextCompat.startActivity(context, intent, null)
        } catch (e: Exception) {
            Toast.makeText(context, "음악을 재생할 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 알람 설정
     */
    fun setAlarm(hour: Int, minute: Int) {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, "꼬미 알람")
            }
            ContextCompat.startActivity(context, intent, null)
        } catch (e: Exception) {
            Toast.makeText(context, "알람을 설정할 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 타이머 설정
     */
    fun setTimer(minutes: Int) {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
                putExtra(AlarmClock.EXTRA_MESSAGE, "꼬미 타이머")
            }
            ContextCompat.startActivity(context, intent, null)
        } catch (e: Exception) {
            Toast.makeText(context, "타이머를 설정할 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 볼륨 조절 (최대 70%)
     */
    fun adjustVolume(percentage: Int) {
        var volume = percentage
        
        // 볼륨 제한 (최대 70%)
        if (volume > 70) volume = 70
        if (volume < 0) volume = 0

        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val newVolume = (maxVolume * volume / 100)

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, AudioManager.FLAG_SHOW_UI)
    }

    /**
     * 밝기 조절
     */
    fun adjustBrightness(percentage: Int) {
        try {
            var brightness = percentage
            
            // 밝기 범위 (0-255)
            if (brightness > 100) brightness = 100
            if (brightness < 0) brightness = 0

            val brightnessValue = (brightness * 255 / 100)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    brightnessValue
                )
            }
        } catch (e: Exception) {
            Toast.makeText(context, "밝기를 조절할 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 현재 볼륨 레벨 조회
     */
    fun getCurrentVolume(): Int {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return (currentVolume * 100 / maxVolume)
    }

    /**
     * 최대 볼륨 레벨 조회
     */
    fun getMaxVolume(): Int {
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }
}
