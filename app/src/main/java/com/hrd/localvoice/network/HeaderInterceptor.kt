package com.hrd.localvoice.network

import android.content.Context
import com.hrd.localvoice.utils.Functions
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException


class HeaderInterceptor(private var context: Context?) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        if (context != null) {
            val token: String? = Functions.getUserToken(context!!)
            if (token?.isEmpty() == false) {
                val request: Request = chain.request()
                    .newBuilder()
                    .addHeader("Authorization", "Token $token")
                    .build()
                return chain.proceed(request)
            }
        }
        return chain.proceed(chain.request())
    }
}