package com.unasp.poupae.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.unasp.poupae.R
import com.unasp.poupae.model.Meta
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MetaAdapter(
    private val context: Context,
    private val metas: List<Meta>,
    private val onMetaClick: (Meta) -> Unit
) : RecyclerView.Adapter<MetaAdapter.MetaViewHolder>() {

    class MetaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNome: TextView = view.findViewById(R.id.tvMetaNome)
        val txtData: TextView = view.findViewById(R.id.tvDataLimite)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBarMeta)
        val txtProgresso: TextView = view.findViewById(R.id.tvProgressoTexto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetaViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_meta, parent, false)
        return MetaViewHolder(view)
    }

    override fun getItemCount(): Int = metas.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MetaViewHolder, position: Int) {
        val meta = metas[position]
        holder.txtNome.text = meta.nome

        val progresso = ((meta.valorAtual / meta.valorAlvo) * 100).toInt().coerceAtMost(100)
        holder.progressBar.progress = progresso
        holder.txtProgresso.text = "$progresso%"

        val dataFormatada = meta.dataLimite ?: "--"
        holder.txtData.text = dataFormatada

        // Clique no card inteiro
        holder.itemView.setOnClickListener {
            onMetaClick(meta)
        }

        // Clique na data para alternar com dias restantes
        var alternandoData = false
        val handlerData = Handler(Looper.getMainLooper())
        holder.txtData.setOnClickListener {
            val dataLimite = meta.dataLimite
            if (dataLimite != null && !alternandoData) {
                alternandoData = true
                val diasRestantes = calcularDiasRestantes(dataLimite)
                holder.txtData.text = "$diasRestantes dias restantes"
                handlerData.postDelayed({
                    holder.txtData.text = dataLimite
                    alternandoData = false
                }, 30000)
            }
        }

        // Clique na barra para alternar tipo de exibição
        var estadoBarra = 0
        val handlerBarra = Handler(Looper.getMainLooper())
        holder.txtProgresso.setOnClickListener {
            estadoBarra = (estadoBarra + 1) % 3
            when (estadoBarra) {
                0 -> holder.txtProgresso.text = "$progresso%"
                1 -> holder.txtProgresso.text = "${100 - progresso}% faltam"
                2 -> holder.txtProgresso.text = "R$ %.2f".format(meta.valorAtual)
            }
            handlerBarra.removeCallbacksAndMessages(null)
            handlerBarra.postDelayed({
                estadoBarra = 0
                holder.txtProgresso.text = "$progresso%"
            }, 30000)
        }
    }

    private fun calcularDiasRestantes(dataLimite: String): Long {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dataFinal = sdf.parse(dataLimite) ?: return 0
            val hoje = Calendar.getInstance().time
            val diff = dataFinal.time - hoje.time
            TimeUnit.MILLISECONDS.toDays(diff)
        } catch (e: Exception) {
            0
        }
    }
}
