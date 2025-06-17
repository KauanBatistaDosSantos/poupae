package com.unasp.poupae.view.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.unasp.poupae.R
import com.unasp.poupae.viewmodel.HomeViewModel
import com.unasp.poupae.repository.TransacaoRepository
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import androidx.core.graphics.toColorInt

class HomeFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var tvSaldoAtual: TextView
    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        pieChart = view.findViewById(R.id.graficoPorCategoria)
        tvSaldoAtual = view.findViewById(R.id.tvSaldoAtual)

        val repository = TransacaoRepository()
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(repository) as T
            }
        })[HomeViewModel::class.java]

        setupObservers()

        viewModel.iniciarResumo()

        return view
    }

    private fun setupObservers() {
        viewModel.saldo.observe(viewLifecycleOwner) {
            tvSaldoAtual.text = String.format("R$ %.2f", it)
        }

        viewModel.entradasGrafico.observe(viewLifecycleOwner) { entradas ->
            val dataSet = PieDataSet(entradas, "Categorias")
            dataSet.colors = listOf(
                "#4CAF50".toColorInt(),
                "#00796B".toColorInt(),
                "#03A9F4".toColorInt(),
                "#E91E63".toColorInt(),
                "#FF5722".toColorInt(),
                "#9C27B0".toColorInt(),
                "#FFC107".toColorInt()
            )
            dataSet.valueTextSize = 12f
            dataSet.valueTextColor = Color.BLACK

            pieChart.data = PieData(dataSet)
            pieChart.setUsePercentValues(true)
            pieChart.description.isEnabled = false
            pieChart.isDrawHoleEnabled = true
            pieChart.setHoleColor(Color.TRANSPARENT)
            pieChart.transparentCircleRadius = 0f
            pieChart.setEntryLabelColor(Color.BLACK)
            pieChart.setEntryLabelTextSize(12f)
            pieChart.legend.isEnabled = false
            pieChart.animateY(1000)
            pieChart.invalidate()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.iniciarResumo()
    }
}
