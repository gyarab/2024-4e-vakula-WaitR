package com.example.waitr

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView

class Food_menu : Fragment() {
    // globalni promenne
    private var CompanyID: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            CompanyID = it.getString(CompanyID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_food_menu, container, false)
        // Inflate the layout for this fragment
        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val editButton = view.findViewById<ImageButton>(R.id.edit_menu_button)
        editButton.setOnClickListener {
            showEditMenuPopup()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(companyId: String) =
            Food_menu().apply {
                arguments = Bundle().apply {
                    putString(CompanyID, companyId)
                }
            }
    }

    private fun showEditMenuPopup(){
// Vytvoření dialogu
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.edit_food_menu_popup)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.95).toInt()
        )
        // Reference na prvky v popup layoutu
        val saveButton = dialog.findViewById<TextView>(R.id.save_menu_edit)
        val cancelButton = dialog.findViewById<TextView>(R.id.cancel_menu_edit)
        val addItemButton = dialog.findViewById<TextView>(R.id.add_menu_item)
        val addGroupButton = dialog.findViewById<TextView>(R.id.add_menu_group)
        saveButton.setOnClickListener {

        }
        cancelButton.setOnClickListener {

        }
        addItemButton.setOnClickListener {

        }
        addGroupButton.setOnClickListener {

        }
    }

}