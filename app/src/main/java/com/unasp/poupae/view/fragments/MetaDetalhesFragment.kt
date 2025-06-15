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
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

        val btnAdicionar = view.findViewById<Button>(R.id.btnAdicionarValor)
        val btnRetirar = view.findViewById<Button>(R.id.btnRetirarValor)

        val btnExcluir = view.findViewById<Button>(R.id.btnExcluirMeta)

        val btnEditar = view.findViewById<Button>(R.id.btnEditarMeta)
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
                .setTitle("Excluir meta")
                .setMessage("Tem certeza que deseja excluir esta meta? O valor atual será retornado ao seu saldo.")
                .setPositiveButton("Sim") { _, _ ->
                    excluirMeta()
                }
                .setNegativeButton("Cancelar", null)
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
            hint = "Digite o valor"
        }

        val titulo = if (adicionar) "Adicionar valor à meta" else "Retirar valor da meta"
        val fator = if (adicionar) 1 else -1

        AlertDialog.Builder(context)
            .setTitle(titulo)
            .setView(input)
            .setPositiveButton("Confirmar") { _, _ ->
                val valor = input.text.toString().toDoubleOrNull()
                if (valor != null && valor > 0) {
                    val novoValor = meta.valorAtual + (valor * fator)

                    val db = FirebaseFirestore.getInstance()
                    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setPositiveButton

                    db.collection("users").document(userId)
                        .collection("metas").document(meta.id)
                        .update("valorAtual", novoValor)
                        .addOnSuccessListener {
                            meta.valorAtual = novoValor
                            view?.findViewById<TextView>(R.id.txtDetalheValorAtual)?.text =
                                "R$ %.2f".format(novoValor)

                            // REGISTRA A TRANSAÇÃO
                            val tipo = if (adicionar) "despesa" else "ganho"
                            val data = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                            val transacao = hashMapOf(
                                "categoria" to "[META] ${meta.nome}",
                                "valor" to valor,
                                "descricao" to if (adicionar) "Adicionado à meta" else "Retirado da meta",
                                "tipo" to tipo,
                                "data" to data,
                                "recorrente" to false
                            )

                            db.collection("users").document(userId)
                                .collection("transacoes")
                                .add(transacao)

                            Toast.makeText(
                                context,
                                if (adicionar) "Valor adicionado!" else "Valor retirado!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Erro: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Digite um valor válido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluirMeta() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val data = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val categoriaMeta = "[META] ${meta.nome}"

        // Primeiro: excluir todas as transações associadas à meta
        db.collection("users").document(userId)
            .collection("transacoes")
            .whereEqualTo("categoria", categoriaMeta)
            .whereEqualTo("tipoMeta", true)
            .get()
            .addOnSuccessListener { transacoesDocs ->
                val batch = db.batch()
                for (doc in transacoesDocs) {
                    batch.delete(doc.reference)
                }
                // Exclui o documento da meta
                val metaRef = db.collection("users").document(userId)
                    .collection("metas").document(meta.id)
                batch.delete(metaRef)

                // Commita todas as ações
                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Meta e registros excluídos com sucesso!", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Erro ao excluir registros da meta: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao buscar registros da meta: ${it.message}", Toast.LENGTH_SHORT).show()
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
            .setTitle("Editar meta")
            .setView(layout)
            .setPositiveButton("Salvar") { _, _ ->
                val novoNome = edtNome.text.toString()
                val novoValorAlvo = edtValor.text.toString().toDoubleOrNull() ?: meta.valorAlvo
                val novaData = edtData.text.toString()

                val atualizacoes = mapOf(
                    "nome" to novoNome,
                    "valorAlvo" to novoValorAlvo,
                    "dataLimite" to novaData
                )

                val db = FirebaseFirestore.getInstance()
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setPositiveButton

                db.collection("users").document(userId)
                    .collection("metas").document(meta.id)
                    .update(atualizacoes)
                    .addOnSuccessListener {
                        meta.nome = novoNome
                        meta.valorAlvo = novoValorAlvo
                        meta.dataLimite = novaData
                        atualizarTela()
                        Toast.makeText(context, "Meta atualizada!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Erro ao editar meta: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun atualizarTela() {
        view?.findViewById<TextView>(R.id.txtDetalheNome)?.text = meta.nome
        view?.findViewById<TextView>(R.id.txtDetalheValorAlvo)?.text = "R$ %.2f".format(meta.valorAlvo)
        view?.findViewById<TextView>(R.id.txtDetalheDataLimite)?.text = meta.dataLimite ?: "--"
    }

}
