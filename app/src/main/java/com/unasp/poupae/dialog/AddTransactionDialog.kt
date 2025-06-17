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

    private val categoriasDespesa by lazy {
        listOf(
            getString(R.string.cat_alimentacao),
            getString(R.string.cat_transporte),
            getString(R.string.cat_lazer),
            getString(R.string.cat_saude),
            getString(R.string.cat_educacao),
            getString(R.string.cat_moradia),
            getString(R.string.cat_outros)
        )
    }

    private val categoriasGanho by lazy {
        listOf(
            getString(R.string.cat_salario),
            getString(R.string.cat_freelancer),
            getString(R.string.cat_aluguel),
            getString(R.string.cat_venda),
            getString(R.string.cat_devolucao),
            getString(R.string.cat_outros)
        )
    }

    private val categoriasPersonalizadas = mutableListOf<String>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null)

        val inputValor = view.findViewById<EditText>(R.id.inputValor)
        val inputDescricao = view.findViewById<EditText>(R.id.inputDescricao)
        val spinnerCategoria = view.findViewById<Spinner>(R.id.spinnerCategoria)
        val spinnerTipo = view.findViewById<Spinner>(R.id.spinnerTipo)
        val checkRecorrente = view.findViewById<CheckBox>(R.id.checkboxRecorrente)

        val inputNome = view.findViewById<EditText>(R.id.inputNome)

        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        val tipoAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            listOf(getString(R.string.despesa), getString(R.string.ganho))
        )
        tipoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipo.adapter = tipoAdapter

        val categoriasCombinadas = mutableListOf<String>()
        categoriasCombinadas.add(getString(R.string.selecione_categoria))
        categoriasCombinadas.addAll(categoriasDespesa)
        categoriasCombinadas.add(getString(R.string.adicionar_categoria))
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
                val listaBase = if (tipoSelecionado == getString(R.string.despesa)) categoriasDespesa else categoriasGanho
                val novaLista = mutableListOf<String>()
                novaLista.add(getString(R.string.selecione_categoria))
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
                                novaLista.add(getString(R.string.adicionar_categoria))
                                setupSpinner(spinnerCategoria, novaLista)
                            }
                    }
                } else {
                    novaLista.add(getString(R.string.adicionar_categoria))
                    setupSpinner(spinnerCategoria, novaLista)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerCategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = spinnerCategoria.selectedItem.toString()
                if (item == getString(R.string.adicionar_categoria)) {
                    showNovaCategoriaDialog(spinnerCategoria)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle(getString(R.string.adicionar_transacao))
            .setPositiveButton(getString(R.string.salvar), null)
            .setNegativeButton(getString(R.string.cancelar)) { _, _ ->
                spinnerCategoria.setSelection(0)
            }
            .create().also { dialog ->
                dialog.setOnShowListener {
                    val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    button.setOnClickListener {
                        val nome = inputNome.text.toString().trim()
                        val categoria = spinnerCategoria.selectedItem.toString()
                        val valor = inputValor.text.toString().toDoubleOrNull()
                        val descricao = inputDescricao.text.toString().trim()
                        val tipo = spinnerTipo.selectedItem.toString().lowercase(Locale.getDefault())
                        val recorrente = checkRecorrente.isChecked

                        if (valor != null && userId != null &&
                            categoria != getString(R.string.selecione_categoria) &&
                            categoria != getString(R.string.adicionar_categoria)
                        ) {
                            if (tipo == getString(R.string.ganho).lowercase() && categoria == getString(R.string.cat_salario)) {
                                verificarSalarioJaRegistrado(valor) {
                                    salvarTransacao(userId, nome, categoria, valor, descricao, tipo, recorrente)
                                    dismiss()
                                }
                            } else if (tipo == getString(R.string.despesa).lowercase()) {
                                registrarDespesaComValidacao(valor) {
                                    salvarTransacao(userId, nome, categoria, valor, descricao, tipo, recorrente)
                                    dismiss()
                                }
                            } else {
                                salvarTransacao(userId, nome, categoria, valor, descricao, tipo, recorrente)
                                dismiss()
                            }
                        } else {
                            Toast.makeText(requireContext(), getString(R.string.preencha_campos), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }

    private fun salvarTransacao(
        userId: String,
        nome: String,
        categoria: String,
        valor: Double,
        descricao: String,
        tipo: String,
        recorrente: Boolean
    ) {
        val db = FirebaseFirestore.getInstance()
        val dataTimestamp = com.google.firebase.Timestamp.now() // Agora com precisão total

        val transacao = hashMapOf(
            "nome" to nome,
            "categoria" to categoria,
            "valor" to valor,
            "descricao" to descricao,
            "tipo" to tipo,
            "data" to dataTimestamp,
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
            .setTitle(getString(R.string.nova_categoria))
            .setView(input)
            .setPositiveButton(getString(R.string.salvar)) { _, _ ->
                val novaCategoria = input.text.toString().trim()
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setPositiveButton

                if (novaCategoria.isNotEmpty()) {
                    val categoriaObj = hashMapOf("nome" to novaCategoria)
                    FirebaseFirestore.getInstance()
                        .collection("users").document(uid)
                        .collection("categorias")
                        .add(categoriaObj)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), getString(R.string.categoria_adicionada), Toast.LENGTH_SHORT).show()
                            (spinner.adapter as ArrayAdapter<String>).insert(novaCategoria, spinner.adapter.count - 1)
                            spinner.setSelection(spinner.adapter.count - 2)
                        }
                } else {
                    spinner.setSelection(0)
                }
            }
            .setNegativeButton(getString(R.string.cancelar)) { _, _ ->
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
            .whereEqualTo("categoria", getString(R.string.cat_salario))
            .whereGreaterThanOrEqualTo("data", dataInicioMes)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    onContinuar()
                } else {
                    AlertDialog.Builder(requireActivity())
                        .setTitle(getString(R.string.salario_registrado_titulo))
                        .setMessage(getString(R.string.salario_registrado_mensagem))
                        .setPositiveButton(getString(R.string.sim)) { _, _ -> onContinuar() }
                        .setNegativeButton(getString(R.string.cancelar), null)
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
                    .setTitle(getString(R.string.saldo_insuficiente_titulo))
                    .setMessage(getString(R.string.saldo_insuficiente_mensagem))
                    .setPositiveButton(getString(R.string.ok), null)
                    .show()
            } else {
                onContinuar()
            }
        }
    }

    fun calcularSaldoRealDoUsuario(onResult: (Double) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val calendarioInicio = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dataInicio = calendarioInicio.time

        db.collection("users").document(userId)
            .collection("transacoes")
            .whereGreaterThanOrEqualTo("data", dataInicio)
            .get()
            .addOnSuccessListener { result ->
                var saldo = 0.0
                for (doc in result) {
                    val valor = doc.getDouble("valor") ?: continue
                    val tipo = doc.getString("tipo") ?: continue

                    if (tipo.lowercase(Locale.ROOT) == "ganho") {
                        saldo += valor
                    } else if (tipo.lowercase(Locale.ROOT) == "despesa") {
                        saldo -= valor
                    }
                }
                onResult(saldo)
            }
            .addOnFailureListener {
                onResult(0.0)
            }
    }
}