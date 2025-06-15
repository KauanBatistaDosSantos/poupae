package com.unasp.poupae.view.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unasp.poupae.R
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var tvSaldoAtual: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        pieChart = view.findViewById(R.id.graficoPorCategoria)
        tvSaldoAtual = view.findViewById(R.id.tvSaldoAtual)

        aplicarTransacoesRecorrentes {
            carregarDadosDoFirestore()
        }

        return view
    }

    // Adicione esse trecho no seu HomeFragment.kt, logo antes de carregar as transações normais

    private fun aplicarTransacoesRecorrentes(onComplete: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val hoje = Calendar.getInstance()
        val diaAtual = hoje.get(Calendar.DAY_OF_MONTH)
        val dataHojeFormatada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(hoje.time)

        db.collection("users").document(userId)
            .collection("orcamento_recorrente")
            .get()
            .addOnSuccessListener { result ->
                val transacoesParaSalvar = mutableListOf<Map<String, Any>>()

                for (doc in result) {
                    val categoria = doc.getString("categoria") ?: continue
                    val descricao = doc.getString("descricao") ?: ""
                    val tipo = doc.getString("tipo") ?: continue
                    val valor = doc.getDouble("valor") ?: continue
                    val diaRecorrente = doc.getLong("dia")?.toInt() ?: continue
                    val ultimoRegistro = doc.getString("ultimoRegistro") ?: ""

                    if (diaAtual == diaRecorrente && ultimoRegistro != dataHojeFormatada) {
                        val novaTransacao = hashMapOf(
                            "categoria" to categoria,
                            "descricao" to descricao,
                            "tipo" to tipo,
                            "valor" to valor,
                            "data" to hoje.time, // salva como Timestamp do Firestore
                            "recorrente" to true
                        )
                        transacoesParaSalvar.add(novaTransacao)

                        // Atualiza o campo ultimoRegistro
                        db.collection("users").document(userId)
                            .collection("orcamento_recorrente")
                            .document(doc.id)
                            .update("ultimoRegistro", dataHojeFormatada)
                    }
                }

                // Salva as transações no histórico
                val transacoesRef = db.collection("users").document(userId).collection("transacoes")
                for (transacao in transacoesParaSalvar) {
                    transacoesRef.add(transacao)
                }

                onComplete()
            }
            .addOnFailureListener {
                onComplete() // continua mesmo com erro
            }
    }

    private fun carregarDadosDoFirestore() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("transacoes")
            .get()
            .addOnSuccessListener { result ->
                val mapa = mutableMapOf<String, Double>()
                var totalGastos = 0.0
                var totalGanhos = 0.0

                for (doc in result) {
                    val tipo = doc.getString("tipo")
                    val categoria = doc.getString("categoria") ?: continue
                    val valor = doc.getDouble("valor") ?: continue

                    if (tipo == "despesa" && !categoria.startsWith("[META")) {
                        mapa[categoria] = mapa.getOrDefault(categoria, 0.0) + valor
                    }

                    if (tipo == "despesa") {
                        totalGastos += valor
                    } else if (tipo == "ganho") {
                        totalGanhos += valor
                    }
                }

                val saldoAtual = totalGanhos - totalGastos
                tvSaldoAtual.text = String.format("Saldo Atual: R$ %.2f", saldoAtual)

                val total = mapa.values.sum()
                val entradas = mapa.map { (categoria, valor) ->
                    PieEntry((valor / total * 100).toFloat(), categoria)
                }

                val dataSet = PieDataSet(entradas, "Categorias")
                dataSet.colors = listOf(
                    Color.parseColor("#4CAF50"),
                    Color.parseColor("#00796B"),
                    Color.parseColor("#03A9F4"),
                    Color.parseColor("#E91E63"),
                    Color.parseColor("#FF5722"),
                    Color.parseColor("#9C27B0"),
                    Color.parseColor("#FFC107")
                )
                dataSet.valueTextSize = 12f
                dataSet.valueTextColor = Color.BLACK

                pieChart.data = PieData(dataSet)
                pieChart.setUsePercentValues(true)
                pieChart.description.isEnabled = false
                pieChart.isDrawHoleEnabled = true
                pieChart.setHoleColor(Color.TRANSPARENT)
                pieChart.setEntryLabelColor(Color.BLACK)
                pieChart.setEntryLabelTextSize(12f)
                pieChart.legend.orientation = Legend.LegendOrientation.VERTICAL
                pieChart.legend.isWordWrapEnabled = true
                val legend = pieChart.legend
                legend.isEnabled = false
                legend.form = Legend.LegendForm.CIRCLE
                legend.orientation = Legend.LegendOrientation.VERTICAL
                legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
                legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                legend.setDrawInside(false)
                legend.textSize = 12f
                pieChart.animateY(1000)
                pieChart.invalidate()
            }
    }
}
