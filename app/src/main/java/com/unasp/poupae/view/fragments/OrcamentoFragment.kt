package com.unasp.poupae.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unasp.poupae.R

class OrcamentoFragment : Fragment() {

    private lateinit var layoutGanhos: LinearLayout
    private lateinit var layoutDespesas: LinearLayout
    private lateinit var textSaldoFinal: TextView
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_orcamento, container, false)

        layoutGanhos = view.findViewById(R.id.layoutGanhos)
        layoutDespesas = view.findViewById(R.id.layoutDespesas)
        textSaldoFinal = view.findViewById(R.id.textSaldoFinal)

        carregarOrcamento()

        return view
    }

    private fun carregarOrcamento() {
        if (userId == null) return

        db.collection("users").document(userId)
            .collection("orcamento_recorrente")
            .get()
            .addOnSuccessListener { docs ->
                var totalGanhos = 0.0
                var totalDespesas = 0.0

                layoutGanhos.removeAllViews()
                layoutDespesas.removeAllViews()

                for (doc in docs) {
                    val tipo = doc.getString("tipo") ?: continue
                    val valor = doc.getDouble("valor") ?: 0.0
                    val categoria = doc.getString("categoria") ?: "Categoria"
                    val descricao = doc.getString("descricao") ?: ""

                    val itemView = TextView(requireContext())
                    itemView.text = "$categoria - R$ %.2f".format(valor) + if (descricao.isNotBlank()) " (${descricao})" else ""
                    itemView.textSize = 14f

                    if (tipo == "ganho") {
                        totalGanhos += valor
                        layoutGanhos.addView(itemView)
                    } else {
                        totalDespesas += valor
                        layoutDespesas.addView(itemView)
                    }
                }

                val saldo = totalGanhos - totalDespesas
                textSaldoFinal.text = "Saldo Mensal: R$ %.2f".format(saldo)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao carregar orçamento: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
