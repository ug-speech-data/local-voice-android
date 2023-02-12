package com.hrd.localvoice.view.authentication

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.DataRepository
import com.hrd.localvoice.models.Configuration
import com.hrd.localvoice.models.User
import com.hrd.localvoice.network.RestApiFactory
import com.hrd.localvoice.network.response_models.AuthenticationResponse
import com.hrd.localvoice.utils.Constants.IS_NEW_USER
import com.hrd.localvoice.utils.Constants.SHARED_PREFS_FILE
import com.hrd.localvoice.utils.Constants.USER_TOKEN
import com.hrd.localvoice.utils.Functions.Companion.removeUserToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class AuthenticationActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DataRepository = DataRepository(application)
    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val profileUpdate: MutableLiveData<Boolean> = MutableLiveData(false)
    private val apiService = RestApiFactory.create(application);
    val isLoggedIn = MutableLiveData<Boolean>()
    private val context: Application = application
    val errorMessage = MutableLiveData<String>()

    val user: LiveData<User?>?
        get() = repository.user

    val configuration: LiveData<Configuration?>?
        get() = repository.configuration

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
                            isLoggedIn.value = true

                            // Store user in the database
                            if (response.body()?.user != null) {
                                AppRoomDatabase.databaseWriteExecutor.execute {
                                    AppRoomDatabase.getDatabase(context)
                                        ?.UserDao()?.deleteAll()
                                    AppRoomDatabase.getDatabase(context)
                                        ?.UserDao()?.insertUser(response.body()!!.user!!)
                                }
                            }
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
        password: String
    ) {
        isLoading.value = true
        apiService?.register(emailAddress, password)
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
                            isLoggedIn.value = true

                            // Store user in the database
                            if (response.body()?.user != null) {
                                AppRoomDatabase.databaseWriteExecutor.execute {
                                    AppRoomDatabase.getDatabase(context)
                                        ?.UserDao()?.deleteAll()
                                    AppRoomDatabase.getDatabase(context)
                                        ?.UserDao()?.insertUser(response.body()!!.user!!)
                                }
                            }
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

    fun updateProfile(
        gender: String,
        surname: String,
        otherNames: String,
        network: String,
        phone: String,
        checkedPrivacyPolicy: Boolean,
        environment: String,
        locale: String,
        age: Int?
    ) {
        isLoading.value = true
        apiService?.updateProfile(
            gender,
            surname,
            otherNames,
            network,
            phone,
            checkedPrivacyPolicy,
            environment,
            locale,
            age
        )
            ?.enqueue(object : Callback<AuthenticationResponse?> {
                override fun onResponse(
                    call: Call<AuthenticationResponse?>,
                    response: Response<AuthenticationResponse?>
                ) {
                    if (response.body()!!.user != null) {
                        // Store user in the database
                        if (response.body()?.user != null) {
                            AppRoomDatabase.databaseWriteExecutor.execute {
                                AppRoomDatabase.getDatabase(context)
                                    ?.UserDao()?.deleteAll()
                                AppRoomDatabase.getDatabase(context)
                                    ?.UserDao()?.insertUser(response.body()!!.user!!)
                            }
                            profileUpdate.value = true
                        }
                    }
                    isLoading.value = false
                }

                override fun onFailure(call: Call<AuthenticationResponse?>, t: Throwable) {
                    errorMessage.value = t.message
                    isLoading.value = false
                    profileUpdate.value = false
                }
            })
    }
}