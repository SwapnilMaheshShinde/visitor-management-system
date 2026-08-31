package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class NetworkManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vms_network_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_DEV_OTP_MODE = "dev_otp_mode"
        private const val KEY_LAST_DEV_OTP = "last_dev_otp"
        private const val DEFAULT_BASE_URL = "http://10.0.2.2:5000/api/" // Android Emulator localhost bridge

        @Volatile
        private var INSTANCE: NetworkManager? = null

        fun getInstance(context: Context): NetworkManager {
            return INSTANCE ?: synchronized(this) {
                val instance = NetworkManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private val _baseUrlState = MutableStateFlow(getBaseUrl())
    val baseUrlState: StateFlow<String> = _baseUrlState.asStateFlow()

    private val _isServerConnected = MutableStateFlow(false)
    val isServerConnected: StateFlow<Boolean> = _isServerConnected.asStateFlow()

    private val _lastPingLatencyMs = MutableStateFlow<Long>(-1)
    val lastPingLatencyMs: StateFlow<Long> = _lastPingLatencyMs.asStateFlow()

    private val _lastDevOtp = MutableStateFlow(getLastDevOtp())
    val lastDevOtp: StateFlow<String> = _lastDevOtp.asStateFlow()

    private var apiService: VmsApiService? = null

    init {
        buildApiService()
    }

    fun getBaseUrl(): String {
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(newUrl: String) {
        val formatted = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        prefs.edit().putString(KEY_BASE_URL, formatted).apply()
        _baseUrlState.value = formatted
        buildApiService()
    }

    fun getAuthToken(): String {
        val token = prefs.getString(KEY_AUTH_TOKEN, "") ?: ""
        return if (token.isNotEmpty() && !token.startsWith("Bearer ")) "Bearer $token" else token
    }

    fun setAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun clearAuthToken() {
        prefs.edit().remove(KEY_AUTH_TOKEN).apply()
    }

    fun isDevOtpMode(): Boolean {
        return prefs.getBoolean(KEY_DEV_OTP_MODE, true)
    }

    fun setDevOtpMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEV_OTP_MODE, enabled).apply()
    }

    fun getLastDevOtp(): String {
        return prefs.getString(KEY_LAST_DEV_OTP, "482910") ?: "482910"
    }

    fun setLastDevOtp(otp: String) {
        prefs.edit().putString(KEY_LAST_DEV_OTP, otp).apply()
        _lastDevOtp.value = otp
    }

    private fun buildApiService() {
        try {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .writeTimeout(8, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(_baseUrlState.value)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            apiService = retrofit.create(VmsApiService::class.java)
        } catch (e: Exception) {
            Log.e("NetworkManager", "Error building Retrofit instance: ${e.message}")
        }
    }

    fun getApiService(): VmsApiService {
        if (apiService == null) {
            buildApiService()
        }
        return apiService!!
    }

    suspend fun testConnection(): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val response = getApiService().checkHealth()
            val latency = System.currentTimeMillis() - start
            val connected = response.isSuccessful
            _isServerConnected.value = connected
            _lastPingLatencyMs.value = if (connected) latency else -1
            Pair(connected, latency)
        } catch (e: Exception) {
            Log.w("NetworkManager", "Connection ping failed: ${e.message}")
            _isServerConnected.value = false
            _lastPingLatencyMs.value = -1
            Pair(false, -1)
        }
    }
}
