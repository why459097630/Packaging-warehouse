package com.ndjc.generated

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ModelsAdapter(
    private val data: MutableList<Model>,
    private val listener: Listener
) : RecyclerView.Adapter<ModelsAdapter.VH>() {

    interface Listener {
        fun onItemClick(position: Int)
        fun onItemLongClick(position: Int)
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.title)
        val meta: TextView = v.findViewById(R.id.meta)
        val summary: TextView = v.findViewById(R.id.summary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_model, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val m = data[position]
        h.title.text = m.name ?: "(未命名)"
        val meta = buildString {
            m.years?.let { append("Years: ").append(it).append(' ') }
            m.engine?.let { append("Engine: ").append(it).append(' ') }
            m.decade?.let { append("Decade: ").append(it) }
        }.trim()
        h.meta.text = meta
        h.summary.text = m.summary ?: ""
        h.itemView.setOnClickListener { listener.onItemClick(h.bindingAdapterPosition) }
        h.itemView.setOnLongClickListener { listener.onItemLongClick(h.bindingAdapterPosition); true }
    }

    override fun getItemCount(): Int = data.size
}
