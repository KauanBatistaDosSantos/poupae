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
    private val onEditar: (String, Transacao) -> Unit,
    private val onTransacaoExcluida: () -> Unit
) : RecyclerView.Adapter<TransacaoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNome: TextView = view.findViewById(R.id.txtNome)
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
        holder.txtNome.text = transacao.nome
        holder.txtValor.text = "R$ %.2f".format(transacao.valor)
        val cor = if (transacao.tipo == "ganho") {
            context.getColor(R.color.verde_sucesso) // Exemplo: defina esse verde no colors.xml
        } else {
            context.getColor(R.color.vermelho_erro) // Cor já usada no XML
        }
        holder.txtValor.setTextColor(cor)
        holder.txtData.text = transacao.data?.toDate()?.let {
            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(it)
        } ?: ""

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
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("transacoes")
            .document(id)
            .get()
            .addOnSuccessListener { doc ->
                val fazParteMeta = doc.getBoolean("tipoMeta") == true
                if (fazParteMeta) {
                    Toast.makeText(
                        context,
                        "Essa transação pertence a uma meta. Exclua a meta para removê-la.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    db.collection("users").document(userId)
                        .collection("transacoes")
                        .document(id)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(
                                context,
                                "Transação excluída com sucesso!",
                                Toast.LENGTH_SHORT
                            ).show()
                            lista.removeAt(position)
                            notifyItemRemoved(position)

                            onTransacaoExcluida()
                        }
                }
            }
    }



    fun atualizarLista(novaLista: List<Pair<String, Transacao>>) {
        lista.clear()
        lista.addAll(novaLista)
        notifyDataSetChanged()
    }
}