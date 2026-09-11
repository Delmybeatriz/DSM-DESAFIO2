package com.example.agenciaviajes.ui.theme

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.agenciaviajes.R
import com.example.agenciaviajes.adapter.DestinoAdapter
import com.example.agenciaviajes.databinding.ActivityCatalogoBinding
import com.example.agenciaviajes.model.Destino
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class CatalogoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCatalogoBinding
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: DestinoAdapter
    private var listenerRegistration: ListenerRegistration? = null

    companion object {
        const val COLLECTION_DESTINOS = "destinos"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCatalogoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.toolbar.inflateMenu(R.menu.menu_catalogo)
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_logout) {
                cerrarSesion()
                true
            } else false
        }

        setupRecyclerView()

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, DestinoFormActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = DestinoAdapter(
            destinos = emptyList(),
            onEditar = { destino ->
                val intent = Intent(this, DestinoFormActivity::class.java)
                intent.putExtra("destino_id", destino.id)
                startActivity(intent)
            },
            onEliminar = { destino -> confirmarEliminar(destino) }
        )
        binding.rvDestinos.layoutManager = LinearLayoutManager(this)
        binding.rvDestinos.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        escucharDestinos()
    }

    override fun onStop() {
        super.onStop()
        listenerRegistration?.remove()
    }

    private fun escucharDestinos() {
        binding.progressBar.visibility = View.VISIBLE
        listenerRegistration = db.collection(COLLECTION_DESTINOS)
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = View.GONE
                if (error != null || snapshot == null) {
                    return@addSnapshotListener
                }
                val lista = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Destino::class.java)?.apply { id = doc.id }
                }
                adapter.actualizarLista(lista)
                binding.tvEmpty.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun confirmarEliminar(destino: Destino) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.msg_confirm_delete))
            .setPositiveButton(getString(R.string.btn_confirm)) { _, _ ->
                db.collection(COLLECTION_DESTINOS).document(destino.id)
                    .delete()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun cerrarSesion() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}