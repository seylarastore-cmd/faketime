package com.mcai.faketime.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mcai.faketime.databinding.ItemAppBinding

data class AppEntry(
    val packageName: String,
    val label: String,
    val forceRealTime: Boolean,
)

class AppsAdapter(
    private val context: Context,
    private val onToggle: (packageName: String, real: Boolean) -> Unit,
) : RecyclerView.Adapter<AppsAdapter.VH>() {

    private val items = mutableListOf<AppEntry>()

    inner class VH(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.textAppName.text = item.label
        holder.binding.textAppPackage.text = item.packageName
        holder.binding.switchRealTime.isChecked = item.forceRealTime
        holder.binding.switchRealTime.setOnCheckedChangeListener(null)
        holder.binding.switchRealTime.setOnCheckedChangeListener { _, checked ->
            onToggle(item.packageName, checked)
        }
    }

    fun submit(newItems: List<AppEntry>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
