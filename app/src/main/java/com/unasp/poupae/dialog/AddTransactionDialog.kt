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
                    db.collection("users").document(uid)
                        .collection("metas")
                        .get()
                        .addOnSuccessListener { metas ->
                            for (doc in metas) {
                                val nomeMeta = doc.getString("nome") ?: continue
                                val nomeCompleto = "[META] $nomeMeta"
                                if (!categoriasCombinadas.contains(nomeCompleto)) {
                                    categoriasCombinadas.add(categoriasCombinadas.size - 1, nomeCompleto)
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

            override fun onNothingSelected(parent: AdapterView<*>?) {}
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
            .setPositiveButton("Salvar", null) // Define manualmente depois
            .setNegativeButton("Cancelar") { _, _ ->
                spinnerCategoria.setSelection(0)
            }
            .create().also { dialog ->
                dialog.setOnShowListener {
                    val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    button.setOnClickListener {
                        val categoria = spinnerCategoria.selectedItem.toString()
                        val valor = inputValor.text.toString().toDoubleOrNull()
                        val descricao = inputDescricao.text.toString().trim()
                        val tipo = spinnerTipo.selectedItem.toString().lowercase(Locale.getDefault())
                        val recorrente = checkRecorrente.isChecked

                        if (valor != null && userId != null && categoria != "Selecione uma categoria" && categoria != "+ Adicionar nova categoria") {
                            if (tipo == "ganho" && categoria == "Salário") {
                                verificarSalarioJaRegistrado(valor) {
                                    salvarTransacao(userId, categoria, valor, descricao, tipo, recorrente)
                                    dismiss() // Fecha só se confirmado
                                }
                            } else if (tipo == "despesa") {
                                registrarDespesaComValidacao(valor) {
                                    salvarTransacao(userId, categoria, valor, descricao, tipo, recorrente)
                                    dismiss()
                                }
                            } else {
                                salvarTransacao(userId, categoria, valor, descricao, tipo, recorrente)
                                dismiss()
                            }
                        } else {
                            Toast.makeText(requireContext(), "Preencha os campos corretamente!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }

    private fun salvarTransacao(userId: String, categoria: String, valor: Double, descricao: String, tipo: String, recorrente: Boolean) {
        val db = FirebaseFirestore.getInstance()
        val dataFormatada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val transacao = hashMapOf(
            "categoria" to categoria,
            "valor" to valor,
            "descricao" to descricao,
            "tipo" to tipo,
            "data" to dataFormatada,
            "recorrente" to recorrente,
            "tipoMeta" to categoria.startsWith("[META]")
        )

        db.collection("users").document(userId)
            .collection("transacoes")
            .add(transacao)
            .addOnSuccessListener {
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
                }
            }
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
                    spinner.setSelection(0)
                }
            }
            .setNegativeButton("Cancelar") { _, _ ->
                spinner.setSelection(0)
            }
            .show()
    }

    fun verificarSalarioJaRegistrado(valor: Double, onContinuar: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val inicioMes = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val formatoData = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dataInicioMes = formatoData.format(inicioMes.time)

        db.collection("users").document(userId)
            .collection("transacoes")
            .whereEqualTo("tipo", "ganho")
            .whereEqualTo("categoria", "Salário")
            .whereGreaterThanOrEqualTo("data", dataInicioMes)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    onContinuar()
                } else {
                    AlertDialog.Builder(requireActivity())
                        .setTitle("Salário já registrado")
                        .setMessage("O salário deste mês já foi registrado. Deseja registrar novamente?")
                        .setPositiveButton("Sim") { _, _ -> onContinuar() }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
    }

    fun registrarDespesaComValidacao(valorDespesa: Double, onContinuar: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        calcularSaldoRealDoUsuario { saldoAtual ->
            if (valorDespesa > saldoAtual) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Saldo insuficiente")
                    .setMessage("Você não possui saldo suficiente para registrar esta despesa.")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                onContinuar()
            }
        }
    }

    fun calcularSaldoRealDoUsuario(onResult: (Double) -> Unit) {
        // TODO: Implemente ou chame a lógica real de cálculo do saldo
        onResult(1000.0) // Substitua isso pela lógica real
    }
}