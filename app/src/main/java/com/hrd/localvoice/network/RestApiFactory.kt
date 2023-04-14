package com.hrd.localvoice.network

import android.content.Context
import com.hrd.localvoice.BuildConfig
import com.hrd.localvoice.R
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class RestApiFactory {
    companion object {
        fun create(context: Context): ApiService? {
            // Setting up request interceptor.
            val headerInterceptor = HeaderInterceptor(context)
            val okHttpClient = OkHttpClient()
                .newBuilder().addInterceptor(headerInterceptor)
                .build()

            var baseUrl: String = context.getString(R.string.test_base_api_url)
            if (!BuildConfig.DEBUG) {
                baseUrl = context.getString(R.string.live_base_api_url)
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