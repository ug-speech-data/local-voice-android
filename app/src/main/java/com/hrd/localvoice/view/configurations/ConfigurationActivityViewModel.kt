package com.hrd.localvoice.view.configurations

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.hrd.localvoice.DataRepository
import com.hrd.localvoice.models.Configuration

class ConfigurationActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DataRepository = DataRepository(application)

    fun getConfiguration(): LiveData<Configuration?>? {
        return repository.configuration
    }
}