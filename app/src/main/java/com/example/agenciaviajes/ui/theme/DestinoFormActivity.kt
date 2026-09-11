package com.example.agenciaviajes.ui.theme

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.agenciaviajes.R
import com.example.agenciaviajes.databinding.ActivityDestinoFormBinding
import com.example.agenciaviajes.model.Destino
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class DestinoFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDestinoFormBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var destinoId: String? = null       // null = creando nuevo, no null = editando
    private var imagenPathActual: String = ""    // ruta local ya guardada (modo edición)
    private var imagenUriSeleccionada: Uri? = null // nueva imagen elegida de la galería

    // Registrador para abrir la galería y recibir el resultado
    private val seleccionarImagenLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imagenUriSeleccionada = uri
            binding.tvErrorImagen.visibility = View.GONE
            Glide.with(this).load(uri).centerCrop().into(binding.ivPreview)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDestinoFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinner()

        binding.btnSelectImage.setOnClickListener {
            seleccionarImagenLauncher.launch("image/*")
        }

        destinoId = intent.getStringExtra("destino_id")
        if (destinoId != null) {
            binding.tvTitleForm.text = getString(R.string.title_edit_destino)
            binding.btnEliminar.visibility = View.VISIBLE
            cargarDestino(destinoId!!)
        }

        binding.btnGuardar.setOnClickListener { validarYGuardar() }
        binding.btnEliminar.setOnClickListener { eliminarDestino() }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.lista_paises,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPais.adapter = adapter
    }

    private fun cargarDestino(id: String) {
        mostrarCargando(true)
        db.collection(CatalogoActivity.COLLECTION_DESTINOS).document(id).get()
            .addOnSuccessListener { doc ->
                mostrarCargando(false)
                val destino = doc.toObject(Destino::class.java) ?: return@addOnSuccessListener
                binding.etNombre.setText(destino.nombre)
                binding.etPrecio.setText(destino.precio.toString())
                binding.etDescripcion.setText(destino.descripcion)
                imagenPathActual = destino.imagenPath

                // Seleccionar el país correcto en el Spinner
                val paises = resources.getStringArray(R.array.lista_paises)
                val index = paises.indexOf(destino.pais)
                if (index >= 0) binding.spinnerPais.setSelection(index)

                if (imagenPathActual.isNotEmpty()) {
                    Glide.with(this).load(File(imagenPathActual)).centerCrop().into(binding.ivPreview)
                }
            }
            .addOnFailureListener {
                mostrarCargando(false)
                mostrarErrorGeneral("No se pudo cargar el destino")
            }
    }

    private fun validarYGuardar() {
        binding.tvErrorGeneral.visibility = View.GONE
        binding.tvErrorImagen.visibility = View.GONE

        val nombre = binding.etNombre.text.toString().trim()
        val pais = binding.spinnerPais.selectedItem?.toString() ?: ""
        val precioTexto = binding.etPrecio.text.toString().trim()
        val descripcion = binding.etDescripcion.text.toString().trim()

        if (nombre.isEmpty() || precioTexto.isEmpty() || descripcion.isEmpty()) {
            mostrarErrorGeneral(getString(R.string.error_campo_vacio))
            return
        }

        val precio = precioTexto.toDoubleOrNull()
        if (precio == null || precio <= 0.0) {
            mostrarErrorGeneral(getString(R.string.error_precio_invalido))
            return
        }

        if (descripcion.length < 20) {
            mostrarErrorGeneral(getString(R.string.error_descripcion_corta))
            return
        }

        // La imagen es obligatoria: o ya existe una (modo edición) o se seleccionó una nueva
        if (imagenUriSeleccionada == null && imagenPathActual.isEmpty()) {
            binding.tvErrorImagen.visibility = View.VISIBLE
            return
        }

        mostrarCargando(true)

        // Si el usuario seleccionó una imagen nueva, la copiamos a almacenamiento interno
        val rutaFinalImagen = if (imagenUriSeleccionada != null) {
            guardarImagenLocal(imagenUriSeleccionada!!)
        } else {
            imagenPathActual
        }

        if (rutaFinalImagen == null) {
            mostrarCargando(false)
            mostrarErrorGeneral("No se pudo guardar la imagen")
            return
        }

        val destino = Destino(
            id = destinoId ?: "",
            nombre = nombre,
            pais = pais,
            precio = precio,
            descripcion = descripcion,
            imagenPath = rutaFinalImagen,
            userId = auth.currentUser?.uid ?: ""
        )

        guardarEnFirestore(destino)
    }

    // Copia la imagen elegida de la galería hacia una carpeta privada de la app
    private fun guardarImagenLocal(uri: Uri): String? {
        return try {
            val nombreArchivo = "destino_${UUID.randomUUID()}.jpg"
            val archivoDestino = File(filesDir, nombreArchivo)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(archivoDestino).use { output ->
                    input.copyTo(output)
                }
            }
            archivoDestino.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun guardarEnFirestore(destino: Destino) {
        val coleccion = db.collection(CatalogoActivity.COLLECTION_DESTINOS)

        val tarea = if (destinoId != null) {
            coleccion.document(destinoId!!).set(destino)
        } else {
            val nuevoDoc = coleccion.document()
            destino.id = nuevoDoc.id
            nuevoDoc.set(destino)
        }

        tarea.addOnSuccessListener {
            mostrarCargando(false)
            finish()
        }.addOnFailureListener {
            mostrarCargando(false)
            mostrarErrorGeneral("Error al guardar: ${it.localizedMessage}")
        }
    }

    private fun eliminarDestino() {
        val id = destinoId ?: return
        mostrarCargando(true)
        db.collection(CatalogoActivity.COLLECTION_DESTINOS).document(id)
            .delete()
            .addOnSuccessListener {
                mostrarCargando(false)
                finish()
            }
            .addOnFailureListener {
                mostrarCargando(false)
                mostrarErrorGeneral("Error al eliminar")
            }
    }

    private fun mostrarErrorGeneral(msg: String) {
        binding.tvErrorGeneral.text = msg
        binding.tvErrorGeneral.visibility = View.VISIBLE
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.progressBar.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnGuardar.isEnabled = !cargando
    }
}