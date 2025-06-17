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
import java.util.Calendar
import com.google.firebase.Timestamp
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
            text = getString(R.string.definir_orcamento_categoria)
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
        context?.let { ctx ->
            val layout = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 20, 50, 10)
            }

            val inputCategoria = EditText(ctx).apply {
                hint = getString(R.string.hint_categoria)
            }
            val inputValor = EditText(ctx).apply {
                hint = getString(R.string.hint_valor)
                inputType =
                    android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            }

            layout.addView(inputCategoria)
            layout.addView(inputValor)

            AlertDialog.Builder(ctx)
                .setTitle(getString(R.string.titulo_novo_orcamento_categoria))
                .setView(layout)
                .setPositiveButton(getString(R.string.salvar)) { _, _ ->
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
                                Toast.makeText(
                                    ctx,
                                    getString(R.string.orcamento_salvo),
                                    Toast.LENGTH_SHORT
                                ).show()
                                carregarOrcamento()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    ctx,
                                    getString(R.string.erro_salvar_orcamento),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
                .setNegativeButton(getString(R.string.cancelar), null)
                .show()
        }
    }

    private fun carregarOrcamento() {
        if (userId == null || !isAdded || context == null) return

        db.collection("users").document(userId)
            .collection("orcamento_recorrente")
            .get()
            .addOnSuccessListener { docs ->
                if (!isAdded || context == null) return@addOnSuccessListener
                val ctx = requireContext()

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
                    val categoria = doc.getString("categoria") ?: ctx.getString(R.string.hint_categoria)
                    val descricao = doc.getString("descricao") ?: ""

                    val itemView = TextView(ctx)
                    itemView.text = "$categoria - R$ %.2f".format(valor) +
                            if (descricao.isNotBlank()) " ($descricao)" else ""
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
                textSaldoRecorrente.text = ctx.getString(R.string.saldo_recorrente, saldoRecorrente)

                calcularSaldoReal(saldoRecorrente, mapaPorCategoria)
            }
            .addOnFailureListener {
                if (!isAdded || context == null) return@addOnFailureListener
                Toast.makeText(requireContext(), getString(R.string.erro_carregar_orcamento, it.message ?: ""), Toast.LENGTH_SHORT).show()
            }
    }

    private fun calcularSaldoReal(saldoRecorrente: Double, mapaPorCategoria: Map<String, Double>) {
        if (!isAdded || context == null || userId == null) return
        val ctx = requireContext()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val dataInicioMes = calendar.time

        db.collection("users").document(userId)
            .collection("transacoes")
            .whereGreaterThanOrEqualTo("data", Timestamp(dataInicioMes))
            .get()
            .addOnSuccessListener { transacoes ->
                if (!isAdded || context == null) return@addOnSuccessListener

                var saldoReal = saldoRecorrente
                val gastosPorCategoria = mutableMapOf<String, Double>()

                for (transacao in transacoes) {
                    val tipo = transacao.getString("tipo") ?: continue
                    val valor = transacao.getDouble("valor") ?: continue
                    val categoria = transacao.getString("categoria") ?: ctx.getString(R.string.categoria)

                    if (tipo == "despesa") {
                        saldoReal -= valor
                        gastosPorCategoria[categoria] = gastosPorCategoria.getOrDefault(categoria, 0.0) + valor
                    }
                }

                textSaldoReal.text = ctx.getString(R.string.saldo_real, saldoReal)

                db.collection("users").document(userId)
                    .collection("orcamento_categoria")
                    .get()
                    .addOnSuccessListener { orcamentosCategoria ->
                        if (!isAdded || context == null) return@addOnSuccessListener

                        layoutPorCategoria.addView(TextView(ctx).apply {
                            text = ctx.getString(R.string.acompanhamento_titulo)
                            textSize = 14f
                        })

                        for (orc in orcamentosCategoria) {
                            val categoria = orc.getString("categoria") ?: continue
                            val limite = orc.getDouble("valorLimite") ?: 0.0
                            val gasto = gastosPorCategoria[categoria] ?: 0.0
                            val status = if (gasto <= limite)
                                ctx.getString(R.string.dentro_orcamento)
                            else
                                ctx.getString(R.string.fora_orcamento)

                            val resumo = TextView(ctx)
                            resumo.text = "$categoria: ${ctx.getString(R.string.gasto)} R$ %.2f / ${ctx.getString(R.string.limite)} R$ %.2f - $status"
                                .format(gasto, limite)
                            resumo.textSize = 14f
                            layoutPorCategoria.addView(resumo)
                        }
                    }
            }
            .addOnFailureListener {
                if (!isAdded || context == null) return@addOnFailureListener
                Toast.makeText(ctx, ctx.getString(R.string.erro_calcular_saldo_real, it.message ?: ""), Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoAdicionarOrcamentoCategoria() {
        if (!isAdded || context == null || userId == null) return

        val ctx = requireContext()
        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_orcamento_categoria, null)

        val spinnerCategoria = dialogView.findViewById<Spinner>(R.id.spinnerCategoria)
        val editValor = dialogView.findViewById<EditText>(R.id.editValor)

        if (spinnerCategoria == null || editValor == null) {
            Toast.makeText(ctx, ctx.getString(R.string.erro_layout_dialogo), Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(userId)
            .collection("transacoes")
            .get()
            .addOnSuccessListener { docs ->
                if (!isAdded || context == null) return@addOnSuccessListener

                val categorias = docs.mapNotNull { it.getString("categoria") }
                    .toSet()
                    .sorted()

                if (categorias.isEmpty()) {
                    Toast.makeText(ctx, ctx.getString(R.string.nenhuma_categoria), Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, categorias)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategoria.adapter = adapter

                AlertDialog.Builder(ctx)
                    .setTitle(ctx.getString(R.string.titulo_novo_orcamento_dialogo))
                    .setView(dialogView)
                    .setPositiveButton(ctx.getString(R.string.salvar)) { _, _ ->
                        val categoria = spinnerCategoria.selectedItem?.toString()?.trim() ?: return@setPositiveButton
                        val valorStr = editValor.text.toString().replace(",", ".")
                        val valor = valorStr.toDoubleOrNull()

                        if (valor != null) {
                            salvarOrcamentoCategoria(categoria, valor)
                        } else {
                            Toast.makeText(ctx, ctx.getString(R.string.erro_valor), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton(ctx.getString(R.string.cancelar), null)
                    .show()
            }
            .addOnFailureListener {
                if (!isAdded || context == null) return@addOnFailureListener
                Toast.makeText(ctx, ctx.getString(R.string.erro_buscar_categorias, it.message ?: ""), Toast.LENGTH_SHORT).show()
            }
    }

    private fun salvarOrcamentoCategoria(categoria: String, valor: Double) {
        if (!isAdded || context == null || userId == null) return
        val ctx = requireContext()

        val dados = mapOf(
            "categoria" to categoria.replaceFirstChar { it.uppercaseChar() },
            "valorLimite" to valor
        )

        db.collection("users").document(userId)
            .collection("orcamento_categoria")
            .document(categoria.lowercase()) // ID = nome da categoria em minúsculo
            .set(dados)
            .addOnSuccessListener {
                if (!isAdded || context == null) return@addOnSuccessListener
                Toast.makeText(ctx, ctx.getString(R.string.orcamento_salvo), Toast.LENGTH_SHORT).show()
                carregarOrcamentosCategoria() // Atualiza visualmente
            }
            .addOnFailureListener {
                if (!isAdded || context == null) return@addOnFailureListener
                Toast.makeText(ctx, ctx.getString(R.string.erro_salvar_generico, it.message ?: ""), Toast.LENGTH_SHORT).show()
            }
    }

    private fun carregarOrcamentosCategoria() {
        if (userId == null || !isAdded || context == null) return
        val ctx = requireContext()

        db.collection("users").document(userId)
            .collection("orcamento_categoria")
            .get()
            .addOnSuccessListener { documentos ->
                if (!isAdded || context == null) return@addOnSuccessListener
                layoutPorCategoria.removeAllViews()

                for (doc in documentos) {
                    val categoria = doc.getString("categoria") ?: continue
                    val valor = doc.getDouble("valorLimite") ?: continue

                    val view = TextView(ctx)
                    view.text = "$categoria - ${ctx.getString(R.string.valor_limite)} R$ %.2f".format(valor)
                    view.textSize = 14f
                    layoutPorCategoria.addView(view)
                }
            }
            .addOnFailureListener {
                if (!isAdded || context == null) return@addOnFailureListener
                Toast.makeText(ctx, ctx.getString(R.string.erro_buscar_categorias, it.message ?: ""), Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        carregarOrcamento()
    }
}
