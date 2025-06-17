package com.unasp.poupae.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OrcamentoRepository {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    fun carregarOrcamento(onResult: (Boolean, List<Map<String, Any>>) -> Unit) {
        if (userId == null) {
            onResult(false, emptyList())
            return
        }

        db.collection("users").document(userId)
            .collection("orcamento_recorrente")
            .get()
            .addOnSuccessListener { result ->
                val lista = result.map { it.data }
                onResult(true, lista)
            }
            .addOnFailureListener {
                onResult(false, emptyList())
            }
    }

    fun carregarTransacoesDesde(dataInicio: Timestamp, onResult: (Boolean, List<Map<String, Any>>) -> Unit) {
        if (userId == null) {
            onResult(false, emptyList())
            return
        }

        db.collection("users").document(userId)
            .collection("transacoes")
            .whereGreaterThanOrEqualTo("data", dataInicio)
            .get()
            .addOnSuccessListener { result ->
                val lista = result.map { it.data }
                onResult(true, lista)
            }
            .addOnFailureListener {
                onResult(false, emptyList())
            }
    }

    fun carregarOrcamentosPorCategoria(onResult: (Boolean, List<Map<String, Any>>) -> Unit) {
        if (userId == null) {
            onResult(false, emptyList())
            return
        }

        db.collection("users").document(userId)
            .collection("orcamento_categoria")
            .get()
            .addOnSuccessListener { result ->
                val lista = result.map { it.data }
                onResult(true, lista)
            }
            .addOnFailureListener {
                onResult(false, emptyList())
            }
    }

    fun salvarOrcamentoCategoria(categoria: String, valor: Double, onComplete: (Boolean) -> Unit) {
        if (userId == null) {
            onComplete(false)
            return
        }

        val dados = mapOf(
            "categoria" to categoria.replaceFirstChar { it.uppercaseChar() },
            "valorLimite" to valor
        )

        db.collection("users").document(userId)
            .collection("orcamento_categoria")
            .document(categoria.lowercase())
            .set(dados)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}