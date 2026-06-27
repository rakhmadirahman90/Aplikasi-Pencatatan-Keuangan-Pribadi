package com.example.data.remote

import com.example.data.model.TransactionEntity
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

data class SyncTransactionDto(
    val id: Long,
    val title: String,
    val amount: Double,
    val isIncome: Boolean,
    val category: String,
    val date: Long,
    val note: String
)

data class SyncRequest(
    val action: String, // "sync" or "get"
    val transactions: List<SyncTransactionDto> = emptyList()
)

data class SyncResponse(
    val success: Boolean,
    val message: String = "",
    val transactions: List<SyncTransactionDto> = emptyList()
)

interface SyncApi {
    @POST
    suspend fun syncWithAppsScript(
        @Url url: String,
        @Body request: SyncRequest
    ): Response<SyncResponse>
}

object RetrofitClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: SyncApi by lazy {
        Retrofit.Builder()
            // We use a dummy base URL since we pass full URL dynamically via @Url
            .baseUrl("https://script.google.com/") 
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(SyncApi::class.java)
    }
}
