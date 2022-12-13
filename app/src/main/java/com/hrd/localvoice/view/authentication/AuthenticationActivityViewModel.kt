package com.hrd.localvoice.view.authentication

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.hrd.localvoice.models.User
import com.hrd.localvoice.network.RestApiFactory
import com.hrd.localvoice.network.response_models.AuthenticationResponse
import com.hrd.localvoice.utils.Constants.IS_NEW_USER
import com.hrd.localvoice.utils.Constants.SHARED_PREFS_FILE
import com.hrd.localvoice.utils.Constants.USER_OBJECT
import com.hrd.localvoice.utils.Constants.USER_TOKEN
import com.hrd.localvoice.utils.Functions.Companion.removeUserToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class AuthenticationActivityViewModel(application: Application) : AndroidViewModel(application) {
    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    private val apiService = RestApiFactory.create(application);
    val isLoggedIn = MutableLiveData<Boolean>()
    private val context: Application = application
    val errorMessage = MutableLiveData<String>()
    private val user: MutableLiveData<User> = MutableLiveData<User>()

    fun login(emailAddress: String, password: String) {
        isLoading.value = true
        apiService?.login(emailAddress, password)
            ?.enqueue(object : Callback<AuthenticationResponse?> {
                override fun onResponse(
                    call: Call<AuthenticationResponse?>,
                    response: Response<AuthenticationResponse?>
                ) {
                    val prefsEditor: SharedPreferences.Editor =
                        context.getSharedPreferences(SHARED_PREFS_FILE, MODE_PRIVATE).edit()

                    if (response.body() != null) {
                        if (response.body()!!.errorMessage != null) {
                            errorMessage.value = response.body()!!.errorMessage
                        }
                        if (response.body()!!.token != null) {
                            prefsEditor.putBoolean(IS_NEW_USER, false)
                            prefsEditor.putString(USER_TOKEN, response.body()!!.token)
                            prefsEditor.apply()
                        }
                        if (response.body()!!.user != null) {
                            user.value = response.body()!!.user
                            val gson = Gson()
                            val userString: String = gson.toJson(response.body()!!.user)
                            prefsEditor.putBoolean(IS_NEW_USER, false)
                            prefsEditor.putString(USER_OBJECT, userString)
                            prefsEditor.apply()
                            isLoggedIn.value = true
                        }
                    } else {
                        errorMessage.value =
                            response.code().toString() + ": " + response.message()
                        if (response.code() == 401) {
                            removeUserToken(context)
                        }
                    }
                    isLoading.value = false
                }

                override fun onFailure(call: Call<AuthenticationResponse?>, t: Throwable) {
                    errorMessage.value = t.message
                    isLoading.value = false
                }
            })
    }

    fun register(
        emailAddress: String,
        surname: String,
        otherNames: String,
        phone: String,
        password: String
    ) {
        isLoading.value = true
        apiService?.register(emailAddress, password, surname, otherNames, phone)
            ?.enqueue(object : Callback<AuthenticationResponse?> {
                override fun onResponse(
                    call: Call<AuthenticationResponse?>,
                    response: Response<AuthenticationResponse?>
                ) {
                    val prefsEditor: SharedPreferences.Editor =
                        context.getSharedPreferences(SHARED_PREFS_FILE, MODE_PRIVATE).edit()

                    if (response.body() != null) {
                        if (response.body()!!.errorMessage != null) {
                            errorMessage.value = response.body()!!.errorMessage
                        }
                        if (response.body()!!.token != null) {
                            prefsEditor.putBoolean(IS_NEW_USER, false)
                            prefsEditor.putString(USER_TOKEN, response.body()!!.token)
                            prefsEditor.apply()
                        }
                        if (response.body()!!.user != null) {
                            user.value = response.body()!!.user
                            val gson = Gson()
                            val userString: String = gson.toJson(response.body()!!.user)
                            prefsEditor.putBoolean(IS_NEW_USER, false)
                            prefsEditor.putString(USER_OBJECT, userString)
                            prefsEditor.apply()
                            isLoggedIn.value = true
                        }
                    } else {
                        errorMessage.value =
                            response.code().toString() + ": " + response.message()
                        if (response.code() == 401) {
                            removeUserToken(context)
                        }
                    }
                    isLoading.value = false
                }

                override fun onFailure(call: Call<AuthenticationResponse?>, t: Throwable) {
                    errorMessage.value = t.message
                    isLoading.value = false
                }
            })
    }
}