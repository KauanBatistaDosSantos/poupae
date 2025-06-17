package com.unasp.poupae.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unasp.poupae.repository.MetaRepository
import kotlinx.coroutines.launch

class MetaDetalhesViewModel(private val repository: MetaRepository) : ViewModel() {

    private val _sucesso = MutableLiveData<Boolean>()
    val sucesso: LiveData<Boolean> = _sucesso

    private val _erro = MutableLiveData<String>()
    val erro: LiveData<String> = _erro

    fun atualizarValor(metaId: String, valorAtual: Double, valorAlterado: Double, adicionar: Boolean, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val novoValor = if (adicionar) valorAtual + valorAlterado else valorAtual - valorAlterado
                repository.atualizarValorMeta(metaId, novoValor)
                callback(true)
            } catch (e: Exception) {
                _erro.value = "Erro ao atualizar valor da meta"
                callback(false)
            }
        }
    }

    fun atualizarMeta(metaId: String, nome: String, valorAlvo: Double, dataLimite: String?, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.atualizarMeta(metaId, nome, valorAlvo, dataLimite)
                callback(true)
            } catch (e: Exception) {
                _erro.value = "Erro ao editar meta"
                callback(false)
            }
        }
    }

    fun excluirMetaComTransacoes(metaId: String, nomeMeta: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.excluirMetaETransacoes(metaId, nomeMeta)
                callback(true)
            } catch (e: Exception) {
                _erro.value = "Erro ao excluir meta"
                callback(false)
            }
        }
    }

    fun registrarTransacaoMeta(nomeMeta: String, valor: Double, adicionar: Boolean) {
        viewModelScope.launch {
            repository.registrarTransacaoMeta(nomeMeta, valor, adicionar)
        }
    }
}
