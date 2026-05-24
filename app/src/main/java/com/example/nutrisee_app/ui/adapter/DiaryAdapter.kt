package com.example.nutrisee.ui.adapter
import com.example.nutrisee.data.model.DiaryItem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.nutrisee.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiaryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_MAKANAN   = 0
        private const val VIEW_TYPE_AKTIVITAS = 1
    }

    private val items = mutableListOf<DiaryItem>()

    fun submitList(newItems: List<DiaryItem>) {
        items.clear()
        items.addAll(newItems.sortedByDescending { it.waktuMs })
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is DiaryItem.Makanan   -> VIEW_TYPE_MAKANAN
        is DiaryItem.Aktivitas -> VIEW_TYPE_AKTIVITAS
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_MAKANAN -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_diary_makanan, parent, false)
                MakananViewHolder(v)
            }
            else -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_diary_aktivitas, parent, false)
                AktivitasViewHolder(v)
            }
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is DiaryItem.Makanan   -> (holder as MakananViewHolder).bind(item)
            is DiaryItem.Aktivitas -> (holder as AktivitasViewHolder).bind(item)
        }
    }

    inner class MakananViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val txtNama     = v.findViewById<TextView>(R.id.txtNamaMakanan)
        private val txtWaktu    = v.findViewById<TextView>(R.id.txtWaktuMakanan)
        private val txtKalori   = v.findViewById<TextView>(R.id.txtKaloriMakanan)
        private val txtKategori = v.findViewById<TextView>(R.id.txtKategoriMakanan)

        fun bind(item: DiaryItem.Makanan) {
            txtNama.text     = item.nama
            txtWaktu.text    = formatTime(item.waktuMs)
            txtKalori.text   = "+${item.kalori} kcal"
            txtKategori.text = item.kategori
        }
    }

    inner class AktivitasViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val txtNama   = v.findViewById<TextView>(R.id.txtNamaAktivitas)
        private val txtWaktu  = v.findViewById<TextView>(R.id.txtWaktuAktivitas)
        private val txtKalori = v.findViewById<TextView>(R.id.txtKaloriAktivitas)

        fun bind(item: DiaryItem.Aktivitas) {
            txtNama.text   = item.nama
            txtWaktu.text  = formatTime(item.waktuMs)
            txtKalori.text = "-${item.kaloriTerbakar} kcal"
        }
    }

    private fun formatTime(ms: Long): String {
        return SimpleDateFormat("HH.mm", Locale.getDefault()).format(Date(ms))
    }
}