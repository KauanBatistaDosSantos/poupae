package com.unasp.poupae.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.unasp.poupae.repository.MetaRepository

class MetasViewModelFactory(private val repository: MetaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MetasViewModel::class.java)) {
            return MetasViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}