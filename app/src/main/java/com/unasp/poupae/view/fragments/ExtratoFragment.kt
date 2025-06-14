package com.unasp.poupae.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unasp.poupae.R
import com.unasp.poupae.adapter.TransacaoAdapter
import com.unasp.poupae.dialog.EditTransactionDialog
import com.unasp.poupae.model.Transacao

class ExtratoFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransacaoAdapter
    private val listaTransacoes = mutableListOf<Pair<String, Transacao>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_extrato, container, false)

        recyclerView = view.findViewById(R.id.recyclerExtrato)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = TransacaoAdapter(requireContext(), listaTransacoes) { id, transacao ->
            EditTransactionDialog(id, transacao) {
                carregarTransacoes() // recarrega os dados após a edição
            }.show(parentFragmentManager, "editDialog")
        }
        recyclerView.adapter = adapter

        carregarTransacoes()

        return view
    }

    private fun carregarTransacoes() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("transacoes")
            .get()
            .addOnSuccessListener { documentos ->
                listaTransacoes.clear()
                for (doc in documentos) {
                    val transacao = doc.toObject(Transacao::class.java)
                    listaTransacoes.add(Pair(doc.id, transacao))
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao carregar transações", Toast.LENGTH_SHORT).show()
            }
    }
}