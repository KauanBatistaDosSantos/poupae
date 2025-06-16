// MainActivity.kt
package com.unasp.poupae.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.unasp.poupae.R
import android.content.Intent
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import androidx.appcompat.widget.Toolbar
import com.unasp.poupae.dialog.AddTransactionDialog
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import com.unasp.poupae.view.fragments.*

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup Drawer
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        toggle.drawerArrowDrawable.color = ContextCompat.getColor(this, android.R.color.white)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_config -> {
                    val idiomas = arrayOf(
                        getString(R.string.idioma_portugues),
                        getString(R.string.idioma_ingles),
                        getString(R.string.idioma_espanhol)
                    )
                    val codigos = arrayOf("pt", "en", "es")

                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.escolha_idioma))
                        .setItems(idiomas) { _, which ->
                            val language = codigos[which]
                            setAppLocale(this, language)
                            recreate() // reinicia a activity para aplicar o idioma
                        }
                        .show()
                    true
                }
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        // Navegação inferior
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

        bottomNav.selectedItemId = R.id.menu_home
    }
}