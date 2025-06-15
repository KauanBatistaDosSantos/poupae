package com.unasp.poupae.model

data class Meta(
    val id: String = "", // ID do documento Firestore
    var nome: String = "",
    var valorAlvo: Double = 0.0,
    var valorAtual: Double = 0.0,
    var dataLimite: String? = null, // Pode ser nula se for uma meta livre
    val criadoEm: String = ""
)