package com.unasp.poupae.view.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unasp.poupae.R
import com.unasp.poupae.model.Meta
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
            .setTitle("Nova Meta")
            .setView(view)
            .setPositiveButton("Salvar") { _, _ ->
                val nome = etNome.text.toString()
                val valor = etValor.text.toString().toDoubleOrNull() ?: 0.0
                val dataLimite = etData.text.toString().ifBlank { null }
                val criadoEm = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setPositiveButton

                val novaMeta = Meta(
                    nome = nome,
                    valorAlvo = valor,
                    valorAtual = 0.0,
                    dataLimite = dataLimite,
                    criadoEm = criadoEm
                )

                FirebaseFirestore.getInstance()
                    .collection("users").document(userId)
                    .collection("metas")
                    .add(novaMeta)
                    .addOnSuccessListener {
                        onMetaAdicionada()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }
}
