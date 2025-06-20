package com.unasp.poupae.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.unasp.poupae.R
import com.unasp.poupae.model.Meta
import java.text.SimpleDateFormat
import java.util.*
import android.app.AlertDialog
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.unasp.poupae.repository.MetaRepository
import com.unasp.poupae.viewmodel.MetaDetalhesViewModel
import com.unasp.poupae.viewmodel.MetaDetalhesViewModelFactory
import com.unasp.poupae.viewmodel.MetasViewModel

class MetaDetalhesFragment : Fragment() {

    private lateinit var meta: Meta
    private lateinit var viewModel: MetaDetalhesViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            meta = it.getSerializable("meta") as Meta
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_detalhes_meta, container, false)

        val factory = MetaDetalhesViewModelFactory(MetaRepository())
        viewModel = ViewModelProvider(this, factory)[MetaDetalhesViewModel::class.java]

        val txtNome = view.findViewById<TextView>(R.id.txtDetalheNome)
        val txtValorAlvo = view.findViewById<TextView>(R.id.txtDetalheValorAlvo)
        val txtValorAtual = view.findViewById<TextView>(R.id.txtDetalheValorAtual)
        val txtDataLimite = view.findViewById<TextView>(R.id.txtDetalheDataLimite)
        val txtDataCriacao = view.findViewById<TextView>(R.id.txtDetalheDataCriacao)

        val btnAdicionar = view.findViewById<Button>(R.id.btnAdicionarValor)
        val btnRetirar = view.findViewById<Button>(R.id.btnRetirarValor)

        val btnExcluir = view.findViewById<Button>(R.id.btnExcluirMeta)

        val btnEditar = view.findViewById<Button>(R.id.btnEditarMeta)

        val btnVoltar = view.findViewById<ImageButton>(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnEditar.setOnClickListener {
            mostrarDialogoEditarMeta()
        }


        btnAdicionar.setOnClickListener {
            mostrarDialogoAlterarValor(true) // true = adicionar
        }

        btnRetirar.setOnClickListener {
            mostrarDialogoAlterarValor(false) // false = retirar
        }

        btnExcluir.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.excluir_meta_titulo))
                .setMessage(getString(R.string.excluir_meta_mensagem))
                .setPositiveButton(getString(R.string.sim)) { _, _ -> excluirMeta() }
                .setNegativeButton(getString(R.string.cancelar), null)
                .show()
        }


        txtNome.text = meta.nome
        txtValorAlvo.text = "R$ %.2f".format(meta.valorAlvo)
        txtValorAtual.text = "R$ %.2f".format(meta.valorAtual)
        txtDataLimite.text = meta.dataLimite ?: "--"
        txtDataCriacao.text = meta.criadoEm.ifBlank { "--" }
        return view
    }

    private fun mostrarDialogoAlterarValor(adicionar: Boolean) {
        val context = view?.context ?: return
        val input = EditText(context).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = getString(R.string.hint_valor)
        }

        val titulo = if (adicionar) getString(R.string.adicionar_valor_meta)
        else getString(R.string.retirar_valor_meta)

        val fator = if (adicionar) 1 else -1

        AlertDialog.Builder(context)
            .setTitle(titulo)
            .setView(input)
            .setPositiveButton(getString(R.string.confirmar)) { _, _ ->
                val valor = input.text.toString().toDoubleOrNull()
                if (valor != null && valor > 0) {
                    val novoValor = meta.valorAtual + (valor * fator)

                    viewModel.atualizarValor(meta.id, meta.valorAtual, valor, adicionar) { sucesso ->
                        if (sucesso) {
                            meta.valorAtual = novoValor
                            view?.findViewById<TextView>(R.id.txtDetalheValorAtual)?.text =
                                "R$ %.2f".format(novoValor)

                            viewModel.registrarTransacaoMeta(meta.nome, valor, adicionar)

                            Toast.makeText(context,
                                if (adicionar) getString(R.string.valor_adicionado)
                                else getString(R.string.valor_retirado),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(context, getString(R.string.erro_generico), Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, getString(R.string.valor_invalido), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    private fun excluirMeta() {
        viewModel.excluirMetaComTransacoes(meta.id, meta.nome) { sucesso ->
            if (sucesso) {
                Toast.makeText(requireContext(), getString(R.string.meta_excluida_sucesso), Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
                requireActivity().findViewById<View>(R.id.detailContainer)?.visibility = View.GONE
            } else {
                Toast.makeText(requireContext(), getString(R.string.erro_excluir_registros), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoEditarMeta() {
        val context = requireContext()
        val layout = LayoutInflater.from(context).inflate(R.layout.dialog_editar_meta, null)

        val edtNome = layout.findViewById<EditText>(R.id.edtNomeMeta)
        val edtValor = layout.findViewById<EditText>(R.id.edtValorAlvoMeta)
        val edtData = layout.findViewById<EditText>(R.id.edtDataLimiteMeta)

        edtNome.setText(meta.nome)
        edtValor.setText(meta.valorAlvo.toString())
        edtData.setText(meta.dataLimite)

        AlertDialog.Builder(context)
            .setTitle(getString(R.string.editar_meta))
            .setView(layout)
            .setPositiveButton(getString(R.string.salvar)) { _, _ ->
                val novoNome = edtNome.text.toString()
                val novoValorAlvo = edtValor.text.toString().toDoubleOrNull() ?: meta.valorAlvo
                val novaData = edtData.text.toString()

                viewModel.atualizarMeta(meta.id, novoNome, novoValorAlvo, novaData) { sucesso ->
                    if (sucesso) {
                        meta.nome = novoNome
                        meta.valorAlvo = novoValorAlvo
                        meta.dataLimite = novaData
                        atualizarTela()
                        Toast.makeText(context, getString(R.string.meta_atualizada), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, getString(R.string.erro_editar_meta), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    private fun atualizarTela() {
        view?.findViewById<TextView>(R.id.txtDetalheNome)?.text = meta.nome
        view?.findViewById<TextView>(R.id.txtDetalheValorAlvo)?.text = "R$ %.2f".format(meta.valorAlvo)
        view?.findViewById<TextView>(R.id.txtDetalheDataLimite)?.text = meta.dataLimite ?: "--"
    }

    companion object {
        fun newInstance(meta: Meta): MetaDetalhesFragment {
            val fragment = MetaDetalhesFragment()
            val args = Bundle()
            args.putSerializable("meta", meta)
            fragment.arguments = args
            return fragment
        }
    }
}