package com.unasp.poupae.view

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.unasp.poupae.R
import com.unasp.poupae.repository.AuthRepository
import com.unasp.poupae.viewmodel.RegisterViewModel
import com.unasp.poupae.viewmodel.RegisterViewModelFactory

class RegisterActivity : AppCompatActivity() {

    private lateinit var viewModel: RegisterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // ViewModel com factory
        val factory = RegisterViewModelFactory(AuthRepository())
        viewModel = ViewModelProvider(this, factory)[RegisterViewModel::class.java]

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val senha = passwordEditText.text.toString()

            if (email.isNotEmpty() && senha.length >= 6) {
                viewModel.registrarUsuario(email, senha)
            } else {
                Toast.makeText(this, getString(R.string.erro_email_senha), Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.estadoRegistro.observe(this) { resultado ->
            resultado.onSuccess {
                Toast.makeText(this, getString(R.string.conta_criada_sucesso), Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }.onFailure { e ->
                Toast.makeText(this, getString(R.string.erro_criar_conta, e.message ?: ""), Toast.LENGTH_LONG).show()
            }
        }
    }
}