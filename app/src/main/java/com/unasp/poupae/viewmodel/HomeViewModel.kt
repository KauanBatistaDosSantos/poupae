package com.unasp.poupae.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.data.PieEntry
import com.unasp.poupae.repository.TransacaoRepository
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: TransacaoRepository) : ViewModel() {

    private val _saldo = MutableLiveData<Double>()
    val saldo: LiveData<Double> = _saldo

    private val _entradasGrafico = MutableLiveData<List<PieEntry>>()
    val entradasGrafico: LiveData<List<PieEntry>> = _entradasGrafico

    fun iniciarResumo() {
        viewModelScope.launch {
            repository.aplicarTransacoesRecorrentes()
            carregarDados()
        }
    }

    private suspend fun carregarDados() {
        val transacoes = repository.carregarTransacoes()

        // 1. Cálculo do saldo estimado: com todas as transações
        var totalGanhos = 0.0
        var totalGastos = 0.0
        transacoes.forEach { transacao ->
            if (transacao.tipo == "despesa") totalGastos += transacao.valor
            else if (transacao.tipo == "ganho") totalGanhos += transacao.valor
        }
        _saldo.postValue(totalGanhos - totalGastos)

        // 2. Cálculo do gráfico: somente despesas que não são de metas
        val mapa = mutableMapOf<String, Double>()
        transacoes.filter { it.tipo == "despesa" && !it.tipoMeta && it.categoria != null }
            .forEach { transacao ->
                val categoria = transacao.categoria!!
                mapa[categoria] = mapa.getOrDefault(categoria, 0.0) + transacao.valor
            }

        val total = mapa.values.sum()
        val entradas = mapa.map { (categoria, valor) ->
            PieEntry((valor / total * 100).toFloat(), categoria)
        }
        _entradasGrafico.postValue(entradas)
    }
}
