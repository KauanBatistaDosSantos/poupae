package com.unasp.poupae.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unasp.poupae.R
import com.unasp.poupae.adapter.MetaAdapter
import com.unasp.poupae.model.Meta
import com.unasp.poupae.view.dialogs.AdicionarMetaDialogFragment

class MetasFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MetaAdapter
    private val listaMetas = mutableListOf<Meta>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        val view = inflater.inflate(R.layout.fragment_metas, container, false)
        val btnAdicionarMeta  = view.findViewById<View>(R.id.btnAdicionarMeta )

        recyclerView = view.findViewById(R.id.recyclerMetas)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = MetaAdapter(requireContext(), listaMetas) { meta ->
            val fragment = MetaDetalhesFragment.newInstance(meta)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        recyclerView.adapter = adapter

        carregarMetas()

        btnAdicionarMeta .setOnClickListener {
            AdicionarMetaDialogFragment {
                carregarMetas()
            }.show(childFragmentManager, "novaMeta")
        }


        return view
    }

    private fun carregarMetas() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("metas")
            .get()
            .addOnSuccessListener { result ->
                listaMetas.clear()
                for (doc in result) {
                    val meta = doc.toObject(Meta::class.java).copy(id = doc.id)
                    listaMetas.add(meta)
                }
                adapter.notifyDataSetChanged()
            }
    }
}
