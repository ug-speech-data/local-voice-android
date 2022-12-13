package com.hrd.localvoice.network

import com.hrd.localvoice.network.response_models.AuthenticationResponse
import com.hrd.localvoice.network.response_models.ConfigurationResponse
import com.hrd.localvoice.network.response_models.ImagesResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*


interface ApiService {
    @FormUrlEncoded
    @POST("auth/login/")
    fun login(
        @Field("email_address") emailAddress: String?, @Field("password") password: String?
    ): Call<AuthenticationResponse?>?

    @FormUrlEncoded
    @POST("auth/register/")
    fun register(
        @Field("email_address") emailAddress: String?,
        @Field("password") password: String?,
        @Field("surname") surname: String?,
        @Field("other_names") otherNames: String?,
        @Field("phone") phone: String?,
    ): Call<AuthenticationResponse?>?

    @POST("auth/logout/")
    fun logOut(): Call<Void?>?

    @Multipart
    @POST("upload-audio/")
    fun uploadAudioFile(
        @Part file: MultipartBody.Part?,
        @Part remoteImageId: MultipartBody.Part?,
    ): Call<ResponseBody?>?

    @GET("get-mobile-app-configurations/")
    fun getConfigurations(): Call<ConfigurationResponse?>?

    @GET("get-assigned-images/")
    fun getAssignedImages(): Call<ImagesResponse?>?
}