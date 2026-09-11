package com.example.agenciaviajes.ui.theme

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.agenciaviajes.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnRegister.setOnClickListener {
            intentarRegistro()
        }

        binding.btnGoLogin.setOnClickListener {
            finish() // regresa al login
        }
    }

    private fun intentarRegistro() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        binding.tvError.visibility = android.view.View.GONE

        if (email.isEmpty() || password.isEmpty()) {
            mostrarError("Todos los campos son obligatorios")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mostrarError("Correo inválido")
            return
        }
        if (password.length < 6) {
            mostrarError("La contraseña debe tener al menos 6 caracteres")
            return
        }

        mostrarCargando(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                mostrarCargando(false)
                if (task.isSuccessful) {
                    startActivity(Intent(this, CatalogoActivity::class.java))
                    finish()
                } else {
                    mostrarError(task.exception?.localizedMessage ?: "Error al registrarse")
                }
            }
    }

    private fun mostrarError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = android.view.View.VISIBLE
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.progressBar.visibility = if (cargando) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnRegister.isEnabled = !cargando
    }
}