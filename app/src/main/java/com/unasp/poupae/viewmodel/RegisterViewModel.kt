package com.unasp.poupae.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unasp.poupae.repository.AuthRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class RegisterViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _estadoRegistro = MutableLiveData<Result<Unit>>()
    val estadoRegistro: LiveData<Result<Unit>> = _estadoRegistro

    fun registrarUsuario(email: String, senha: String) {
        viewModelScope.launch {
            val resultado = repository.criarConta(email, senha)
            _estadoRegistro.postValue(resultado)
        }
    }
}
