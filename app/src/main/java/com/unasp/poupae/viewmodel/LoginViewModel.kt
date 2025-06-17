package com.unasp.poupae.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unasp.poupae.repository.AuthRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _estadoLogin = MutableLiveData<Result<Unit>>()
    val estadoLogin: LiveData<Result<Unit>> = _estadoLogin

    fun fazerLogin(email: String, senha: String) {
        viewModelScope.launch {
            val resultado = repository.loginEmailSenha(email, senha)
            _estadoLogin.postValue(resultado)
        }
    }
}
