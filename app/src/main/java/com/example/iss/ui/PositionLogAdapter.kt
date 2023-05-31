package com.example.iss.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.iss.R
import com.example.iss.databinding.ItemPositionLogBinding.bind
import com.example.iss.db.entity.Position
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

class PositionLogAdapter(
    private val timeZone: ZoneId
) : RecyclerView.Adapter<PositionLogAdapter.ViewHolder>() {
    private val differ = AsyncListDiffer(this, object: DiffUtil.ItemCallback<Position>() {
        override fun areItemsTheSame(oldItem: Position, newItem: Position): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Position, newItem: Position): Boolean =
            oldItem == newItem
    })

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_position_log, parent, false)
        )

    override fun getItemCount(): Int =
        differ.currentList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = differ.currentList[position]
        val instant = Instant.ofEpochSecond(item.time)
        val localTime = ZonedDateTime.ofInstant(instant, timeZone).toLocalDateTime()
        holder.binding.logTimestamp.text = ISO_LOCAL_DATE_TIME.format(localTime)
        holder.binding.logPosition.text = holder.itemView.context.getString(R.string.coordinates, item.latitude, item.longitude)
    }

    fun update(items: List<Position>) {
        differ.submitList(items)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = bind(itemView)
    }
}
