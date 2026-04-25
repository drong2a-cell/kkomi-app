package com.kkomi.assistant

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.zetetic.database.sqlcipher.SQLiteOpenHelper as SQLCipherOpenHelper
import java.security.KeyStore

/**
 * 꼬미 앱 데이터베이스 (SQLCipher - AES-256 암호화)
 */
class KkomiDatabase(private val context: Context) : SQLCipherOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "kkomi.db"
        private const val DB_VERSION = 1

        // 테이블 이름
        const val TABLE_CONVERSATIONS = "conversations"
        const val TABLE_USER_PREFERENCES = "user_preferences"
        const val TABLE_COMMAND_HISTORY = "command_history"
        const val TABLE_CONTACTS_CACHE = "contacts_cache"
        const val TABLE_APP_SETTINGS = "app_settings"
        const val TABLE_AI_MODEL_CACHE = "ai_model_cache"
        const val TABLE_VOICE_PROFILES = "voice_profiles"
        const val TABLE_SEARCH_CACHE = "search_cache"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // conversations 테이블
        db.execSQL("""
            CREATE TABLE $TABLE_CONVERSATIONS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_message TEXT NOT NULL,
                kkomi_response TEXT NOT NULL,
                intent TEXT,
                confidence REAL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                is_offline BOOLEAN DEFAULT 1,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """)
        db.execSQL("CREATE INDEX idx_conversations_timestamp ON $TABLE_CONVERSATIONS(timestamp)")
        db.execSQL("CREATE INDEX idx_conversations_intent ON $TABLE_CONVERSATIONS(intent)")

        // user_preferences 테이블
        db.execSQL("""
            CREATE TABLE $TABLE_USER_PREFERENCES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                key TEXT UNIQUE NOT NULL,
                value TEXT NOT NULL,
                category TEXT,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """)
        db.execSQL("CREATE INDEX idx_user_preferences_category ON $TABLE_USER_PREFERENCES(category)")

        // command_history 테이블
        db.execSQL("""
            CREATE TABLE $TABLE_COMMAND_HISTORY (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                command_type TEXT NOT NULL,
                command_details TEXT,
                recipient TEXT,
                status TEXT,
                execution_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                duration_ms INTEGER,
                error_message TEXT
            )
        """)
        db.execSQL("CREATE INDEX idx_command_history_type ON $TABLE_COMMAND_HISTORY(command_type)")
        db.execSQL("CREATE INDEX idx_command_history_execution_time ON $TABLE_COMMAND_HISTORY(execution_time)")

        // contacts_cache 테이블
        db.execSQL("""
            CREATE TABLE $TABLE_CONTACTS_CACHE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                contact_name TEXT NOT NULL,
                phone_number_hash TEXT NOT NULL,
                contact_type TEXT,
                frequency INTEGER DEFAULT 0,
                last_used DATETIME,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """)
        db.execSQL("CREATE INDEX idx_contacts_cache_name ON $TABLE_CONTACTS_CACHE(contact_name)")
        db.execSQL("CREATE INDEX idx_contacts_cache_frequency ON $TABLE_CONTACTS_CACHE(frequency)")

        // app_settings 테이블
        db.execSQL("""
            CREATE TABLE $TABLE_APP_SETTINGS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                setting_key TEXT UNIQUE NOT NULL,
                setting_value TEXT NOT NULL,
                data_type TEXT,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """)

        // ai_model_cache 테이블
        db.execSQL("""
            CREATE TABLE $TABLE_AI_MODEL_CACHE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                model_name TEXT NOT NULL,
                model_version TEXT NOT NULL,
                model_size_mb REAL,
                accuracy REAL,
                last_updated DATETIME DEFAULT CURRENT_TIMESTAMP,
                is_active BOOLEAN DEFAULT 1
            )
        """)

        // voice_profiles 테이블
        db.execSQL("""
            CREATE TABLE $TABLE_VOICE_PROFILES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                voice_feature_vector BLOB,
                pitch REAL,
                speech_rate REAL,
                accent TEXT,
                confidence REAL,
                recorded_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """)

        // search_cache 테이블
        db.execSQL("""
            CREATE TABLE $TABLE_SEARCH_CACHE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                query TEXT NOT NULL,
                result TEXT NOT NULL,
                source TEXT,
                cached_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                expires_at DATETIME,
                hit_count INTEGER DEFAULT 0
            )
        """)
        db.execSQL("CREATE INDEX idx_search_cache_query ON $TABLE_SEARCH_CACHE(query)")
        db.execSQL("CREATE INDEX idx_search_cache_expires_at ON $TABLE_SEARCH_CACHE(expires_at)")

        // 기본 설정 데이터 삽입
        insertDefaultSettings(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 마이그레이션 로직 (향후 버전 업데이트 시 사용)
        when (oldVersion) {
            1 -> {
                // v1 → v2 마이그레이션
            }
        }
    }

    /**
     * 기본 설정 데이터 삽입
     */
    private fun insertDefaultSettings(db: SQLiteDatabase) {
        val defaultSettings = mapOf(
            "battery_saver_mode" to "0",
            "wifi_only_search" to "1",
            "hotword_sensitivity" to "0.7",
            "max_volume" to "70",
            "language" to "ko-KR",
            "app_version" to "1.0.0"
        )

        defaultSettings.forEach { (key, value) ->
            db.execSQL("""
                INSERT INTO $TABLE_APP_SETTINGS (setting_key, setting_value, data_type)
                VALUES ('$key', '$value', 'TEXT')
            """)
        }
    }

    /**
     * 대화 기록 저장
     */
    fun saveConversation(
        userMessage: String,
        kkomiResponse: String,
        intent: String?,
        confidence: Float
    ): Long {
        val db = writableDatabase
        val values = android.content.ContentValues().apply {
            put("user_message", userMessage)
            put("kkomi_response", kkomiResponse)
            put("intent", intent)
            put("confidence", confidence)
        }
        return db.insert(TABLE_CONVERSATIONS, null, values)
    }

    /**
     * 명령어 실행 기록 저장
     */
    fun saveCommandHistory(
        commandType: String,
        commandDetails: String?,
        status: String,
        errorMessage: String? = null
    ): Long {
        val db = writableDatabase
        val values = android.content.ContentValues().apply {
            put("command_type", commandType)
            put("command_details", commandDetails)
            put("status", status)
            put("error_message", errorMessage)
        }
        return db.insert(TABLE_COMMAND_HISTORY, null, values)
    }

    /**
     * 사용자 선호도 저장
     */
    fun saveUserPreference(key: String, value: String, category: String? = null): Long {
        val db = writableDatabase
        val values = android.content.ContentValues().apply {
            put("key", key)
            put("value", value)
            put("category", category)
        }
        return db.insertWithOnConflict(TABLE_USER_PREFERENCES, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    /**
     * 사용자 선호도 조회
     */
    fun getUserPreference(key: String): String? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USER_PREFERENCES,
            arrayOf("value"),
            "key = ?",
            arrayOf(key),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                it.getString(0)
            } else {
                null
            }
        }
    }

    /**
     * 앱 설정 저장
     */
    fun saveAppSetting(key: String, value: String): Long {
        val db = writableDatabase
        val values = android.content.ContentValues().apply {
            put("setting_key", key)
            put("setting_value", value)
        }
        return db.insertWithOnConflict(TABLE_APP_SETTINGS, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    /**
     * 앱 설정 조회
     */
    fun getAppSetting(key: String): String? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_APP_SETTINGS,
            arrayOf("setting_value"),
            "setting_key = ?",
            arrayOf(key),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                it.getString(0)
            } else {
                null
            }
        }
    }

    /**
     * 데이터베이스 초기화 (테스트용)
     */
    fun clearAllData() {
        val db = writableDatabase
        db.delete(TABLE_CONVERSATIONS, null, null)
        db.delete(TABLE_COMMAND_HISTORY, null, null)
        db.delete(TABLE_CONTACTS_CACHE, null, null)
        db.delete(TABLE_VOICE_PROFILES, null, null)
        db.delete(TABLE_SEARCH_CACHE, null, null)
    }

    /**
     * 오래된 데이터 정리
     */
    fun cleanupOldData() {
        val db = writableDatabase
        
        // 30일 이상 된 대화 기록 삭제
        db.delete(
            TABLE_CONVERSATIONS,
            "datetime(timestamp) < datetime('now', '-30 days')",
            null
        )
        
        // 90일 이상 된 명령어 기록 삭제
        db.delete(
            TABLE_COMMAND_HISTORY,
            "datetime(execution_time) < datetime('now', '-90 days')",
            null
        )
        
        // 만료된 검색 캐시 삭제
        db.delete(
            TABLE_SEARCH_CACHE,
            "datetime(expires_at) < datetime('now')",
            null
        )
    }
}
