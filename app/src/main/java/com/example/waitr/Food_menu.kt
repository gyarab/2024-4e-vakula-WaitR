package com.example.waitr

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText

class Food_menu : Fragment() {
    // globalni promenne
    private var CompanyID: String? = null
    private lateinit var menuLayout: LinearLayout
    private lateinit var editMenuDialoge: Dialog
    private lateinit var editMenuLayout: LinearLayout

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
        menuLayout = view.findViewById(R.id.menu_layout)
        editMenuDialoge = Dialog(requireContext())
        editMenuDialoge.setContentView(R.layout.edit_food_menu_popup)
        editMenuLayout = editMenuDialoge.findViewById(R.id.edit_menu_layout)
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
        // Nastavení velikosti dialogu
        editMenuDialoge.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.95).toInt()
        )
        // Reference na prvky v popup layoutu
        val saveButton = editMenuDialoge.findViewById<TextView>(R.id.save_menu_edit)
        val cancelButton = editMenuDialoge.findViewById<TextView>(R.id.cancel_menu_edit)
        val addItemButton = editMenuDialoge.findViewById<TextView>(R.id.add_menu_item)
        val addGroupButton = editMenuDialoge.findViewById<TextView>(R.id.add_menu_group)
        saveButton.setOnClickListener {

        }
        cancelButton.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Cancel changes")
            builder.setMessage("Are you sure you want to cancel all the changes?")

            builder.setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                editMenuDialoge.dismiss()
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }

            val alertDialog = builder.create()
            alertDialog.show()
        }
        addItemButton.setOnClickListener {
            addItemPopup()
        }
        addGroupButton.setOnClickListener {
            addGroupPopup()
        }
        editMenuDialoge.show()
    }
    private fun addItemPopup(){
        // Vytvoření dialogu
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.add_item_to_menu_popup)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.7).toInt(),
            (resources.displayMetrics.heightPixels * 0.6).toInt()
        )
        // Reference na prvky v popup layoutu
        val itemNameInput = dialog.findViewById<TextInputEditText>(R.id.item_name)
        val itemPriceInput = dialog.findViewById<TextInputEditText>(R.id.item_price)
        val itemDiscriptionInput = dialog.findViewById<TextInputEditText>(R.id.item_discription)
        val addButton = dialog.findViewById<Button>(R.id.add_item_button)

        addButton.setOnClickListener {
            val itemName = itemNameInput.text.toString().trim()
            val itemDiscription = itemDiscriptionInput.text.toString().trim()
            val itemPriceText = itemPriceInput.text.toString().trim()

            if (itemName.isEmpty() || itemDiscription.isEmpty() || itemPriceText.isEmpty()) {
                Toast.makeText(dialog.context, "All fields must be filled!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val itemPrice = try {
                itemPriceText.toDouble()
            } catch (e: NumberFormatException) {
                Toast.makeText(dialog.context, "Zadejte platnou cenu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newMenuItem = MenuItem(itemName, itemPrice, itemDiscription)
            //TODO dodelat dynamicke UI update

            dialog.dismiss()
        }
        dialog.show()
    }
    private fun addGroupPopup(){
        // Vytvoření dialogu
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.add_group_to_menu_popup)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.7).toInt(),
            (resources.displayMetrics.heightPixels * 0.6).toInt()
        )
        val groupNameInput = dialog.findViewById<TextInputEditText>(R.id.group_name)
        val addButton = dialog.findViewById<Button>(R.id.add_group_button)

        addButton.setOnClickListener {
            val groupName = groupNameInput.text.toString().trim()
            if (groupName.isEmpty()){
                Toast.makeText(dialog.context, "You must enter the name first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val menuGroup = MenuGroup(groupName, null, null)
            //TODO dodelat dynamicke UI update
            dialog.dismiss()
        }
        dialog.show()
    }

}