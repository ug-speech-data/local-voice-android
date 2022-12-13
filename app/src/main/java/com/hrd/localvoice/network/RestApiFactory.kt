package com.hrd.localvoice.network

import android.content.Context
import com.hrd.localvoice.BuildConfig
import com.hrd.localvoice.utils.Constants.LIVE_BASE_API_URL
import com.hrd.localvoice.utils.Constants.TEST_BASE_API_URL
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class RestApiFactory {
    companion object {
        fun create(context: Context?): ApiService? {
            // Setting up request interceptor.
            val headerInterceptor = HeaderInterceptor(context)
            val okHttpClient = OkHttpClient()
                .newBuilder().addInterceptor(headerInterceptor)
                .build()
            var baseUrl: String = TEST_BASE_API_URL
            if (!BuildConfig.DEBUG) {
                baseUrl = LIVE_BASE_API_URL
            }
            val retrofit = Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(ApiService::class.java)
        }
    }
}