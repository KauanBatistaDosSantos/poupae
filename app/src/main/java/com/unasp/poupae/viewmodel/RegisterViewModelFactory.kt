package com.unasp.poupae.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.unasp.poupae.repository.AuthRepository

class RegisterViewModelFactory(
    private val repository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RegisterViewModel(repository) as T
    }
}