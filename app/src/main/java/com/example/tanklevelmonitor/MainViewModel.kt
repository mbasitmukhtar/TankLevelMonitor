package com.example.tanklevelmonitor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.tanklevelmonitor.repository.DatabaseRepository

class MainViewModel(
    app: Application,
    val databaseRepository: DatabaseRepository,
) : AndroidViewModel(app) {



}