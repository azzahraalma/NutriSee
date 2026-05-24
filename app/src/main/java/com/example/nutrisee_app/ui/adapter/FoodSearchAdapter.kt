package com.example.nutrisee.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.nutrisee.data.model.FoodSearchItem

class FoodSearchAdapter(
    context: Context,
    private val items: List<FoodSearchItem>
) : ArrayAdapter<FoodSearchItem>(context, 0, items) {

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {

        val view = convertView
            ?: LayoutInflater.from(context)
                .inflate(
                    android.R.layout.simple_list_item_2,
                    parent,
                    false
                )

        val item = items[position]

        val text1 = view.findViewById<TextView>(android.R.id.text1)
        val text2 = view.findViewById<TextView>(android.R.id.text2)

        text1.text = item.title
        text1.setTextColor(0xFF1A1A1A.toInt())
        text1.textSize = 14f

        text2.text = "Tap untuk lihat nutrisi"
        text2.setTextColor(0xFF9E9E9E.toInt())
        text2.textSize = 12f

        view.setPadding(32, 16, 32, 16)

        return view
    }
}