package com.example.waitr

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MenuAdapter(
    private var items: List<MenuItem>,
    private val onAddClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuAdapter.ViewHolder>() {

    // ViewHolder třída, která drží reference na view komponenty pro každou položku v seznamu
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.textViewName)
        val buttonAdd: Button = itemView.findViewById(R.id.buttonAdd)
    }

    // Metoda pro vytvoření nového ViewHolderu, když RecyclerView potřebuje nový
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu, parent, false)
        return ViewHolder(view)
    }

    // Metoda pro přiřazení dat k view komponentám v ViewHolderu na konkrétní pozici
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = "${item.name} - ${item.price} Kč"

        // Kliknutí na tlačítko "Add"
        holder.buttonAdd.setOnClickListener {
            onAddClick(item)
        }
    }

    override fun getItemCount() = items.size

    // Metoda pro aktualizaci dat v adapteru a upozornění RecyclerView na změnu dat
    fun updateData(newItems: List<MenuItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}