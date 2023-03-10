package com.example.tanklevelmonitor

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tanklevelmonitor.repository.DatabaseRepository

class MainViewModelProviderFactory(
    val app: Application,
    private val databaseRepository: DatabaseRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(
            app,
            databaseRepository,
        ) as T
    }
}