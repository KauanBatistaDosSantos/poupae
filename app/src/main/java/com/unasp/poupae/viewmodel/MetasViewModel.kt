package com.unasp.poupae.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unasp.poupae.model.Meta
import com.unasp.poupae.repository.MetaRepository
import kotlinx.coroutines.launch

class MetasViewModel(private val repository: MetaRepository) : ViewModel() {

    private val _metas = MutableLiveData<List<Meta>>()
    val metas: LiveData<List<Meta>> = _metas

    private val _erro = MutableLiveData<String>()
    val erro: LiveData<String> = _erro

    fun carregarMetas() {
        viewModelScope.launch {
            try {
                val metasCarregadas = repository.carregarMetas()
                _metas.value = metasCarregadas
            } catch (e: Exception) {
                _erro.value = "Erro ao carregar metas."
            }
        }
    }

    fun adicionarMeta(meta: Meta, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val sucesso = repository.adicionarMeta(meta)
                onResult(sucesso)
            } catch (e: Exception) {
                _erro.value = "Erro ao adicionar meta."
                onResult(false)
            }
        }
    }
}