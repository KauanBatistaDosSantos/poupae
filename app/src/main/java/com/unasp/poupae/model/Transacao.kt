package com.unasp.poupae.model

data class Transacao(
    var id: String? = null,         // ← ESSENCIAL
    val categoria: String = "",
    val valor: Double = 0.0,
    val descricao: String = "",
    val tipo: String = "",
    val data: String = ""
)