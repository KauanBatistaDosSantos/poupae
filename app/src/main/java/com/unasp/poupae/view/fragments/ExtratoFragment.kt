package com.unasp.poupae.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unasp.poupae.R
import com.unasp.poupae.adapter.TransacaoAdapter
import com.unasp.poupae.dialog.EditTransactionDialog
import com.unasp.poupae.model.Transacao
import java.text.NumberFormat
import java.text.SimpleDateFormat
import android.graphics.Color
import java.util.*

class ExtratoFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransacaoAdapter
    private lateinit var barChart: BarChart
    private val listaTransacoes = mutableListOf<Pair<String, Transacao>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_extrato, container, false)

        recyclerView = view.findViewById(R.id.recyclerExtrato)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = TransacaoAdapter(
            requireContext(),
            listaTransacoes,
            onEditar = { id, transacao ->
                EditTransactionDialog(id, transacao) {
                    carregarTransacoes()
                }.show(parentFragmentManager, "editDialog")
            },
            onTransacaoExcluida = {
                carregarTransacoes() // ← Isso recarrega tudo, inclusive o gráfico e o saldo real
            }
        )
        recyclerView.adapter = adapter

        barChart = view.findViewById(R.id.barChart)

        carregarTransacoes()

        return view
    }

    private fun carregarTransacoes() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("transacoes")
            .get()
            .addOnSuccessListener { documentos ->
                listaTransacoes.clear()

                val ganhosPorMes = mutableMapOf<Int, Float>()
                val despesasPorMes = mutableMapOf<Int, Float>()
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                for (doc in documentos) {
                    val transacao = try {
                        doc.toObject(Transacao::class.java)
                    } catch (e: Exception) {
                        val dataString = doc.getString("data")
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val parsedDate = try {
                            dataString?.let { sdf.parse(it) }
                        } catch (ex: Exception) {
                            null
                        }

                        Transacao(
                            nome = doc.getString("nome") ?: "",
                            valor = doc.getDouble("valor") ?: 0.0,
                            tipo = doc.getString("tipo") ?: "",
                            data = parsedDate?.let { com.google.firebase.Timestamp(it) }
                        )
                    }
                    listaTransacoes.add(Pair(doc.id, transacao))

                    val data = transacao.data?.toDate() ?: continue
                    val calendar = Calendar.getInstance().apply { time = data }
                    val mes = calendar.get(Calendar.MONTH)
                    val valor = transacao.valor.toFloat()

                    if (transacao.tipo == "ganho") {
                        ganhosPorMes[mes] = ganhosPorMes.getOrDefault(mes, 0f) + valor
                    } else if (transacao.tipo == "despesa") {
                        despesasPorMes[mes] = despesasPorMes.getOrDefault(mes, 0f) + valor
                    }
                }

                listaTransacoes.sortByDescending { it.second.data?.toDate() }

                gerarGrafico(ganhosPorMes, despesasPorMes)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), getString(R.string.erro_carregar_transacoes), Toast.LENGTH_SHORT).show()
            }
    }

    private fun gerarGrafico(ganhos: Map<Int, Float>, despesas: Map<Int, Float>) {
        val entriesGanhos = mutableListOf<BarEntry>()
        val entriesDespesas = mutableListOf<BarEntry>()
        val labelsCompletos = listOf(
            getString(R.string.mes_janeiro),
            getString(R.string.mes_fevereiro),
            getString(R.string.mes_marco),
            getString(R.string.mes_abril),
            getString(R.string.mes_maio),
            getString(R.string.mes_junho),
            getString(R.string.mes_julho),
            getString(R.string.mes_agosto),
            getString(R.string.mes_setembro),
            getString(R.string.mes_outubro),
            getString(R.string.mes_novembro),
            getString(R.string.mes_dezembro)
        )
        val mesAtual = Calendar.getInstance().get(Calendar.MONTH)
        val labelsVisiveis = labelsCompletos.subList(0, mesAtual + 1)

        for (i in 0..mesAtual) {
            entriesGanhos.add(BarEntry(i.toFloat(), ganhos.getOrDefault(i, 0f)))
            entriesDespesas.add(BarEntry(i.toFloat(), despesas.getOrDefault(i, 0f)))
        }

        val dataSetGanhos = BarDataSet(entriesGanhos, getString(R.string.grafico_entradas)).apply {
            color = ColorTemplate.MATERIAL_COLORS[0]
            valueTextColor = Color.WHITE // ← muda a cor dos valores sobre as barras
            valueTextSize = 12f          // (opcional) aumenta um pouco a fonte
        }

        val dataSetDespesas = BarDataSet(entriesDespesas, getString(R.string.grafico_saidas)).apply {
            color = ColorTemplate.MATERIAL_COLORS[1]
            valueTextColor = Color.WHITE
            valueTextSize = 12f
        }

        val data = BarData(dataSetGanhos, dataSetDespesas).apply {
            barWidth = 0.4f
        }

        barChart.data = data
        barChart.description.isEnabled = false
        barChart.setFitBars(true)
        barChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = IndexAxisValueFormatter(labelsVisiveis)
            granularity = 1f
            labelCount = labelsVisiveis.size
            isGranularityEnabled = true
            setDrawGridLines(false)
            setAvoidFirstLastClipping(false)
        }

        val formatoBrasileiro = object : ValueFormatter() {
            private val formato = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).apply {
                maximumFractionDigits = 0
                minimumFractionDigits = 0
            }

            override fun getFormattedValue(value: Float): String {
                return formato.format(value.toDouble())
            }
        }


        barChart.axisLeft.apply {
            axisMinimum = 0f
            valueFormatter = formatoBrasileiro
        }

        barChart.axisRight.isEnabled = false
        barChart.groupBars(-0.5f, 0.2f, 0.02f)
        barChart.invalidate()

        // Fundo e área do gráfico brancos
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.setDrawBorders(false)
        barChart.setNoDataTextColor(Color.WHITE)

// Área de plotagem branca
        barChart.setGridBackgroundColor(Color.WHITE)
        barChart.legend.textColor = Color.WHITE
        barChart.xAxis.textColor = Color.WHITE
        barChart.axisLeft.textColor = Color.WHITE


// Remove legenda interna do gráfico
        barChart.legend.isEnabled = false

// Eixo X (inferior)
        barChart.xAxis.apply {
            textColor = Color.WHITE
            axisLineColor = Color.WHITE
            gridColor = Color.WHITE
            setDrawGridLines(false)
            gridLineWidth = 2f // engrossa as linhas horizontais
        }

// Eixo Y (esquerda)
        barChart.axisLeft.apply {
            textColor = Color.WHITE
            gridColor = Color.WHITE
            axisLineColor = Color.WHITE
            gridLineWidth = 2f // engrossa linhas horizontais
            axisLineWidth = 2f // engrossa linha vertical esquerda
            setDrawAxisLine(false) // ⬅ remove a linha vertical esquerda
        }

        barChart.axisLeft.valueFormatter = formatoBrasileiro

// Eixo Y (direita já desativado)
        barChart.axisRight.isEnabled = false

    }
}
