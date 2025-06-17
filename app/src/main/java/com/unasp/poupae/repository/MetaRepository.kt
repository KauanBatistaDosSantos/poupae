package com.unasp.poupae.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.unasp.poupae.model.Meta
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

class MetaRepository {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    suspend fun carregarMetas(): List<Meta> {
        if (userId == null) return emptyList()

        val snapshot = db.collection("users").document(userId)
            .collection("metas")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Meta::class.java)?.copy(id = doc.id)
        }
    }

    suspend fun adicionarMeta(meta: Meta): Boolean {
        if (userId == null) return false

        return try {
            db.collection("users").document(userId)
                .collection("metas")
                .add(meta)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun atualizarValorMeta(metaId: String, novoValor: Double) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("metas").document(metaId)
            .update("valorAtual", novoValor).await()
    }

    suspend fun atualizarMeta(metaId: String, nome: String, valorAlvo: Double, dataLimite: String?) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("metas").document(metaId)
            .update(
                mapOf(
                    "nome" to nome,
                    "valorAlvo" to valorAlvo,
                    "dataLimite" to dataLimite
                )
            ).await()
    }

    suspend fun excluirMetaETransacoes(metaId: String, nomeMeta: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val categoria = "[META] $nomeMeta"
        val transacoes = db.collection("users").document(userId)
            .collection("transacoes")
            .whereEqualTo("categoria", categoria)
            .whereEqualTo("tipoMeta", true)
            .get().await()

        val batch = db.batch()
        transacoes.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        val metaRef = db.collection("users").document(userId)
            .collection("metas").document(metaId)
        batch.delete(metaRef)

        batch.commit().await()
    }

    fun registrarTransacaoMeta(nomeMeta: String, valor: Double, adicionar: Boolean) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val tipo = if (adicionar) "despesa" else "ganho"
        val categoria = if (adicionar) "Meta: $nomeMeta" else "Meta: $nomeMeta (Retirada)"
        val dataAtual = Timestamp.now()

        val transacao = hashMapOf(
            "valor" to valor,
            "tipo" to tipo,
            "categoria" to categoria,
            "descricao" to "",
            "data" to dataAtual, // 👈 ESSENCIAL!
            "recorrente" to false,
            "tipoMeta" to true,
            "nome" to nomeMeta
        )

        db.collection("users").document(userId) // certo
            .collection("transacoes")
            .add(transacao)
            .addOnSuccessListener {
                // Atualiza o saldo do usuário:
                val ajuste = if (adicionar) -valor else valor
                db.collection("users").document(userId) // certo
                    .update("saldo", FieldValue.increment(ajuste))
            }
    }
}