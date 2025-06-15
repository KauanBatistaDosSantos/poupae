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
        adapter = TransacaoAdapter(requireContext(), listaTransacoes) { id, transacao ->
            EditTransactionDialog(id, transacao) {
                carregarTransacoes()
            }.show(parentFragmentManager, "editDialog")
        }
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

                for (doc in documentos) {
                    val transacao = doc.toObject(Transacao::class.java)
                    listaTransacoes.add(Pair(doc.id, transacao))

                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val data = sdf.parse(transacao.data)
                    val calendar = Calendar.getInstance().apply { time = data }
                    val mes = calendar.get(Calendar.MONTH)
                    val valor = transacao.valor.toFloat()

                    if (transacao.tipo == "ganho") {
                        ganhosPorMes[mes] = ganhosPorMes.getOrDefault(mes, 0f) + valor
                    } else if (transacao.tipo == "despesa") {
                        despesasPorMes[mes] = despesasPorMes.getOrDefault(mes, 0f) + valor
                    }
                }

                gerarGrafico(ganhosPorMes, despesasPorMes)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao carregar transações", Toast.LENGTH_SHORT).show()
            }
    }

    private fun gerarGrafico(ganhos: Map<Int, Float>, despesas: Map<Int, Float>) {
        val entriesGanhos = mutableListOf<BarEntry>()
        val entriesDespesas = mutableListOf<BarEntry>()
        val labelsCompletos = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
        val mesAtual = Calendar.getInstance().get(Calendar.MONTH)
        val labelsVisiveis = labelsCompletos.subList(0, mesAtual + 1)

        for (i in 0..mesAtual) {
            entriesGanhos.add(BarEntry(i.toFloat(), ganhos.getOrDefault(i, 0f)))
            entriesDespesas.add(BarEntry(i.toFloat(), despesas.getOrDefault(i, 0f)))
        }

        val dataSetGanhos = BarDataSet(entriesGanhos, "Entradas").apply {
            color = ColorTemplate.MATERIAL_COLORS[0]
        }
        val dataSetDespesas = BarDataSet(entriesDespesas, "Saídas").apply {
            color = ColorTemplate.MATERIAL_COLORS[1]
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
            private val formato = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
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
    }
}
