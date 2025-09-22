package dev.hungrymonkey.careercompass.network

import dev.hungrymonkey.careercompass.config.AppConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (AppConfig.Logging.ENABLE_NETWORK_LOGGING) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(AppConfig.Backend.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(AppConfig.Backend.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(AppConfig.Backend.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    private val resumeRetrofit = Retrofit.Builder()
        .baseUrl(AppConfig.Backend.RESUME_SERVICE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val coverLetterRetrofit = Retrofit.Builder()
        .baseUrl(AppConfig.Backend.COVER_LETTER_SERVICE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: ApiService = resumeRetrofit.create(ApiService::class.java)
    val resumeApiService: ApiService = resumeRetrofit.create(ApiService::class.java)
    val coverLetterApiService: ApiService = coverLetterRetrofit.create(ApiService::class.java)
}
