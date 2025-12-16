package com.example.kuriermapa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView

class MiejscaAdapter(
    private val miejsca: MutableList<Miejsce>,
    private val onCheckChanged: (Miejsce, Boolean) -> Unit,
    private val onDeliveredClicked: (Miejsce) -> Unit
) : RecyclerView.Adapter<MiejscaAdapter.MiejsceViewHolder>() {

    private val selectedStates = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiejsceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_miejsce, parent, false)
        return MiejsceViewHolder(view)
    }

    override fun onBindViewHolder(holder: MiejsceViewHolder, position: Int) {
        val miejsce = miejsca[position]

        holder.checkBox.text = miejsce.nazwa
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = selectedStates.contains(miejsce.nazwa)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedStates.add(miejsce.nazwa)
            else selectedStates.remove(miejsce.nazwa)
            onCheckChanged(miejsce, isChecked)
        }

        holder.btnDelivered.setOnClickListener {
            onDeliveredClicked(miejsce)
        }
    }

    override fun getItemCount(): Int = miejsca.size

    class MiejsceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        val btnDelivered: Button = itemView.findViewById(R.id.btnDelivered)
    }

    fun getSelectedPlaces(): List<Miejsce> = miejsca.filter { selectedStates.contains(it.nazwa) }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        val temp = miejsca[fromPosition]
        miejsca.removeAt(fromPosition)
        miejsca.add(toPosition, temp)
        notifyItemMoved(fromPosition, toPosition)
    }
    fun removeItem(miejsce: Miejsce) {
        val index = miejsca.indexOf(miejsce)
        if (index != -1) {
            miejsca.removeAt(index)
            selectedStates.remove(miejsce.nazwa)
            notifyItemRemoved(index)
        }
    }

}
