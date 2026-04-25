package com.kkomi.assistant

import android.os.Bundle
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * 꼬미 앱 설정 화면
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var database: KkomiDatabase
    private lateinit var llSettings: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        database = KkomiDatabase(this)
        llSettings = findViewById(R.id.ll_settings)

        setupSettings()
    }

    private fun setupSettings() {
        // 배터리 절약 모드
        addToggleSetting(
            "배터리 절약 모드",
            "배터리 소모를 줄이기 위해 음성 인식 주기를 증가시킵니다",
            "battery_saver_mode"
        )

        // Wi-Fi 전용 온라인 기능
        addToggleSetting(
            "Wi-Fi 전용 검색",
            "모바일 데이터를 절약하기 위해 Wi-Fi 연결 시에만 온라인 검색을 활성화합니다",
            "wifi_only_search"
        )

        // 사용자 이름
        addTextSetting(
            "사용자 이름",
            "꼬미가 부를 사용자의 이름을 설정합니다",
            "user_name"
        )

        // 음성 인식 민감도
        addSliderSetting(
            "음성 인식 민감도",
            "핫워드 감지 민감도를 조절합니다 (0.5 ~ 1.0)",
            "hotword_sensitivity"
        )

        // 최대 볼륨
        addSliderSetting(
            "최대 볼륨",
            "음악 재생 시 최대 볼륨을 설정합니다 (50 ~ 100%)",
            "max_volume"
        )

        // 정보
        addInfoSection()
    }

    /**
     * 토글 설정 추가
     */
    private fun addToggleSetting(title: String, description: String, key: String) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(resources.getColor(R.color.black, null))
        }

        val descView = TextView(this).apply {
            text = description
            textSize = 12f
            setTextColor(resources.getColor(R.color.dark_gray, null))
        }

        val switchView = Switch(this).apply {
            isChecked = database.getAppSetting(key)?.toIntOrNull() == 1
            setOnCheckedChangeListener { _, isChecked ->
                database.saveAppSetting(key, if (isChecked) "1" else "0")
                Toast.makeText(this@SettingsActivity, "설정이 저장되었습니다", Toast.LENGTH_SHORT).show()
            }
        }

        container.addView(titleView)
        container.addView(descView)
        container.addView(switchView)
        llSettings.addView(container)
    }

    /**
     * 텍스트 설정 추가
     */
    private fun addTextSetting(title: String, description: String, key: String) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(resources.getColor(R.color.black, null))
        }

        val descView = TextView(this).apply {
            text = description
            textSize = 12f
            setTextColor(resources.getColor(R.color.dark_gray, null))
        }

        val editView = EditText(this).apply {
            setText(database.getUserPreference(key) ?: "")
            hint = "입력하세요..."
            setTextColor(resources.getColor(R.color.black, null))
        }

        val saveButton = android.widget.Button(this).apply {
            text = "저장"
            setOnClickListener {
                val value = editView.text.toString()
                if (value.isNotEmpty()) {
                    database.saveUserPreference(key, value)
                    Toast.makeText(this@SettingsActivity, "설정이 저장되었습니다", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SettingsActivity, "값을 입력해주세요", Toast.LENGTH_SHORT).show()
                }
            }
        }

        container.addView(titleView)
        container.addView(descView)
        container.addView(editView)
        container.addView(saveButton)
        llSettings.addView(container)
    }

    /**
     * 슬라이더 설정 추가 (간단한 버전)
     */
    private fun addSliderSetting(title: String, description: String, key: String) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(resources.getColor(R.color.black, null))
        }

        val descView = TextView(this).apply {
            text = description
            textSize = 12f
            setTextColor(resources.getColor(R.color.dark_gray, null))
        }

        val valueView = TextView(this).apply {
            text = "현재값: ${database.getAppSetting(key) ?: "기본값"}"
            textSize = 14f
            setTextColor(resources.getColor(R.color.kkomi_primary, null))
        }

        val editView = EditText(this).apply {
            setText(database.getAppSetting(key) ?: "")
            hint = "숫자를 입력하세요..."
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(resources.getColor(R.color.black, null))
        }

        val saveButton = android.widget.Button(this).apply {
            text = "저장"
            setOnClickListener {
                val value = editView.text.toString()
                if (value.isNotEmpty()) {
                    database.saveAppSetting(key, value)
                    valueView.text = "현재값: $value"
                    Toast.makeText(this@SettingsActivity, "설정이 저장되었습니다", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SettingsActivity, "값을 입력해주세요", Toast.LENGTH_SHORT).show()
                }
            }
        }

        container.addView(titleView)
        container.addView(descView)
        container.addView(valueView)
        container.addView(editView)
        container.addView(saveButton)
        llSettings.addView(container)
    }

    /**
     * 정보 섹션 추가
     */
    private fun addInfoSection() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 32, 16, 16)
            }
        }

        val titleView = TextView(this).apply {
            text = "정보"
            textSize = 16f
            setTextStyle(android.graphics.Typeface.BOLD)
            setTextColor(resources.getColor(R.color.black, null))
        }

        val versionView = TextView(this).apply {
            text = "버전: 1.0.0"
            textSize = 14f
            setTextColor(resources.getColor(R.color.dark_gray, null))
        }

        val privacyView = TextView(this).apply {
            text = "모든 데이터는 기기 내에 AES-256으로 암호화되어 저장됩니다"
            textSize = 12f
            setTextColor(resources.getColor(R.color.dark_gray, null))
        }

        container.addView(titleView)
        container.addView(versionView)
        container.addView(privacyView)
        llSettings.addView(container)
    }

    override fun onDestroy() {
        super.onDestroy()
        database.close()
    }
}
