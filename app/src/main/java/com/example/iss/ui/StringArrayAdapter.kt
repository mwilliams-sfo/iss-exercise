package com.example.iss.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StringArrayAdapter : RecyclerView.Adapter<StringArrayAdapter.ViewHolder>() {
    private var _items = mutableListOf<String>()
    var items: List<String>
        get() = _items
        set(value) = updateItems(value)

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

    private fun updateItems(items: List<String>) {
        val formerSize = _items.size
        _items.clear()
        notifyItemRangeRemoved(0, formerSize)
        _items.addAll(items)
        notifyItemRangeInserted(0, this.items.size)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
