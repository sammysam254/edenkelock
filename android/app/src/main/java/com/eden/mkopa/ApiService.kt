package com.eden.mkopa

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class DeviceStatus(
    val is_locked: Boolean,
    val loan_balance: Double,
    val status: String
)

interface SupabaseApi {
    @GET("rest/v1/devices")
    suspend fun getDeviceStatus(
        @Query("device_code") deviceCode: String,
        @Query("select") select: String = "is_locked,loan_balance,status",
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String
    ): Response<List<DeviceStatus>>
    
    @PATCH("rest/v1/devices")
    suspend fun updateLastSync(
        @Query("device_code") deviceCode: String,
        @Body body: Map<String, String>,
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String
    ): Response<Unit>
}

object ApiClient {
    private const val BASE_URL = "https://fvkjeteywfcppbtovbiv.supabase.co/"
    private const val API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZ2a2pldGV5d2ZjcHBidG92Yml2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQxOTU2NzEsImV4cCI6MjA4OTc3MTY3MX0.5pOcpCSWn98Vvmq4IBQkWWv-nvvA6zbeUZXjSQ3cfC0"
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val api: SupabaseApi = retrofit.create(SupabaseApi::class.java)
    
    fun getAuthHeader(): String = "Bearer $API_KEY"
}
