package com.example.agenciaviajes.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.agenciaviajes.databinding.ItemDestinoBinding
import com.example.agenciaviajes.model.Destino
import java.io.File

class DestinoAdapter(
    private var destinos: List<Destino>,
    private val onEditar: (Destino) -> Unit,
    private val onEliminar: (Destino) -> Unit
) : RecyclerView.Adapter<DestinoAdapter.DestinoViewHolder>() {

    inner class DestinoViewHolder(val binding: ItemDestinoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DestinoViewHolder {
        val binding = ItemDestinoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DestinoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DestinoViewHolder, position: Int) {
        val destino = destinos[position]
        with(holder.binding) {
            tvNombre.text = destino.nombre
            tvPais.text = destino.pais
            tvPrecio.text = "$${String.format("%.2f", destino.precio)}"
            tvDescripcion.text = destino.descripcion

            // Cargamos la imagen desde el almacenamiento local con Glide
            if (destino.imagenPath.isNotEmpty()) {
                Glide.with(ivDestino.context)
                    .load(File(destino.imagenPath))
                    .centerCrop()
                    .into(ivDestino)
            }

            btnEditar.setOnClickListener { onEditar(destino) }
            btnEliminar.setOnClickListener { onEliminar(destino) }
        }
    }

    override fun getItemCount(): Int = destinos.size

    fun actualizarLista(nuevaLista: List<Destino>) {
        destinos = nuevaLista
        notifyDataSetChanged()
    }
}