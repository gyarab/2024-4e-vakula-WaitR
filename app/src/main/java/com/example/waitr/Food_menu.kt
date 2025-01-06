package com.example.waitr

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class Food_menu : Fragment() {
    // globalni promenne
    private var CompanyID: String? = null
    private val db = FirebaseDatabase.getInstance("https://waitr-dee9a-default-rtdb.europe-west1.firebasedatabase.app/").reference // Using Realtime Database reference
    private lateinit var menuLayout: LinearLayout
    private lateinit var editMenuDialoge: Dialog
    private lateinit var editMenuLayout: LinearLayout
    private var selectedGroupID: String? = null
    private var selectedItemID: String? = null
    private lateinit var menu: MenuGroup
    private lateinit var editMenu: MenuGroup
    private var newGroupname: String? = null
    private var newItemname: String? = null
    private var newItemPrice: Double? = null
    private var newItemDescription: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            CompanyID = it.getString(CompanyID)
        }
        // TODO nacte z db menu a updatne ui
        menu = MenuGroup("menuId", "menu", null, null)
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
        editMenu = menu
        updateMenu()

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
            menu = editMenu
            updateMenu()
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
                Toast.makeText(dialog.context, "Add valid price!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val randomID = UUID.randomUUID().toString()
            val newMenuItem = MenuItem(randomID, itemName, itemPrice, itemDiscription)

            val itemView = TextView(context).apply {
                text = "$itemName - $itemPrice Kč"
                textSize = 25f
                setPadding(16, 16, 16, 16)
                tag = randomID
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    // Nastavení margin: levý, horní, pravý, dolní
                    setMargins(32, 0, 0, 8)
                }
            }
            itemView.setOnClickListener { view ->
                CustomClickListener(
                    onClick = {

                    },
                    onDoubleClick = {
                        selectedItemID = itemView.tag.toString()
                        itemOptionPopup(selectedItemID!!)
                    }
                ).onClick(view)
            }
            if (selectedGroupID == null){
                editMenu.items?.add(newMenuItem)
                editMenuLayout.addView(itemView)
            } else {
                val menuGroupToUpdate = findGroupById(editMenu, selectedGroupID!!)
                if (menuGroupToUpdate != null) {
                    menuGroupToUpdate.items?.add(newMenuItem)
                }
                val headerforLayout = findViewWithTagRecursively(editMenuLayout, selectedGroupID!!)
                val layoutToUpdate = headerforLayout?.parent
                if (layoutToUpdate is LinearLayout) {
                    layoutToUpdate.addView(itemView)
                }
            }

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
            } else {
                val randomID = UUID.randomUUID().toString()
                val menuGroup = MenuGroup(randomID, groupName, null, null)

                val groupHeader = TextView(context).apply {
                    text = "$groupName:"
                    textSize = 25f
                    setPadding(16, 16, 16, 16)
                    tag = randomID
                }
                groupHeader.setOnClickListener { view ->
                    CustomClickListener(
                        onClick = {
                            selectedGroupID = groupHeader.tag.toString()
                            Log.e("testik", selectedGroupID!!)
                            //TODO set visibility
                        },
                        onDoubleClick = {
                            selectedGroupID = groupHeader.tag.toString()
                            groupOptionsPopup(selectedGroupID!!)
                        }
                    ).onClick(view)
                }
                val menuGroupLayout = LinearLayout(context).apply {
                    setBackgroundColor(android.graphics.Color.LTGRAY)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(32, 0, 0, 0)
                    }
                    orientation = LinearLayout.VERTICAL
                }
                menuGroupLayout.addView(groupHeader)

                if (selectedGroupID == null){
                    editMenu.subGroups?.add(menuGroup)
                    editMenuLayout.addView(menuGroupLayout)
                }else {
                    val menuGroupToUpdate = findGroupById(editMenu, selectedGroupID!!)
                    if (menuGroupToUpdate != null) {
                        menuGroupToUpdate.subGroups?.add(menuGroup)
                    }
                    val headerforLayout = findViewWithTagRecursively(editMenuLayout, selectedGroupID!!)
                    val layoutToUpdate = headerforLayout?.parent
                    if (layoutToUpdate is LinearLayout) {
                        layoutToUpdate.addView(menuGroupLayout)
                    }
                }

                selectedGroupID = null
                dialog.dismiss()
            }
        }
        dialog.show()
    }
    private fun findViewWithTagRecursively(root: ViewGroup, tag: String): View? {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)

            if (child.tag == tag) {
                return child
            }

            if (child is ViewGroup) {
                val foundView = findViewWithTagRecursively(child, tag)
                if (foundView != null) {
                    return foundView
                }
            }
        }
        return null
    }
    private fun itemOptionPopup(itemId: String){
        val item = findItemById(editMenu, itemId)
        val itemName = item?.name
        val itemPrice = item?.price
        val itemDescription = item?.description
        // Vytvoření dialogu
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.item_option_popup)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.7).toInt(),
            (resources.displayMetrics.heightPixels * 0.6).toInt()
        )
        // Reference na prvky v popup layoutu
        val nameOfTheItem = dialog.findViewById<TextView>(R.id.name_of_the_item)
        nameOfTheItem.text = itemName
        val priceOfTheItem = dialog.findViewById<TextView>(R.id.price_of_the_item)
        priceOfTheItem.text = itemPrice.toString()
        val descriptionOfTheItem = dialog.findViewById<TextView>(R.id.discription_of_the_item)
        descriptionOfTheItem.text = itemDescription
        val saveButton = dialog.findViewById<Button>(R.id.save_item_options_button)
        saveButton.setOnClickListener {
            if (newItemname == null && itemName != null){
                item.name = itemName
            }
            if (newItemname != null){
                item?.name = newItemname.toString()
            }
            if (newItemPrice == null && itemPrice != null){
                item.price = itemPrice
            }
            if (newItemPrice != null){
                item?.price = newItemPrice!!.toDouble()
            }
            if (newItemDescription == null && itemDescription != null){
                item.description = itemDescription
            }
            if (newItemDescription != null){
                item?.description = newItemDescription.toString()
            }
            newItemname = null
            newItemPrice = null
            newItemDescription = null
            dialog.dismiss()
        }
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_item_options_button)
        cancelButton.setOnClickListener {
            newItemname = null
            newItemPrice = null
            newItemDescription = null
            dialog.dismiss()
        }
        val deleteButton = dialog.findViewById<Button>(R.id.delete_item_options_button)
        deleteButton.setOnClickListener {
            newItemname = null
            newItemPrice = null
            newItemDescription = null
            editMenu.deleteItem(itemId)

            val viewToRemove = findViewWithTagRecursively(editMenuLayout, selectedItemID!!)
            val layout = viewToRemove?.parent
            if (layout is LinearLayout){
                layout.removeView(viewToRemove)
            }
            dialog.dismiss()
        }
        val changeNameButton = dialog.findViewById<Button>(R.id.change_item_name)
        changeNameButton.setOnClickListener {
            changeItemName()
        }
        val changePriceButton = dialog.findViewById<Button>(R.id.change_item_price)
        changePriceButton.setOnClickListener {
            changeItemPrice()
        }
        val changeDescriptionButton = dialog.findViewById<Button>(R.id.change_item_discription)
        changeDescriptionButton.setOnClickListener {
            changeItemDescription()
        }
        dialog.show()
    }
    private fun changeItemName(){
        // Vytvoření dialogu
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.change_parameters_for_menu_elements)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.5).toInt(),
            (resources.displayMetrics.heightPixels * 0.4).toInt()
        )
        // Reference na prvky v popup layoutu
        val textView = dialog.findViewById<TextView>(R.id.parametr_view)
        textView.text = "Item name"
        val parametrToChange = dialog.findViewById<TextInputEditText>(R.id.parameter_to_change)
        parametrToChange.hint = "New item name"
        val changeTextButton = dialog.findViewById<Button>(R.id.change_group_name_button)
        changeTextButton.text = "Change name"
        changeTextButton.setOnClickListener {
            val newName = parametrToChange.text.toString().trim()
            if (newName.isEmpty()){
                Toast.makeText(dialog.context, "You must enter the name first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            newItemname = newName
            dialog.dismiss()
        }
        dialog.show()
    }
    private fun changeItemPrice(){
        // Vytvoření dialogu
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.change_parameters_for_menu_elements)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.5).toInt(),
            (resources.displayMetrics.heightPixels * 0.4).toInt()
        )
        // Reference na prvky v popup layoutu
        val textView = dialog.findViewById<TextView>(R.id.parametr_view)
        textView.text = "Item price"
        val parametrToChange = dialog.findViewById<TextInputEditText>(R.id.parameter_to_change)
        parametrToChange.hint = "New item price"
        val changeTextButton = dialog.findViewById<Button>(R.id.change_group_name_button)
        changeTextButton.text = "Change price"
        changeTextButton.setOnClickListener {
            val newPrice = parametrToChange.text.toString().trim()
            if (newPrice.isEmpty()){
                Toast.makeText(dialog.context, "You must enter the price first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val itemPrice = try {
                newPrice.toDouble()
            } catch (e: NumberFormatException) {
                Toast.makeText(dialog.context, "Enter valid price!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            newItemPrice = itemPrice
            dialog.dismiss()
        }
        dialog.show()
    }
    private fun changeItemDescription(){
        // Vytvoření dialogu
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.change_parameters_for_menu_elements)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.5).toInt(),
            (resources.displayMetrics.heightPixels * 0.4).toInt()
        )
        // Reference na prvky v popup layoutu
        val textView = dialog.findViewById<TextView>(R.id.parametr_view)
        textView.text = "Item description"
        val parametrToChange = dialog.findViewById<TextInputEditText>(R.id.parameter_to_change)
        parametrToChange.hint = "New item description"
        val changeTextButton = dialog.findViewById<Button>(R.id.change_group_name_button)
        changeTextButton.text = "Change description"
        changeTextButton.setOnClickListener {
            val newDescription = parametrToChange.text.toString().trim()
            if (newDescription.isEmpty()){
                Toast.makeText(dialog.context, "You must enter the description first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            newItemDescription = newDescription
            dialog.dismiss()
        }
        dialog.show()
    }
    private fun groupOptionsPopup(groupId: String){
        //najde MenuGroup
        val menuGroup = findGroupById(editMenu, groupId)
        val menuGroupName = menuGroup?.name

        // Vytvoření dialogu
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.group_option_popup)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.7).toInt(),
            (resources.displayMetrics.heightPixels * 0.6).toInt()
        )
        // Reference na prvky v popup layoutu
        val nameOfTheGroup = dialog.findViewById<TextView>(R.id.name_of_the_group)
        nameOfTheGroup.text = menuGroupName
        val saveButton = dialog.findViewById<Button>(R.id.save_group_options_button)
        saveButton.setOnClickListener {
            if (newGroupname == null && menuGroupName != null){
                menuGroup.name = menuGroupName
            }
            if (newGroupname != null){
                menuGroup?.name = newGroupname.toString()
            }
            val headerToUpdate = findViewWithTagRecursively(editMenuLayout, selectedGroupID!!)
            if (headerToUpdate is TextView){
                headerToUpdate.text = newGroupname
            }
            newGroupname = null
            dialog.dismiss()
        }
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_group_options_button)
        cancelButton.setOnClickListener {
            newGroupname = null
            dialog.dismiss()
        }
        val deleteButton = dialog.findViewById<Button>(R.id.delete_group_options_button)
        deleteButton.setOnClickListener {
            newGroupname = null
            editMenu.deleteGroup(groupId)

            val headerforLayout = findViewWithTagRecursively(editMenuLayout, selectedGroupID!!)
            val layoutToRemove = headerforLayout?.parent
            if (layoutToRemove is LinearLayout) {
                val layout = layoutToRemove.parent
                if (layout is LinearLayout){
                    layout.removeView(layoutToRemove)
                }
            }
            dialog.dismiss()
        }
        val changeNameButton = dialog.findViewById<Button>(R.id.change_group_name)
        changeNameButton.setOnClickListener {
            changeGroupName()
        }
        dialog.show()
    }
    private fun changeGroupName(){
        // Vytvoření dialogu
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.change_parameters_for_menu_elements)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.5).toInt(),
            (resources.displayMetrics.heightPixels * 0.4).toInt()
        )
        // Reference na prvky v popup layoutu
        val textView = dialog.findViewById<TextView>(R.id.parametr_view)
        textView.text = "Group name"
        val parametrToChange = dialog.findViewById<TextInputEditText>(R.id.parameter_to_change)
        parametrToChange.hint = "New group name"
        val changeTextButton = dialog.findViewById<Button>(R.id.change_group_name_button)
        changeTextButton.text = "Change name"
        changeTextButton.setOnClickListener {
            val newName = parametrToChange.text.toString().trim()
            if (newName.isEmpty()){
                Toast.makeText(dialog.context, "You must enter the name first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            newGroupname = newName
            dialog.dismiss()
        }
        dialog.show()
    }
    private fun findItemById(group: MenuGroup, itemId: String): MenuItem? {
        // Hledání v aktuální skupině
        group.items?.forEach { item ->
            if (item.id == itemId) return item
        }
        // Rekurzivní hledání v podskupinách
        group.subGroups?.forEach { subGroup ->
            val found = findItemById(subGroup, itemId)
            if (found != null) return found
        }
        // Pokud nic nenalezeno
        return null
    }
    private fun findGroupById(group: MenuGroup, groupId: String): MenuGroup? {
        // Hledání v aktuální skupině
        if (group.id == groupId) return group

        // Rekurzivní hledání v podskupinách
        group.subGroups?.forEach { subGroup ->
            val found = findGroupById(subGroup, groupId)
            if (found != null) return found
        }

        // Pokud nic nenalezeno
        return null
    }
    private fun updateMenu(){
        val companyMenuRef = CompanyID?.let {
            db.child("companies").child(it).child("Menu")
        }
        companyMenuRef
            ?.updateChildren(editMenu.toMap())
            ?.addOnSuccessListener {
                Toast.makeText(
                    context,
                    "Changes saved!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            ?.addOnFailureListener {
                Toast.makeText(
                    context,
                    "Failed to save the changes!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        fetchMenu()
    }
    private fun fetchMenu(){
        val companyMenuRef = CompanyID?.let {
            db.child("companies").child(it).child("Menu")
        }

        companyMenuRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Deserialize snapshot into MenuGroup object
                    val fetchedMenu = snapshot.getValue(MenuGroup::class.java)
                    if (fetchedMenu != null) {
                        // Assign to local variable or state
                        menu = fetchedMenu
                        Toast.makeText(
                            context,
                            "Menu loaded successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Failed to parse menu data.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Menu not found.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    context,
                    "Error loading menu: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
        updateMenuUI(menuLayout)
        updateEditMenuUI(editMenuLayout)
    }
    private fun updateEditMenuUI(parentLayout: LinearLayout) {
        // Nejprve vyčistit layout
        parentLayout.removeAllViews()

        // Vnitřní rekurzivní funkce pro vykreslení MenuGroup
        fun renderMenuGroup(menuGroup: MenuGroup, parent: LinearLayout) {
            // Vytvoření hlavičky MenuGroup
            val groupHeader = TextView(context).apply {
                text = "${menuGroup.name}:"
                textSize = 25f
                setPadding(16, 16, 16, 16)
                tag = menuGroup.id
            }
            groupHeader.setOnClickListener { view ->
                CustomClickListener(
                    onClick = {
                        selectedGroupID = groupHeader.tag.toString()
                        Log.e("testik", selectedGroupID!!)
                    },
                    onDoubleClick = {
                        selectedGroupID = groupHeader.tag.toString()
                        groupOptionsPopup(selectedGroupID!!)
                    }
                ).onClick(view)
            }

            // Vytvoření layoutu pro podskupiny
            val menuGroupLayout = LinearLayout(context).apply {
                setBackgroundColor(android.graphics.Color.LTGRAY)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(32, 0, 0, 0)
                }
                orientation = LinearLayout.VERTICAL
            }

            // Přidání hlavičky skupiny do layoutu skupiny
            menuGroupLayout.addView(groupHeader)

            // Přidání všech položek do layoutu skupiny
            menuGroup.items?.forEach { menuItem ->
                val itemView = TextView(context).apply {
                    text = "${menuItem.name} - ${menuItem.price} Kč"
                    textSize = 25f
                    setPadding(16, 16, 16, 16)
                    tag = menuItem.id
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(32, 0, 0, 8)
                    }
                }
                itemView.setOnClickListener { view ->
                    CustomClickListener(
                        onClick = {

                        },
                        onDoubleClick = {
                            selectedItemID = itemView.tag.toString()
                            itemOptionPopup(selectedItemID!!)
                        }
                    ).onClick(view)
                }

                menuGroupLayout.addView(itemView)
            }

            // Rekurzivně vykreslit podskupiny
            menuGroup.subGroups?.forEach { subGroup ->
                renderMenuGroup(subGroup, menuGroupLayout)
            }

            // Přidání vytvořeného layoutu skupiny do rodičovského layoutu
            parent.addView(menuGroupLayout)
        }

        // Pokud je hlavní menu null, nic nevykreslíme
        menu?.let {
            // TODO dodelat tamtu vec
            renderMenuGroup(it, parentLayout)
        }
    }
    private fun updateMenuUI(parentLayout: LinearLayout){
        // Nejprve vyčistit layout
        parentLayout.removeAllViews()

        // Vnitřní rekurzivní funkce pro vykreslení MenuGroup
        fun renderMenuGroup(menuGroup: MenuGroup, parent: LinearLayout) {
            // Vytvoření hlavičky MenuGroup
            val groupHeader = TextView(context).apply {
                text = "${menuGroup.name}:"
                textSize = 25f
                setPadding(16, 16, 16, 16)
                tag = menuGroup.id
            }

            // Vytvoření layoutu pro podskupiny
            val menuGroupLayout = LinearLayout(context).apply {
                setBackgroundColor(android.graphics.Color.LTGRAY)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(32, 0, 0, 0)
                }
                orientation = LinearLayout.VERTICAL
            }

            // Přidání hlavičky skupiny do layoutu skupiny
            menuGroupLayout.addView(groupHeader)

            // Přidání všech položek do layoutu skupiny
            menuGroup.items?.forEach { menuItem ->
                val itemView = TextView(context).apply {
                    text = "${menuItem.name} - ${menuItem.price} Kč"
                    textSize = 25f
                    setPadding(16, 16, 16, 16)
                    tag = menuItem.id
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(32, 0, 0, 8)
                    }
                }
                itemView.setOnClickListener { view ->
                    CustomClickListener(
                        onClick = {

                        },
                        onDoubleClick = {
                            selectedItemID = itemView.tag.toString()
                            itemOptionPopup(selectedItemID!!)
                        }
                    ).onClick(view)
                }
                menuGroupLayout.addView(itemView)
            }

            // Rekurzivně vykreslit podskupiny
            menuGroup.subGroups?.forEach { subGroup ->
                renderMenuGroup(subGroup, menuGroupLayout)
            }

            // Přidání vytvořeného layoutu skupiny do rodičovského layoutu
            parent.addView(menuGroupLayout)
        }

        // Pokud je hlavní menu null, nic nevykreslíme
        menu?.let {
            // TODO dodelat tamtu vec
            renderMenuGroup(it, parentLayout)
        }
    }
}