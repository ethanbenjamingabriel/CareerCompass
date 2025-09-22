package dev.hungrymonkey.careercompass.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CareerGoalsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CareerGoalsViewModel::class.java)) {
            return CareerGoalsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
