// MainActivity.kt
package com.unasp.poupae.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.unasp.poupae.R
import com.unasp.poupae.dialog.AddTransactionDialog
import com.unasp.poupae.view.fragments.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.menu_add) {
                AddTransactionDialog().show(supportFragmentManager, "addDialog")
                return@setOnItemSelectedListener true
            }

            val fragment: Fragment = when (item.itemId) {
                R.id.menu_home -> HomeFragment()
                R.id.menu_extrato -> ExtratoFragment()
                R.id.menu_metas -> MetasFragment()
                R.id.menu_orcamento -> OrcamentoFragment()
                else -> HomeFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()

            true
        }

        bottomNav.selectedItemId = R.id.menu_home // default
    }
}