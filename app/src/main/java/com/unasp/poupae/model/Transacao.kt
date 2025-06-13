package com.unasp.poupae.model

data class Transacao(
    val categoria: String = "",
    val valor: Double = 0.0,
    val descricao: String = "",
    val tipo: String = "",
    val data: String = ""
)