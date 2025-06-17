package com.unasp.poupae.view.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unasp.poupae.R
import com.unasp.poupae.model.Meta
import com.unasp.poupae.repository.MetaRepository
import com.unasp.poupae.viewmodel.MetasViewModel
import com.unasp.poupae.viewmodel.MetasViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

class AdicionarMetaDialogFragment(
    private val onMetaAdicionada: () -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_adicionar_meta, null)

        val etNome = view.findViewById<EditText>(R.id.etNomeMeta)
        val etValor = view.findViewById<EditText>(R.id.etValorAlvoMeta)
        val etData = view.findViewById<EditText>(R.id.etDataLimiteMeta)

        return AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.nova_meta))
            .setView(view)
            .setPositiveButton(getString(R.string.salvar)) { _, _ ->
                val nome = etNome.text.toString()
                val valor = etValor.text.toString().toDoubleOrNull() ?: 0.0
                val dataLimite = etData.text.toString().ifBlank { null }
                val criadoEm = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                val novaMeta = Meta(
                    nome = nome,
                    valorAlvo = valor,
                    valorAtual = 0.0,
                    dataLimite = dataLimite,
                    criadoEm = criadoEm
                )
                val viewModel = ViewModelProvider(
                    requireActivity(),
                    MetasViewModelFactory(MetaRepository())
                )[MetasViewModel::class.java]

                viewModel.adicionarMeta(novaMeta) { sucesso ->
                    if (sucesso) {
                        onMetaAdicionada()
                    } else {
                        Toast.makeText(requireContext(), "Erro ao salvar meta", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .create()
    }
}
