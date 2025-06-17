package com.unasp.poupae.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.unasp.poupae.repository.MetaRepository
import com.unasp.poupae.R
import com.unasp.poupae.adapter.MetaAdapter
import android.widget.Toast
import com.unasp.poupae.model.Meta
import com.unasp.poupae.view.dialogs.AdicionarMetaDialogFragment
import com.unasp.poupae.viewmodel.MetasViewModel

class MetasFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MetaAdapter
    private lateinit var viewModel: MetasViewModel
    private val listaMetas = mutableListOf<Meta>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_metas, container, false)
        val btnAdicionarMeta = view.findViewById<View>(R.id.btnAdicionarMeta)

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

        val repository = MetaRepository()
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MetasViewModel(repository) as T
            }
        })[MetasViewModel::class.java]

        observarViewModel()
        viewModel.carregarMetas()

        btnAdicionarMeta.setOnClickListener {
            AdicionarMetaDialogFragment {
                viewModel.carregarMetas()
            }.show(childFragmentManager, "novaMeta")
        }

        return view
    }

    private fun observarViewModel() {
        viewModel.metas.observe(viewLifecycleOwner) { novasMetas ->
            listaMetas.clear()
            listaMetas.addAll(novasMetas)
            adapter.notifyDataSetChanged()
        }

        viewModel.erro.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }
    }
}