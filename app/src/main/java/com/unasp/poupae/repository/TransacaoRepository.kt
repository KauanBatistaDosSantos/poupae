package com.unasp.poupae.repository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unasp.poupae.model.Transacao
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class TransacaoRepository {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    suspend fun aplicarTransacoesRecorrentes() {
        if (userId == null) return
        val hoje = Calendar.getInstance()
        val diaAtual = hoje.get(Calendar.DAY_OF_MONTH)
        val dataHojeFormatada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(hoje.time)

        val result = db.collection("users").document(userId)
            .collection("orcamento_recorrente")
            .get()
            .await()

        val transacoesParaSalvar = mutableListOf<Map<String, Any>>()

        for (doc in result) {
            val categoria = doc.getString("categoria") ?: continue
            val descricao = doc.getString("descricao") ?: ""
            val tipo = doc.getString("tipo") ?: continue
            val valor = doc.getDouble("valor") ?: continue
            val diaRecorrente = doc.getLong("dia")?.toInt() ?: continue
            val ultimoRegistro = doc.getString("ultimoRegistro") ?: ""

            if (diaAtual == diaRecorrente && ultimoRegistro != dataHojeFormatada) {
                val novaTransacao = hashMapOf(
                    "categoria" to categoria,
                    "descricao" to descricao,
                    "tipo" to tipo,
                    "valor" to valor,
                    "data" to hoje.time,
                    "recorrente" to true
                )
                transacoesParaSalvar.add(novaTransacao)

                db.collection("users").document(userId)
                    .collection("orcamento_recorrente")
                    .document(doc.id)
                    .update("ultimoRegistro", dataHojeFormatada)
            }
        }

        val transacoesRef = db.collection("users").document(userId).collection("transacoes")
        for (transacao in transacoesParaSalvar) {
            transacoesRef.add(transacao)
        }
    }

    data class TransacaoResumo(
        val tipo: String,
        val categoria: String?,
        val valor: Double,
        val tipoMeta: Boolean
    )

    suspend fun carregarTransacoes(): List<TransacaoResumo> {
        if (userId == null) return emptyList()
        val result = db.collection("users").document(userId)
            .collection("transacoes")
            .get()
            .await()

        return result.mapNotNull { doc ->
            val tipo = doc.getString("tipo") ?: return@mapNotNull null
            val categoria = doc.getString("categoria")
            val valor = doc.getDouble("valor") ?: return@mapNotNull null
            val tipoMeta = doc.getBoolean("tipoMeta") ?: false
            TransacaoResumo(tipo, categoria, valor, tipoMeta)
        }
    }

    //funções do extrato
    suspend fun buscarTransacoesComId(): List<Pair<String, Transacao>> {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()

        val snapshot = FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .collection("transacoes")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            try {
                val transacao = doc.toObject(Transacao::class.java)
                if (transacao != null) {
                    // Força tipoMeta como true se existir no Firebase
                    val tipoMeta = doc.getBoolean("tipoMeta") ?: false
                    transacao.copy(tipoMeta = tipoMeta).let {
                        Pair(doc.id, it)
                    }
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

}
