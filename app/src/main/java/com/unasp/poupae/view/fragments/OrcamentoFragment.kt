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
import androidx.fragment.app.viewModels
import com.unasp.poupae.viewmodel.OrcamentoViewModel

class OrcamentoFragment : Fragment() {

    private lateinit var layoutGanhos: LinearLayout
    private lateinit var layoutDespesas: LinearLayout
    private lateinit var layoutPorCategoria: LinearLayout
    private lateinit var textSaldoRecorrente: TextView
    private lateinit var textSaldoReal: TextView
    private lateinit var btnAdicionarOrcamento: Button
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val viewModel: OrcamentoViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_orcamento, container, false)

        layoutGanhos = view.findViewById(R.id.layoutGanhos)
        layoutDespesas = view.findViewById(R.id.layoutDespesas)
        layoutPorCategoria = view.findViewById(R.id.layoutPorCategoria)
        textSaldoRecorrente = view.findViewById(R.id.textSaldoRecorrente)
        textSaldoReal = view.findViewById(R.id.textSaldoReal)
        btnAdicionarOrcamento = Button(requireContext()).apply {
            text = getString(R.string.definir_orcamento_categoria)
            setOnClickListener { mostrarDialogoAdicionarOrcamentoCategoria() } // ✅
        }

        val btnAdicionar = view.findViewById<Button>(R.id.btnAdicionarOrcamentoCategoria)
        btnAdicionar.setOnClickListener {
            mostrarDialogoAdicionarOrcamentoCategoria()
        }

        observarDados()

        return view
    }

    private fun observarDados() {
        viewModel.orcamentosRecorrentes.observe(viewLifecycleOwner) { orcamentos ->
            exibirOrcamentosRecorrentes(orcamentos)
            viewModel.carregarTransacoesDesdeInicioDoMes()
        }

        viewModel.transacoesDoMes.observe(viewLifecycleOwner) { transacoes ->
            val saldoRecorrente = calcularSaldoRecorrente(viewModel.orcamentosRecorrentes.value ?: emptyList())
            exibirSaldoReal(saldoRecorrente, transacoes)
        }

        viewModel.orcamentosCategoria.observe(viewLifecycleOwner) { orcamentos ->
            exibirOrcamentosCategoria(orcamentos)
        }

        viewModel.erro.observe(viewLifecycleOwner) { erro ->
            erro?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.limparErro()
            }
        }

        viewModel.carregarOrcamentosRecorrentes()
        viewModel.carregarOrcamentosCategoria()
    }

    private fun exibirOrcamentosRecorrentes(lista: List<Map<String, Any>>) {
        layoutGanhos.removeAllViews()
        layoutDespesas.removeAllViews()
        layoutPorCategoria.removeAllViews()
        if (layoutPorCategoria.indexOfChild(btnAdicionarOrcamento) == -1) {
            layoutPorCategoria.addView(btnAdicionarOrcamento)
        }
        for (orc in lista) {
            val tipo = orc["tipo"] as? String ?: continue
            val valor = orc["valor"] as? Double ?: continue
            val categoria = orc["categoria"] as? String ?: getString(R.string.hint_categoria)
            val descricao = orc["descricao"] as? String ?: ""

            val itemView = TextView(requireContext())
            itemView.text = "$categoria - R$ %.2f".format(valor) +
                    if (descricao.isNotBlank()) " ($descricao)" else ""
            itemView.textSize = 14f

            if (tipo == "ganho") {
                layoutGanhos.addView(itemView)
            } else {
                layoutDespesas.addView(itemView)
            }
        }

        val saldo = calcularSaldoRecorrente(lista)
        textSaldoRecorrente.text = getString(R.string.saldo_recorrente, saldo)
    }

    private fun calcularSaldoRecorrente(lista: List<Map<String, Any>>): Double {
        var ganhos = 0.0
        var despesas = 0.0
        for (orc in lista) {
            val tipo = orc["tipo"] as? String ?: continue
            val valor = orc["valor"] as? Double ?: continue
            if (tipo == "ganho") ganhos += valor else despesas += valor
        }
        return ganhos - despesas
    }

    private fun exibirSaldoReal(saldoRecorrente: Double, transacoes: List<Map<String, Any>>) {
        var saldoReal = saldoRecorrente
        val gastos = mutableMapOf<String, Double>()

        for (t in transacoes) {
            val tipo = t["tipo"] as? String ?: continue
            val valor = t["valor"] as? Double ?: continue
            val categoria = t["categoria"] as? String ?: getString(R.string.categoria)
            if (tipo == "despesa") {
                saldoReal -= valor
                gastos[categoria] = gastos.getOrDefault(categoria, 0.0) + valor
            }
        }

        textSaldoReal.text = getString(R.string.saldo_real, saldoReal)

        viewModel.orcamentosCategoria.value?.let { categorias ->
            layoutPorCategoria.addView(TextView(requireContext()).apply {
                text = getString(R.string.acompanhamento_titulo)
                textSize = 14f
            })

            for (orc in categorias) {
                val categoria = orc["categoria"] as? String ?: continue
                val limite = orc["valorLimite"] as? Double ?: continue
                val gasto = gastos[categoria] ?: 0.0
                val status = if (gasto <= limite)
                    getString(R.string.dentro_orcamento)
                else
                    getString(R.string.fora_orcamento)

                val resumo = TextView(requireContext())
                resumo.text = "$categoria: ${getString(R.string.gasto)} R$ %.2f / ${getString(R.string.limite)} R$ %.2f - $status"
                    .format(gasto, limite)
                resumo.textSize = 14f
                layoutPorCategoria.addView(resumo)
            }
        }
    }

    private fun exibirOrcamentosCategoria(lista: List<Map<String, Any>>) {
        layoutPorCategoria.removeAllViews()
        for (orc in lista) {
            val categoria = orc["categoria"] as? String ?: continue
            val valor = orc["valorLimite"] as? Double ?: continue
            val view = TextView(requireContext())
            view.text = "$categoria - ${getString(R.string.valor_limite)} R$ %.2f".format(valor)
            view.textSize = 14f
            layoutPorCategoria.addView(view)
        }
    }

    private fun mostrarDialogoAdicionarOrcamentoCategoria() {
        if (!isAdded || context == null || userId == null) return

        val ctx = requireContext()
        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_orcamento_categoria, null)

        val spinnerCategoria = dialogView.findViewById<Spinner>(R.id.spinnerCategoria)
        val editValor = dialogView.findViewById<EditText>(R.id.editValor)

        // 🟡 Carrega categorias únicas a partir das transações salvas
        db.collection("users").document(userId)
            .collection("transacoes")
            .get()
            .addOnSuccessListener { docs ->
                val categorias = docs.mapNotNull { it.getString("categoria") }
                    .toSet()
                    .sorted()

                if (categorias.isEmpty()) {
                    Toast.makeText(ctx, getString(R.string.nenhuma_categoria), Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // 🟢 Preenche o spinner com as categorias encontradas
                val adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, categorias)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategoria.adapter = adapter

                // Exibe o AlertDialog normalmente
                AlertDialog.Builder(ctx)
                    .setTitle(getString(R.string.titulo_novo_orcamento_categoria))
                    .setView(dialogView)
                    .setPositiveButton(getString(R.string.salvar)) { _, _ ->
                        val categoria = spinnerCategoria.selectedItem?.toString()?.trim() ?: return@setPositiveButton
                        val valor = editValor.text.toString().toDoubleOrNull() ?: return@setPositiveButton

                        viewModel.salvarOrcamentoCategoria(categoria, valor) { sucesso ->
                            if (sucesso) {
                                Toast.makeText(ctx, getString(R.string.orcamento_salvo), Toast.LENGTH_SHORT).show()
                                viewModel.carregarOrcamentosCategoria()
                            } else {
                                Toast.makeText(ctx, getString(R.string.erro_salvar_generico), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton(getString(R.string.cancelar), null)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(ctx, getString(R.string.erro_buscar_categorias), Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
    }
}
