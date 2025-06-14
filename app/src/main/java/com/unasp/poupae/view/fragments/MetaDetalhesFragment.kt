package com.unasp.poupae.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.unasp.poupae.R
import com.unasp.poupae.model.Meta
import java.text.SimpleDateFormat
import java.util.*

class MetaDetalhesFragment(private val meta: Meta) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_detalhes_meta, container, false)

        val txtNome = view.findViewById<TextView>(R.id.txtDetalheNome)
        val txtValorAlvo = view.findViewById<TextView>(R.id.txtDetalheValorAlvo)
        val txtValorAtual = view.findViewById<TextView>(R.id.txtDetalheValorAtual)
        val txtDataLimite = view.findViewById<TextView>(R.id.txtDetalheDataLimite)
        val txtDataCriacao = view.findViewById<TextView>(R.id.txtDetalheDataCriacao)

        txtNome.text = meta.nome
        txtValorAlvo.text = "R$ %.2f".format(meta.valorAlvo)
        txtValorAtual.text = "R$ %.2f".format(meta.valorAtual)
        txtDataLimite.text = meta.dataLimite ?: "--"
        txtDataCriacao.text = meta.criadoEm.ifBlank { "--" }
        return view
    }
}
