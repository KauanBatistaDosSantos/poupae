package com.unasp.poupae.view

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.unasp.poupae.R
import androidx.lifecycle.ViewModelProvider
import com.unasp.poupae.repository.AuthRepository
import com.unasp.poupae.viewmodel.LoginViewModelFactory
import com.unasp.poupae.viewmodel.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: LoginViewModel
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Forma moderna de capturar o resultado do login do Google
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.result
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            goToMain()
                        } else {
                            showToast(getString(R.string.erro_google_login))
                        }
                    }
            } catch (e: Exception) {
                showToast(getString(R.string.erro_generico, e.message ?: ""))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = LoginViewModelFactory(AuthRepository())
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        // ✅ Verifica se o usuário já está logado
        if (FirebaseAuth.getInstance().currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Configuração do login com Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // deve estar no strings.xml
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Referências de UI
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val googleLoginButton = findViewById<Button>(R.id.googleLoginButton)
        val registerText = findViewById<TextView>(R.id.registerText)

        // Login com Email e Senha
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.fazerLogin(email, password)
            } else {
                showToast(getString(R.string.preencha_campos))
            }
        }

        viewModel.estadoLogin.observe(this) { resultado ->
            resultado.onSuccess {
                goToMain()
            }.onFailure { e ->
                showToast(getString(R.string.erro_login_email, e.message ?: ""))
            }
        }


        // Login com Google
        googleLoginButton.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        // Ir para a tela de registro
        registerText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}