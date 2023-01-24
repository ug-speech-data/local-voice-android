package com.hrd.localvoice.network

import com.hrd.localvoice.network.response_models.AuthenticationResponse
import com.hrd.localvoice.network.response_models.ConfigurationResponse
import com.hrd.localvoice.network.response_models.ImagesResponse
import com.hrd.localvoice.network.response_models.UploadResponse
import okhttp3.MultipartBody
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
        @Field("password") password: String?
    ): Call<AuthenticationResponse?>?

    @FormUrlEncoded
    @POST("auth/profile/")
    fun updateProfile(
        @Field("gender") gender: String,
        @Field("surname") surname: String,
        @Field("other_names") otherNames: String,
        @Field("phone_network") network: String,
        @Field("phone") phone: String,
        @Field("accepted_privacy_policy") checkedPrivacyPolicy: Boolean,
        @Field("recording_environment") environment: String,
        @Field("age") age: Int?
    ): Call<AuthenticationResponse?>?

    @POST("auth/logout/")
    fun logOut(): Call<Void?>?

    @Multipart
    @POST("upload-audio/")
    fun uploadAudioFile(
        @Part file: MultipartBody.Part?,
        @Part audioDataBody: MultipartBody.Part?,
        @Part participantDataBody: MultipartBody.Part?,
    ): Call<UploadResponse?>?

    @GET("get-mobile-app-configurations/")
    fun getConfigurations(): Call<ConfigurationResponse?>?

    @GET("auth/profile/")
    fun getProfile(): Call<AuthenticationResponse?>?

    @GET("get-assigned-images/")
    fun getAssignedImages(): Call<ImagesResponse?>?
}