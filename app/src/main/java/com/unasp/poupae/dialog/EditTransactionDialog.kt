package com.unasp.poupae.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unasp.poupae.R
import com.unasp.poupae.model.Transacao

class EditTransactionDialog(
    private val idTransacao: String,
    private val transacao: Transacao,
    private val onAtualizado: () -> Unit
) : DialogFragment() {

    private val categoriasPadrao = listOf("Alimentação", "Transporte", "Lazer", "Saúde", "Educação", "Moradia", "Outros")
    private val categoriasPersonalizadas = mutableListOf<String>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = context ?: return super.onCreateDialog(savedInstanceState)
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_add_transaction, null)

        val inputValor = view.findViewById<EditText>(R.id.inputValor)
        val inputDescricao = view.findViewById<EditText>(R.id.inputDescricao)
        val inputNome = view.findViewById<EditText>(R.id.inputNome)
        inputNome.setText(transacao.nome)
        val spinnerCategoria = view.findViewById<Spinner>(R.id.spinnerCategoria)

        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        inputValor.setText(transacao.valor.toString())
        inputDescricao.setText(transacao.descricao ?: "")

        val categoriasCombinadas = categoriasPadrao.toMutableList()

        userId?.let { uid ->
            db.collection("users").document(uid)
                .collection("categorias")
                .get()
                .addOnSuccessListener { result ->
                    if (!isAdded || context == null) return@addOnSuccessListener

                    for (doc in result) {
                        val nome = doc.getString("nome") ?: continue
                        if (!categoriasCombinadas.contains(nome)) {
                            categoriasPersonalizadas.add(nome)
                            categoriasCombinadas.add(nome)
                        }
                    }
                    categoriasCombinadas.add("+ Adicionar nova categoria")

                    val safeContext = context
                    if (isAdded && safeContext != null) {
                        view.post {
                            if (isAdded && context != null) {
                                setupSpinner(safeContext, spinnerCategoria, categoriasCombinadas, transacao.categoria)
                            }
                        }
                    }
                }
        }

        spinnerCategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = spinnerCategoria.selectedItem.toString()
                if (item == "+ Adicionar nova categoria") {
                    showNovaCategoriaDialog(spinnerCategoria)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        return AlertDialog.Builder(requireActivity().takeIf { isAdded } ?: return super.onCreateDialog(savedInstanceState))
            .setView(view)
            .setTitle("Editar Transação")
            .setPositiveButton("Atualizar") { _, _ ->
                val categoria = spinnerCategoria.selectedItem.toString()
                val valor = inputValor.text.toString().toDoubleOrNull()
                val descricao = inputDescricao.text.toString().trim()

                if (categoria.isNotEmpty() && valor != null && userId != null) {
                    val nome = inputNome.text.toString().trim()

                    val transacaoAtualizada = hashMapOf(
                        "nome" to nome,
                        "categoria" to categoria,
                        "valor" to valor,
                        "descricao" to descricao,
                        "tipo" to transacao.tipo,
                        "data" to transacao.data
                    )

                    db.collection("users").document(userId)
                        .collection("transacoes").document(idTransacao)
                        .set(transacaoAtualizada)
                        .addOnSuccessListener {
                            val safeContext = context
                            if (isAdded && safeContext != null) {
                                Toast.makeText(safeContext, "Transação atualizada!", Toast.LENGTH_SHORT).show()
                            }
                            onAtualizado()
                        }
                        .addOnFailureListener {
                            val safeContext = context
                            if (isAdded && safeContext != null) {
                                Toast.makeText(safeContext, "Erro ao atualizar: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    val safeContext = context
                    if (isAdded && safeContext != null) {
                        Toast.makeText(safeContext, "Preencha os campos corretamente!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    private fun setupSpinner(
        ctx: Context,
        spinner: Spinner,
        categorias: List<String>,
        categoriaSelecionada: String
    ) {
        if (!isAdded) return

        val adapter = ArrayAdapter(ctx, R.layout.spinner_item, categorias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val index = categorias.indexOf(categoriaSelecionada)
        if (index >= 0) spinner.setSelection(index)
    }

    private fun showNovaCategoriaDialog(spinner: Spinner) {
        val ctx = context ?: return
        val input = EditText(ctx)

        AlertDialog.Builder(ctx)
            .setTitle("Nova Categoria")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                val novaCategoria = input.text.toString().trim()
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setPositiveButton

                if (novaCategoria.isNotEmpty()) {
                    val categoriaObj = hashMapOf("nome" to novaCategoria)

                    FirebaseFirestore.getInstance()
                        .collection("users").document(uid)
                        .collection("categorias")
                        .add(categoriaObj)
                        .addOnSuccessListener {
                            if (!isAdded || context == null) return@addOnSuccessListener
                            Toast.makeText(ctx, "Categoria adicionada!", Toast.LENGTH_SHORT).show()
                            (spinner.adapter as ArrayAdapter<String>).insert(novaCategoria, spinner.adapter.count - 1)
                            spinner.setSelection(spinner.adapter.count - 2)
                        }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
