package com.unasp.poupae.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.unasp.poupae.R
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionDialog : DialogFragment() {

    private val categoriasDespesa = listOf("Alimentação", "Transporte", "Lazer", "Saúde", "Educação", "Moradia", "Outros")
    private val categoriasGanho = listOf("Salário", "Freelancer", "Aluguel", "Venda", "Devolução", "Outros")
    private val categoriasPersonalizadas = mutableListOf<String>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null)

        val inputValor = view.findViewById<EditText>(R.id.inputValor)
        val inputDescricao = view.findViewById<EditText>(R.id.inputDescricao)
        val spinnerCategoria = view.findViewById<Spinner>(R.id.spinnerCategoria)
        val spinnerTipo = view.findViewById<Spinner>(R.id.spinnerTipo)
        val checkRecorrente = view.findViewById<CheckBox>(R.id.checkboxRecorrente)

        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        val tipoAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, listOf("Despesa", "Ganho"))
        tipoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipo.adapter = tipoAdapter

        val categoriasCombinadas = mutableListOf<String>()
        categoriasCombinadas.add("Selecione uma categoria")
        categoriasCombinadas.addAll(categoriasDespesa)
        categoriasCombinadas.add("+ Adicionar nova categoria")
        setupSpinner(spinnerCategoria, categoriasCombinadas)


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

                    // Adiciona metas como categorias
                    db.collection("users").document(uid)
                        .collection("metas")
                        .get()
                        .addOnSuccessListener { metas ->
                            for (doc in metas) {
                                val nomeMeta = doc.getString("nome") ?: continue
                                val nomeCompleto = "[META] $nomeMeta"
                                if (!categoriasCombinadas.contains(nomeCompleto)) {
                                    categoriasCombinadas.add(categoriasCombinadas.size - 1, nomeCompleto) // adiciona antes de "+ Adicionar nova categoria"
                                }
                            }
                            setupSpinner(spinnerCategoria, categoriasCombinadas)
                        }

                }
        }

        spinnerTipo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val tipoSelecionado = parent?.getItemAtPosition(position).toString()
                val listaBase = if (tipoSelecionado == "Despesa") categoriasDespesa else categoriasGanho
                val novaLista = mutableListOf<String>()
                novaLista.add("Selecione uma categoria")
                novaLista.addAll(listaBase)
                novaLista.addAll(categoriasPersonalizadas.filterNot { novaLista.contains(it) })
                if (tipoSelecionado == "Despesa") {
                    userId?.let { uid ->
                        db.collection("users").document(uid)
                            .collection("metas")
                            .get()
                            .addOnSuccessListener { metas ->
                                for (doc in metas) {
                                    val nomeMeta = doc.getString("nome") ?: continue
                                    val nomeCompleto = "[META] $nomeMeta"
                                    if (!novaLista.contains(nomeCompleto)) {
                                        novaLista.add(novaLista.size, nomeCompleto)
                                    }
                                }
                                novaLista.add("+ Adicionar nova categoria")
                                setupSpinner(spinnerCategoria, novaLista)
                            }
                    }
                } else {
                    novaLista.add("+ Adicionar nova categoria")
                    setupSpinner(spinnerCategoria, novaLista)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Não faz nada, mas precisa estar implementado
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
                val tipo = spinnerTipo.selectedItem.toString().lowercase(Locale.getDefault())
                val recorrente = checkRecorrente.isChecked

                if (valor != null && userId != null && categoria != "Selecione uma categoria" && categoria != "+ Adicionar nova categoria") {
                    val dataFormatada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                    val transacao = hashMapOf(
                        "categoria" to categoria,
                        "valor" to valor,
                        "descricao" to descricao,
                        "tipo" to tipo,
                        "data" to dataFormatada,
                        "recorrente" to recorrente
                    )

                    db.collection("users").document(userId)
                        .collection("transacoes")
                        .add(transacao)
                        .addOnSuccessListener {
                            view?.context?.let { ctx ->
                                Toast.makeText(ctx, "Transação salva!", Toast.LENGTH_SHORT).show()
                            }

                            // Se categoria for de meta, atualiza valorAtual
                            if (categoria.startsWith("[META] ")) {
                                val nomeMeta = categoria.removePrefix("[META] ")
                                db.collection("users").document(userId)
                                    .collection("metas")
                                    .whereEqualTo("nome", nomeMeta)
                                    .get()
                                    .addOnSuccessListener { result ->
                                        if (!result.isEmpty) {
                                            val docId = result.documents[0].id
                                            db.collection("users").document(userId)
                                                .collection("metas").document(docId)
                                                .update("valorAtual", FieldValue.increment(valor))
                                        }
                                    }
                            }

                            // Se for recorrente, salva também no orçamento
                            if (recorrente) {
                                val orcamentoRef = db.collection("users").document(userId)
                                    .collection("orcamento_recorrente")

                                val recorrenteItem = hashMapOf(
                                    "categoria" to categoria,
                                    "valor" to valor,
                                    "descricao" to descricao,
                                    "tipo" to tipo,
                                    "frequencia" to "mensal",
                                    "criadoEm" to FieldValue.serverTimestamp()
                                )

                                orcamentoRef.add(recorrenteItem)
                                    .addOnSuccessListener {
                                        view?.context?.let { ctx ->
                                            Toast.makeText(ctx, "Salvo como recorrente no orçamento!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        }
                        .addOnFailureListener {
                            view?.context?.let { ctx ->
                                Toast.makeText(ctx, "Erro ao salvar: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    view?.context?.let { ctx ->
                        Toast.makeText(ctx, "Preencha os campos corretamente!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar") { _, _ ->
                spinnerCategoria.setSelection(0) // ✅ referência correta ao spinner
            }
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
                } else {
                    spinner.setSelection(0) // volta para "Selecione uma categoria" se salvar vazio
                }
            }
            .setNegativeButton("Cancelar") { _, _ ->
                spinner.setSelection(0) // ✅ volta ao item padrão ao cancelar
            }
            .show()
    }
}
