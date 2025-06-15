package com.unasp.poupae.model
import com.google.firebase.Timestamp

data class Transacao(
    var id: String? = null,         // ← ESSENCIAL
    val categoria: String = "",
    val valor: Double = 0.0,
    val descricao: String = "",
    val tipo: String = "",
    val data: Timestamp? = null, // ← aqui está o ajuste principal
    val recorrente: Boolean = false, // NOVO
    val frequencia: String? = null,   // NOVO: ex "mensal", "semanal", etc
    val tipoMeta: Boolean = false
)