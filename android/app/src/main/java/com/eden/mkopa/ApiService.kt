package com.eden.mkopa

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

data class DeviceStatus(
    val device_id: String,
    val status: String,
    val is_locked: Boolean,
    val balance: Double,
    val total_amount: Double,
    val amount_paid: Double
)

data class AppVersion(
    val version_code: Int,
    val version_name: String,
    val download_url: String,
    val force_update: Boolean
)

interface ApiService {
    @GET("api/devices/{device_id}/status")
    fun getDeviceStatus(@Path("device_id") deviceId: String): Call<DeviceStatus>
    
    @GET("api/app/version")
    fun checkAppVersion(): Call<AppVersion>
    
    companion object {
        private const val BASE_URL = "https://eden-mkopa.onrender.com/"
        
        fun create(): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            return retrofit.create(ApiService::class.java)
        }
    }
}
