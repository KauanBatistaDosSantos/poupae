package com.unasp.poupae.model
import com.google.firebase.Timestamp

data class Transacao(
    var id: String? = null,
    val categoria: String = "",
    val valor: Double = 0.0,
    val descricao: String = "",
    val tipo: String = "",
    val data: Timestamp? = null,
    val recorrente: Boolean = false,
    val frequencia: String? = null,
    val tipoMeta: Boolean = false,
    val nome: String = ""
)