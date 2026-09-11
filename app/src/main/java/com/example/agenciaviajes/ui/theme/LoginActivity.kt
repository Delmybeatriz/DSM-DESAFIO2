package com.example.agenciaviajes.ui.theme

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.agenciaviajes.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnLogin.setOnClickListener {
            intentarLogin()
        }

        binding.btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        // Si ya hay una sesión activa, saltar directo al catálogo
        if (auth.currentUser != null) {
            irACatalogo()
        }
    }

    private fun intentarLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        binding.tvError.visibility = android.view.View.GONE

        if (email.isEmpty() || password.isEmpty()) {
            mostrarError(getString(com.example.agenciaviajes.R.string.error_campo_vacio))
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mostrarError("Correo inválido")
            return
        }

        mostrarCargando(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                mostrarCargando(false)
                if (task.isSuccessful) {
                    irACatalogo()
                } else {
                    mostrarError(task.exception?.localizedMessage ?: "Error al iniciar sesión")
                }
            }
    }

    private fun irACatalogo() {
        startActivity(Intent(this, CatalogoActivity::class.java))
        finish()
    }

    private fun mostrarError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = android.view.View.VISIBLE
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.progressBar.visibility = if (cargando) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnLogin.isEnabled = !cargando
    }
}