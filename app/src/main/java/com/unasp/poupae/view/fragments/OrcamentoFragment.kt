package com.unasp.poupae.view.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unasp.poupae.R
import java.text.SimpleDateFormat
import java.util.Locale

class OrcamentoFragment : Fragment() {

    private lateinit var layoutGanhos: LinearLayout
    private lateinit var layoutDespesas: LinearLayout
    private lateinit var layoutPorCategoria: LinearLayout
    private lateinit var textSaldoRecorrente: TextView
    private lateinit var textSaldoReal: TextView
    private lateinit var btnAdicionarOrcamento: Button
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_orcamento, container, false)

        layoutGanhos = view.findViewById(R.id.layoutGanhos)
        layoutDespesas = view.findViewById(R.id.layoutDespesas)
        layoutPorCategoria = view.findViewById(R.id.layoutPorCategoria)
        textSaldoRecorrente = view.findViewById(R.id.textSaldoRecorrente)
        textSaldoReal = view.findViewById(R.id.textSaldoReal)
        btnAdicionarOrcamento = Button(requireContext()).apply {
            text = "Definir orçamento por categoria"
            setOnClickListener { mostrarDialogoOrcamento() }
        }

        val btnAdicionar = view.findViewById<Button>(R.id.btnAdicionarOrcamentoCategoria)
        btnAdicionar.setOnClickListener {
            mostrarDialogoAdicionarOrcamentoCategoria()
        }

        layoutPorCategoria.addView(btnAdicionarOrcamento)

        carregarOrcamento()
        carregarOrcamentosCategoria()

        return view
    }

    private fun mostrarDialogoOrcamento() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 10)
        }

        val inputCategoria = EditText(requireContext()).apply {
            hint = "Categoria"
        }
        val inputValor = EditText(requireContext()).apply {
            hint = "Valor em R$"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        layout.addView(inputCategoria)
        layout.addView(inputValor)

        AlertDialog.Builder(requireContext())
            .setTitle("Novo orçamento por categoria")
            .setView(layout)
            .setPositiveButton("Salvar") { _, _ ->
                val categoria = inputCategoria.text.toString().trim()
                val valor = inputValor.text.toString().toDoubleOrNull() ?: 0.0

                if (categoria.isNotEmpty() && userId != null) {
                    val orcamentoData = hashMapOf(
                        "categoria" to categoria,
                        "valorLimite" to valor
                    )
                    db.collection("users").document(userId)
                        .collection("orcamento_categoria")
                        .document(categoria)
                        .set(orcamentoData)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Orçamento salvo!", Toast.LENGTH_SHORT).show()
                            carregarOrcamento()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Erro ao salvar orçamento.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun carregarOrcamento() {
        if (userId == null) return

        db.collection("users").document(userId)
            .collection("orcamento_recorrente")
            .get()
            .addOnSuccessListener { docs ->
                var totalGanhos = 0.0
                var totalDespesas = 0.0
                val mapaPorCategoria = mutableMapOf<String, Double>()

                layoutGanhos.removeAllViews()
                layoutDespesas.removeAllViews()
                layoutPorCategoria.removeAllViews()
                layoutPorCategoria.addView(btnAdicionarOrcamento)

                for (doc in docs) {
                    val tipo = doc.getString("tipo") ?: continue
                    val valor = doc.getDouble("valor") ?: 0.0
                    val categoria = doc.getString("categoria") ?: "Categoria"
                    val descricao = doc.getString("descricao") ?: ""

                    val itemView = TextView(requireContext())
                    itemView.text = "$categoria - R$ %.2f".format(valor) + if (descricao.isNotBlank()) " ($descricao)" else ""
                    itemView.textSize = 14f

                    mapaPorCategoria[categoria] = mapaPorCategoria.getOrDefault(categoria, 0.0) + valor

                    if (tipo == "ganho") {
                        totalGanhos += valor
                        layoutGanhos.addView(itemView)
                    } else {
                        totalDespesas += valor
                        layoutDespesas.addView(itemView)
                    }
                }

                val saldoRecorrente = totalGanhos - totalDespesas
                textSaldoRecorrente.text = "Saldo Recorrente: R$ %.2f".format(saldoRecorrente)

                calcularSaldoReal(saldoRecorrente, mapaPorCategoria)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao carregar orçamento: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calcularSaldoReal(saldoRecorrente: Double, mapaPorCategoria: Map<String, Double>) {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        val inicioMesFormatado = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        db.collection("users").document(userId!!)
            .collection("transacoes")
            .whereGreaterThanOrEqualTo("data", inicioMesFormatado)
            .get()
            .addOnSuccessListener { transacoes ->
                var saldoReal = saldoRecorrente
                val gastosPorCategoria = mutableMapOf<String, Double>()

                for (transacao in transacoes) {
                    val tipo = transacao.getString("tipo") ?: continue
                    val valor = transacao.getDouble("valor") ?: continue
                    val categoria = transacao.getString("categoria") ?: "Outros"

                    if (tipo == "despesa") {
                        saldoReal -= valor
                        gastosPorCategoria[categoria] = gastosPorCategoria.getOrDefault(categoria, 0.0) + valor
                    }
                }

                textSaldoReal.text = "Saldo Real: R$ %.2f".format(saldoReal)

                db.collection("users").document(userId!!)
                    .collection("orcamento_categoria")
                    .get()
                    .addOnSuccessListener { orcamentosCategoria ->
                        layoutPorCategoria.addView(TextView(requireContext()).apply {
                            text = "\n-- Acompanhamento do orçamento por categoria --"
                            textSize = 14f
                        })

                        for (orc in orcamentosCategoria) {
                            val categoria = orc.getString("categoria") ?: continue
                            val limite = orc.getDouble("valorLimite") ?: 0.0
                            val gasto = gastosPorCategoria[categoria] ?: 0.0
                            val status = if (gasto <= limite) "Dentro do orçamento" else "Ultrapassou o orçamento"

                            val resumo = TextView(requireContext())
                            resumo.text = "$categoria: Gasto R$ %.2f / Limite R$ %.2f - $status".format(gasto, limite)
                            resumo.textSize = 14f
                            layoutPorCategoria.addView(resumo)
                        }
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao calcular saldo real: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoAdicionarOrcamentoCategoria() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_orcamento_categoria, null)

        val spinnerCategoria = dialogView.findViewById<Spinner>(R.id.spinnerCategoria)
        val editValor = dialogView.findViewById<EditText>(R.id.editValor)

        if (spinnerCategoria == null || editValor == null) {
            Toast.makeText(requireContext(), "Erro ao carregar layout do diálogo.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(userId!!)
            .collection("transacoes")
            .get()
            .addOnSuccessListener { docs ->
                val categorias = docs.mapNotNull { it.getString("categoria") }
                    .toSet()
                    .sorted()

                if (categorias.isEmpty()) {
                    Toast.makeText(requireContext(), "Nenhuma categoria encontrada.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categorias)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategoria.adapter = adapter

                AlertDialog.Builder(requireContext())
                    .setTitle("Novo Orçamento por Categoria")
                    .setView(dialogView)
                    .setPositiveButton("Salvar") { _, _ ->
                        val categoria = spinnerCategoria.selectedItem?.toString()?.trim() ?: return@setPositiveButton
                        val valorStr = editValor.text.toString().replace(",", ".")
                        val valor = valorStr.toDoubleOrNull()

                        if (valor != null) {
                            salvarOrcamentoCategoria(categoria, valor)
                        } else {
                            Toast.makeText(requireContext(), "Preencha o valor corretamente.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao buscar categorias: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun salvarOrcamentoCategoria(categoria: String, valor: Double) {
        val dados = mapOf(
            "categoria" to categoria.replaceFirstChar { it.uppercaseChar() },
            "valorLimite" to valor
        )

        db.collection("users").document(userId!!)
            .collection("orcamento_categoria")
            .document(categoria.lowercase()) // ID = nome da categoria em minúsculo
            .set(dados)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Orçamento salvo!", Toast.LENGTH_SHORT).show()
                carregarOrcamentosCategoria() // Atualiza visualmente
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao salvar: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun carregarOrcamentosCategoria() {
        if (userId == null) return

        db.collection("users").document(userId)
            .collection("orcamento_categoria")
            .get()
            .addOnSuccessListener { documentos ->
                layoutPorCategoria.removeAllViews()

                for (doc in documentos) {
                    val categoria = doc.getString("categoria") ?: continue
                    val valor = doc.getDouble("valorLimite") ?: continue

                    val view = TextView(requireContext())
                    view.text = "$categoria - Limite: R$ %.2f".format(valor)
                    view.textSize = 14f
                    layoutPorCategoria.addView(view)
                }
            }
    }

}
