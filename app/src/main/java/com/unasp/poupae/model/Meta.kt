package com.unasp.poupae.model

data class Meta(
    val id: String = "", // ID do documento Firestore
    val nome: String = "",
    val valorAlvo: Double = 0.0,
    val valorAtual: Double = 0.0,
    val dataLimite: String? = null, // Pode ser nula se for uma meta livre
    val criadoEm: String = ""
)