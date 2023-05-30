package com.example.iss.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.iss.R
import com.example.iss.databinding.ItemPositionLogBinding
import com.example.iss.db.entity.Position
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class PositionLogAdapter(
    private val timeZone: ZoneId
) : RecyclerView.Adapter<PositionLogAdapter.ViewHolder>() {
    val items = mutableListOf<Position>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_position_log, parent, false)
        )

    override fun getItemCount(): Int =
        items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item: Position = items[position]
        val instant = Instant.ofEpochSecond(item.time)
        val localTime = ZonedDateTime.ofInstant(instant, timeZone).toLocalDateTime()
        holder.binding.logTimestamp.text = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localTime)
        holder.binding.logPosition.text =holder.itemView.context.getString(R.string.coordinates, item.latitude, item.longitude)
    }

    fun update(items: List<Position>) {
        val formerSize = this.items.size
        this.items.run {
            clear()
            addAll(items)
        }
        notifyItemRangeChanged(0, minOf(formerSize, items.size))
        notifyItemRangeInserted(formerSize, maxOf(0, items.size - formerSize))
        notifyItemRangeRemoved(items.size, maxOf(0, formerSize - items.size))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ItemPositionLogBinding.bind(itemView)
    }
}
