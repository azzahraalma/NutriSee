package com.example.nutrisee_app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.nutrisee.R

class WeekDateAdapter(
    private val days: List<Pair<String, String>>,
    private var selectedDate: String,
    private val onDateSelected: (String) -> Unit
) : RecyclerView.Adapter<WeekDateAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val txtDay: TextView = view.findViewById(R.id.txtDay)    // ✅ sesuai layout
        val txtNum: TextView = view.findViewById(R.id.txtDate)   // ✅ sesuai layout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_date_chip, parent, false)
        return VH(view)
    }

    override fun getItemCount() = days.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (label, tanggal) = days[position]
        val parts = label.split("\n")
        holder.txtDay.text = parts.getOrElse(0) { "" }
        holder.txtNum.text = parts.getOrElse(1) { "" }

        val isSelected = tanggal == selectedDate

        holder.itemView.setBackgroundResource(
            if (isSelected) R.drawable.bg_chip_selected
            else R.drawable.bg_chip_unselected
        )

        val textColor = if (isSelected)
            ContextCompat.getColor(holder.itemView.context, android.R.color.white)
        else
            ContextCompat.getColor(holder.itemView.context, R.color.green_primary)

        holder.txtDay.setTextColor(textColor)
        holder.txtNum.setTextColor(textColor)

        holder.itemView.setOnClickListener {
            val old = days.indexOfFirst { it.second == selectedDate }
            selectedDate = tanggal
            notifyItemChanged(old)
            notifyItemChanged(position)
            onDateSelected(tanggal)
        }
    }
}