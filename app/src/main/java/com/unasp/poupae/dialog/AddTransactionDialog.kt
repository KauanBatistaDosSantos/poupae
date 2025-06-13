package com.unasp.poupae.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unasp.poupae.R
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionDialog : DialogFragment() {

    private val categoriasPadrao = listOf("Alimentação", "Transporte", "Lazer", "Saúde", "Educação", "Moradia", "Outros")
    private val categoriasPersonalizadas = mutableListOf<String>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null)

        val inputValor = view.findViewById<EditText>(R.id.inputValor)
        val inputDescricao = view.findViewById<EditText>(R.id.inputDescricao)
        val spinnerCategoria = view.findViewById<Spinner>(R.id.spinnerCategoria)

        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        val categoriasCombinadas = categoriasPadrao.toMutableList()

        userId?.let { uid ->
            db.collection("users").document(uid)
                .collection("categorias")
                .get()
                .addOnSuccessListener { result ->
                    for (doc in result) {
                        val nome = doc.getString("nome") ?: continue
                        if (!categoriasCombinadas.contains(nome)) {
                            categoriasPersonalizadas.add(nome)
                            categoriasCombinadas.add(nome)
                        }
                    }
                    categoriasCombinadas.add("+ Adicionar nova categoria")
                    setupSpinner(spinnerCategoria, categoriasCombinadas)
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

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("Adicionar Transação")
            .setPositiveButton("Salvar") { _, _ ->
                val categoria = spinnerCategoria.selectedItem.toString()
                val valor = inputValor.text.toString().toDoubleOrNull()
                val descricao = inputDescricao.text.toString().trim()

                if (categoria.isNotEmpty() && valor != null && userId != null) {
                    val dataFormatada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                    val transacao = hashMapOf(
                        "categoria" to categoria,
                        "valor" to valor,
                        "descricao" to descricao,
                        "tipo" to "despesa",
                        "data" to dataFormatada
                    )

                    db.collection("users").document(userId)
                        .collection("transacoes")
                        .add(transacao)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Transação salva!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Erro ao salvar: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "Preencha os campos corretamente!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    private fun setupSpinner(spinner: Spinner, categorias: List<String>) {
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, categorias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun showNovaCategoriaDialog(spinner: Spinner) {
        val input = EditText(requireContext())
        AlertDialog.Builder(requireContext())
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
                            Toast.makeText(requireContext(), "Categoria adicionada!", Toast.LENGTH_SHORT).show()
                            (spinner.adapter as ArrayAdapter<String>).insert(novaCategoria, spinner.adapter.count - 1)
                            spinner.setSelection(spinner.adapter.count - 2)
                        }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}