package com.unasp.poupae.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.unasp.poupae.repository.OrcamentoRepository
import java.util.*

class OrcamentoViewModel : ViewModel() {

    private val repository = OrcamentoRepository()

    private val _orcamentosRecorrentes = MutableLiveData<List<Map<String, Any>>>()
    val orcamentosRecorrentes: LiveData<List<Map<String, Any>>> = _orcamentosRecorrentes

    private val _transacoesDoMes = MutableLiveData<List<Map<String, Any>>>()
    val transacoesDoMes: LiveData<List<Map<String, Any>>> = _transacoesDoMes

    private val _orcamentosCategoria = MutableLiveData<List<Map<String, Any>>>()
    val orcamentosCategoria: LiveData<List<Map<String, Any>>> = _orcamentosCategoria

    private val _erro = MutableLiveData<String?>()
    val erro: LiveData<String?> = _erro

    fun carregarOrcamentosRecorrentes() {
        repository.carregarOrcamento { sucesso, dados ->
            if (sucesso) {
                _orcamentosRecorrentes.postValue(dados)
            } else {
                _erro.postValue("Erro ao carregar orçamentos recorrentes.")
            }
        }
    }

    fun carregarTransacoesDesdeInicioDoMes() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val inicioDoMes = Timestamp(calendar.time)

        repository.carregarTransacoesDesde(inicioDoMes) { sucesso, dados ->
            if (sucesso) {
                _transacoesDoMes.postValue(dados)
            } else {
                _erro.postValue("Erro ao carregar transações do mês.")
            }
        }
    }

    fun carregarOrcamentosCategoria() {
        repository.carregarOrcamentosPorCategoria { sucesso, dados ->
            if (sucesso) {
                _orcamentosCategoria.postValue(dados)
            } else {
                _erro.postValue("Erro ao carregar orçamentos por categoria.")
            }
        }
    }

    fun salvarOrcamentoCategoria(categoria: String, valor: Double, onComplete: (Boolean) -> Unit) {
        repository.salvarOrcamentoCategoria(categoria, valor) {
            onComplete(it)
        }
    }

    fun limparErro() {
        _erro.postValue(null)
    }
}