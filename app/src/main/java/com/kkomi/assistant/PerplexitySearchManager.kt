package com.kkomi.assistant

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Perplexity API를 통한 온라인 검색 기능
 * Wi-Fi 연결 시 실시간 정보 검색을 수행합니다
 */
class PerplexitySearchManager(private val context: Context, private val database: KkomiDatabase) {

    companion object {
        private const val TAG = "PerplexitySearchManager"
        private const val API_ENDPOINT = "https://api.perplexity.ai/chat/completions"
        private const val CACHE_DURATION_HOURS = 24
        private const val CACHE_EXPIRY_DAYS = 7
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Wi-Fi 연결 상태 확인
     */
    fun isWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * 온라인 검색 수행
     */
    suspend fun search(query: String, apiKey: String): SearchResult {
        return withContext(Dispatchers.IO) {
            try {
                // 캐시 확인
                val cachedResult = getCachedResult(query)
                if (cachedResult != null) {
                    Log.d(TAG, "캐시된 검색 결과 사용: $query")
                    return@withContext cachedResult
                }

                // Wi-Fi 확인
                if (!isWifiConnected()) {
                    Log.w(TAG, "Wi-Fi 연결 없음")
                    return@withContext SearchResult(
                        query = query,
                        result = "Wi-Fi 연결이 필요합니다",
                        source = "offline",
                        success = false
                    )
                }

                // API 호출
                val result = callPerplexityAPI(query, apiKey)
                
                // 결과 캐싱
                if (result.success) {
                    cacheResult(query, result)
                }

                result
            } catch (e: Exception) {
                Log.e(TAG, "검색 실패: ${e.message}")
                SearchResult(
                    query = query,
                    result = "검색 중 오류가 발생했습니다: ${e.message}",
                    source = "error",
                    success = false
                )
            }
        }
    }

    /**
     * Perplexity API 호출
     */
    private fun callPerplexityAPI(query: String, apiKey: String): SearchResult {
        try {
            // 요청 본문 생성
            val requestBody = JSONObject().apply {
                put("model", "pplx-7b-online")
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", query)
                    })
                })
                put("temperature", 0.7)
                put("max_tokens", 500)
                put("top_p", 0.9)
            }.toString()

            // HTTP 요청 생성
            val request = Request.Builder()
                .url(API_ENDPOINT)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            // 요청 실행
            val response = httpClient.newCall(request).execute()

            return if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val jsonResponse = JSONObject(responseBody)
                
                // 응답 파싱
                val content = jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                SearchResult(
                    query = query,
                    result = content,
                    source = "perplexity",
                    success = true
                )
            } else {
                Log.e(TAG, "API 오류: ${response.code}")
                SearchResult(
                    query = query,
                    result = "API 오류: ${response.code}",
                    source = "error",
                    success = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "API 호출 실패: ${e.message}")
            throw e
        }
    }

    /**
     * 캐시된 검색 결과 조회
     */
    private fun getCachedResult(query: String): SearchResult? {
        return try {
            val db = database.readableDatabase
            val cursor = db.query(
                KkomiDatabase.TABLE_SEARCH_CACHE,
                arrayOf("result", "source"),
                "query = ? AND datetime(expires_at) > datetime('now')",
                arrayOf(query),
                null,
                null,
                "cached_at DESC",
                "1"
            )

            cursor.use {
                if (it.moveToFirst()) {
                    SearchResult(
                        query = query,
                        result = it.getString(0),
                        source = it.getString(1),
                        success = true,
                        isCached = true
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "캐시 조회 실패: ${e.message}")
            null
        }
    }

    /**
     * 검색 결과 캐싱
     */
    private fun cacheResult(query: String, result: SearchResult) {
        try {
            val expiresAt = System.currentTimeMillis() + (CACHE_DURATION_HOURS * 60 * 60 * 1000)
            database.saveSearchResult(query, result.result, result.source, expiresAt)
            Log.d(TAG, "검색 결과 캐싱: $query")
        } catch (e: Exception) {
            Log.e(TAG, "캐싱 실패: ${e.message}")
        }
    }

    /**
     * 오래된 캐시 정리
     */
    fun cleanupOldCache() {
        try {
            database.cleanupOldData()
            Log.d(TAG, "오래된 캐시 정리 완료")
        } catch (e: Exception) {
            Log.e(TAG, "캐시 정리 실패: ${e.message}")
        }
    }

    data class SearchResult(
        val query: String,
        val result: String,
        val source: String,
        val success: Boolean,
        val isCached: Boolean = false
    )
}

// KkomiDatabase 확장 함수
fun KkomiDatabase.saveSearchResult(
    query: String,
    result: String,
    source: String,
    expiresAt: Long
) {
    val db = writableDatabase
    val values = android.content.ContentValues().apply {
        put("query", query)
        put("result", result)
        put("source", source)
        put("expires_at", expiresAt)
        put("hit_count", 1)
    }
    db.insertWithOnConflict(
        KkomiDatabase.TABLE_SEARCH_CACHE,
        null,
        values,
        android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
    )
}
