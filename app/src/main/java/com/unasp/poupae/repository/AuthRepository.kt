package com.unasp.poupae.repository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    suspend fun loginEmailSenha(email: String, senha: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, senha).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun criarConta(email: String, senha: String): Result<Unit> {
        return try {
            auth.createUserWithEmailAndPassword(email, senha).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}