package com.unasp.poupae.adapter

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unasp.poupae.R
import com.unasp.poupae.model.Transacao

class TransacaoAdapter(
    private val context: Context,
    private val lista: MutableList<Pair<String, Transacao>>,
    private val onEditar: (String, Transacao) -> Unit
) : RecyclerView.Adapter<TransacaoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtCategoria: TextView = view.findViewById(R.id.txtCategoria)
        val txtValor: TextView = view.findViewById(R.id.txtValor)
        val txtData: TextView = view.findViewById(R.id.txtData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transacao, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (id, transacao) = lista[position]
        holder.txtCategoria.text = transacao.categoria
        holder.txtValor.text = "R$ %.2f".format(transacao.valor)
        holder.txtData.text = transacao.data

        holder.itemView.setOnLongClickListener {
            AlertDialog.Builder(context)
                .setTitle("Transação")
                .setItems(arrayOf("Editar", "Excluir")) { dialog, which ->
                    when (which) {
                        0 -> onEditar(id, transacao)
                        1 -> excluirTransacao(id, position)
                    }
                }.show()
            true
        }
    }

    override fun getItemCount(): Int = lista.size

    private fun excluirTransacao(id: String, position: Int) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("transacoes").document(id)
            .delete()
            .addOnSuccessListener {
                lista.removeAt(position)
                notifyItemRemoved(position)
                Toast.makeText(context, "Transação excluída", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Erro ao excluir: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun atualizarLista(novaLista: List<Pair<String, Transacao>>) {
        lista.clear()
        lista.addAll(novaLista)
        notifyDataSetChanged()
    }
}