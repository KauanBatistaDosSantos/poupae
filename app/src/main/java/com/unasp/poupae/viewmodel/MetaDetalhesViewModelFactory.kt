package com.unasp.poupae.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.unasp.poupae.repository.MetaRepository

class MetaDetalhesViewModelFactory(private val repository: MetaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MetaDetalhesViewModel::class.java)) {
            return MetaDetalhesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
