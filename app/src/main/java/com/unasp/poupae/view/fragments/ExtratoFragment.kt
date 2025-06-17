package com.unasp.poupae.view.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
import com.unasp.poupae.R
import com.unasp.poupae.adapter.TransacaoAdapter
import com.unasp.poupae.dialog.EditTransactionDialog
import com.unasp.poupae.model.Transacao
import com.unasp.poupae.repository.TransacaoRepository
import com.unasp.poupae.viewmodel.ExtratoViewModel
import java.text.NumberFormat
import java.util.*

class ExtratoFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransacaoAdapter
    private lateinit var barChart: BarChart
    private lateinit var viewModel: ExtratoViewModel
    private val listaTransacoes = mutableListOf<Pair<String, Transacao>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_extrato, container, false)

        // Setup gráfico e lista
        barChart = view.findViewById(R.id.barChart)
        recyclerView = view.findViewById(R.id.recyclerExtrato)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = TransacaoAdapter(
            requireContext(),
            listaTransacoes,
            onEditar = { id, transacao ->
                EditTransactionDialog(id, transacao) {
                    viewModel.carregarTransacoes()
                }.show(parentFragmentManager, "editDialog")
            },
            onTransacaoExcluida = {
                viewModel.carregarTransacoes()
            }
        )
        recyclerView.adapter = adapter

        // Setup ViewModel
        val repository = TransacaoRepository()
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ExtratoViewModel(repository) as T
            }
        })[ExtratoViewModel::class.java]

        observarViewModel()

        viewModel.carregarTransacoes()

        return view
    }

    private fun observarViewModel() {
        viewModel.transacoes.observe(viewLifecycleOwner) { lista ->
            listaTransacoes.clear()
            listaTransacoes.addAll(lista)
            adapter.notifyDataSetChanged()
        }

        viewModel.ganhosPorMes.observe(viewLifecycleOwner) { ganhos ->
            viewModel.despesasPorMes.value?.let { despesas ->
                gerarGrafico(ganhos, despesas)
            }
        }

        viewModel.despesasPorMes.observe(viewLifecycleOwner) { despesas ->
            viewModel.ganhosPorMes.value?.let { ganhos ->
                gerarGrafico(ganhos, despesas)
            }
        }

        viewModel.erro.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
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
            valueTextColor = Color.WHITE
            valueTextSize = 12f
        }

        val dataSetDespesas = BarDataSet(entriesDespesas, getString(R.string.grafico_saidas)).apply {
            color = ColorTemplate.MATERIAL_COLORS[1]
            valueTextColor = Color.WHITE
            valueTextSize = 12f
        }

        val data = BarData(dataSetGanhos, dataSetDespesas).apply {
            barWidth = 0.4f
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

        barChart.data = data
        barChart.description.isEnabled = false
        barChart.setFitBars(true)
        barChart.groupBars(-0.5f, 0.2f, 0.02f)
        barChart.invalidate()
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.setDrawBorders(false)
        barChart.setNoDataTextColor(Color.WHITE)
        barChart.setGridBackgroundColor(Color.WHITE)
        barChart.legend.isEnabled = false

        barChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = IndexAxisValueFormatter(labelsVisiveis)
            granularity = 1f
            labelCount = labelsVisiveis.size
            isGranularityEnabled = true
            setDrawGridLines(false)
            setAvoidFirstLastClipping(false)
            textColor = Color.WHITE
            axisLineColor = Color.WHITE
            gridColor = Color.WHITE
            gridLineWidth = 2f
        }

        barChart.axisLeft.apply {
            axisMinimum = 0f
            valueFormatter = formatoBrasileiro
            textColor = Color.WHITE
            gridColor = Color.WHITE
            axisLineColor = Color.WHITE
            gridLineWidth = 2f
            axisLineWidth = 2f
            setDrawAxisLine(false)
        }

        barChart.axisRight.isEnabled = false
    }
}
