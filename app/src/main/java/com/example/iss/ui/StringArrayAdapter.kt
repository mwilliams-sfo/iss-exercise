package com.example.iss.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StringArrayAdapter() : RecyclerView.Adapter<StringArrayAdapter.ViewHolder>() {
    private val items = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
        )

    override fun getItemCount(): Int =
        items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val textView = holder.itemView.findViewById<TextView>(android.R.id.text1)
        textView.text = items[position]
    }

    fun update(items: List<String>) =
        this.items.run {
            val formerSize = size
            clear()
            addAll(items)
            notifyItemRangeRemoved(0, formerSize)
            notifyItemRangeInserted(0, size)
        }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
