package com.unasp.poupae.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unasp.poupae.model.Transacao
import com.unasp.poupae.repository.TransacaoRepository
import kotlinx.coroutines.launch
import java.util.*

class ExtratoViewModel(private val repository: TransacaoRepository) : ViewModel() {

    private val _transacoes = MutableLiveData<List<Pair<String, Transacao>>>()
    val transacoes: LiveData<List<Pair<String, Transacao>>> = _transacoes

    private val _ganhosPorMes = MutableLiveData<Map<Int, Float>>()
    val ganhosPorMes: LiveData<Map<Int, Float>> = _ganhosPorMes

    private val _despesasPorMes = MutableLiveData<Map<Int, Float>>()
    val despesasPorMes: LiveData<Map<Int, Float>> = _despesasPorMes

    private val _erro = MutableLiveData<String>()
    val erro: LiveData<String> = _erro

    fun carregarTransacoes() {
        viewModelScope.launch {
            val lista = repository.buscarTransacoesComId() // você pode usar o mesmo método de fallback de data

            _transacoes.value = lista.sortedByDescending { it.second.data?.toDate() }

            val ganhos = mutableMapOf<Int, Float>()
            val despesas = mutableMapOf<Int, Float>()

            lista.forEach { (_, transacao) ->
                val data = transacao.data?.toDate() ?: return@forEach
                val mes = Calendar.getInstance().apply { time = data }.get(Calendar.MONTH)
                val valor = transacao.valor.toFloat()

                when (transacao.tipo) {
                    "ganho" -> ganhos[mes] = ganhos.getOrDefault(mes, 0f) + valor
                    "despesa" -> despesas[mes] = despesas.getOrDefault(mes, 0f) + valor
                }
            }

            _ganhosPorMes.value = ganhos
            _despesasPorMes.value = despesas
        }
    }
}
