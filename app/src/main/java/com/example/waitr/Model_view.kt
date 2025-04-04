package com.example.waitr

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class Model_view : Fragment() {
    //promenne
    private var CompanyID: String? = null
    private lateinit var editButton: ImageButton
    private lateinit var helpButton: ImageButton
    private lateinit var modelScenesBar: LinearLayout
    private lateinit var currentScene: ConstraintLayout
    private val db = FirebaseDatabase.getInstance("https://waitr-dee9a-default-rtdb.europe-west1.firebasedatabase.app/").reference // Using Realtime Database reference
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser
    private val userId = currentUser?.uid
    private lateinit var noStagesTODisplayTextView: TextView
    private lateinit var addStageButtonIfNon: ImageButton
    private lateinit var dynamicLinearLayout: LinearLayout
    private lateinit var editModelDialoge: Dialog
    private lateinit var editModelScenesLayout: LinearLayout
    private lateinit var editModelSceneLayout: ConstraintLayout
    private var model: Model = Model(mutableListOf())
    private var editModel: Model = Model(mutableListOf())
    private var tableEditMode: Boolean = false
    private lateinit var saveButton: TextView
    private lateinit var cancelButton: TextView
    private lateinit var addTableButton: TextView
    private lateinit var addSceneButton: TextView
    private lateinit var addHelperButton: TextView
    private lateinit var confirmTableChanges: ImageButton
    private lateinit var currentTableToEdit: TextView
    private lateinit var currentHelperToEdit: TextView
    private var finalBiasX: Float = 0f
    private var finalBiasY: Float = 0f
    private var selectedStageId: String? = null
    private var selectedTableId: String? = null
    private var selectedHelperId: String? = null
    private var helperEditMode: Boolean = false
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialBiasX: Float = 0f
    private var initialBiasY: Float = 0f
    private var newTableName: String? = null
    private var newTableHeight: Int? = null
    private var newTableWidth: Int? = null
    private var newHelperHeight: Int? = null
    private var newHelperWidth: Int? = null
    private var newSceneName: String? = null
    private lateinit var tableOptionsDialog: Dialog
    private lateinit var nameOfTheTable: TextView
    private lateinit var heightOfTheTable: TextView
    private lateinit var widthOfTheTable: TextView
    private lateinit var helperOptionsDialog: Dialog
    private lateinit var heightOfTheHelper: TextView
    private lateinit var widthOfTheHelper: TextView
    private lateinit var sceneOptionsDialog: Dialog
    private lateinit var nameOfTheScene: TextView
    private lateinit var emptyTableManagingDialog: Dialog
    private lateinit var seatedTableManagingDialog: Dialog
    private lateinit var checkOutButton: Button
    private lateinit var tableOrdersLayout: LinearLayout
    private lateinit var tableTotalPriceTextView: TextView
    private var selectedCustomerId: String? = null
    private var selectedItemFromOrderId: String? = null
    private var allMenuItems: MutableList<MenuItem> = mutableListOf()
    private lateinit var paidTableManagingDialog: Dialog
    private val tableStateListeners = mutableMapOf<String, ValueEventListener>()
    private var seatedTableNotificationPeriod = 5
    private var eatingTableNotificationPeriod = 5
    private var paidTableNotificationPeriod = 5
    private lateinit var authorization: String
    private lateinit var fragmentContext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentContext = context // Uložení Contextu do globální proměnné
    }

    // zde psat pouze kod nesouvisejici s UI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    // Cekati na predani argumentu z aktivity do promene na CompanyID
    arguments?.getString(COMPANY_ID)?.let {
        CompanyID = it
    }
    listenForSettingsChanges()
    }
// zde psat kod souvisejici s UI
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
    // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_model_view, container, false)
    // nastaveni promenne editModelDialoge
    editModelDialoge = Dialog(fragmentContext)
    editModelDialoge.setContentView(R.layout.edit_model_view_popup)
    val scrollableView = editModelDialoge.findViewById<HorizontalScrollView>(R.id.edit_model_horizontal_scroll_view)
    editModelScenesLayout = scrollableView.findViewById(R.id.linearlayout_for_scenes)
    editModelSceneLayout = editModelDialoge.findViewById(R.id.edit_model_canvas_layout)
    confirmTableChanges = editModelDialoge.findViewById(R.id.confirm_table_changes_button)
    tableOptionsDialog = Dialog(fragmentContext)
    tableOptionsDialog.setContentView(R.layout.table_options_popup)
    nameOfTheTable = tableOptionsDialog.findViewById(R.id.name_of_the_table)
    heightOfTheTable = tableOptionsDialog.findViewById(R.id.height_of_the_table)
    widthOfTheTable = tableOptionsDialog.findViewById(R.id.width_of_the_table)
    helperOptionsDialog = Dialog(fragmentContext)
    helperOptionsDialog.setContentView(R.layout.helper_options_popup)
    heightOfTheHelper = helperOptionsDialog.findViewById(R.id.height_of_the_helper)
    widthOfTheHelper = helperOptionsDialog.findViewById(R.id.width_of_the_helper)
    sceneOptionsDialog = Dialog(fragmentContext)
    sceneOptionsDialog.setContentView(R.layout.scene_options_popup)
    nameOfTheScene = sceneOptionsDialog.findViewById(R.id.name_of_the_scene)
    emptyTableManagingDialog = Dialog(fragmentContext)
    emptyTableManagingDialog.setContentView(R.layout.managing_table_empty_state)
    seatedTableManagingDialog = Dialog(fragmentContext)
    seatedTableManagingDialog.setContentView(R.layout.managing_table_seated_state)
    checkOutButton = seatedTableManagingDialog.findViewById<Button>(R.id.manage_table_check_out_button)
    tableOrdersLayout = seatedTableManagingDialog.findViewById(R.id.manage_customers_layout)
    tableTotalPriceTextView = seatedTableManagingDialog.findViewById(R.id.manage_table_view_total_price_of_the_table)
    paidTableManagingDialog = Dialog(fragmentContext)
    paidTableManagingDialog.setContentView(R.layout.managing_table_paid_state)

    //nastaveni akce po kliknutí u tlačítka na potvrzení polohy položky v modelu
    confirmTableChanges.setOnClickListener {
        if (tableEditMode){
            tableEditMode = false
            val table = findTableById(editModel, selectedTableId!!)
            table?.xPosition = finalBiasX
            table?.yPosition = finalBiasY

            confirmTableChanges.visibility = View.GONE
            saveButton.visibility = View.VISIBLE
            cancelButton.visibility = View.VISIBLE
            addTableButton.visibility = View.VISIBLE
            addSceneButton.visibility = View.VISIBLE
            addHelperButton.visibility = View.VISIBLE
            editModelScenesLayout.visibility = View.VISIBLE
            Log.e("model", editModel.toString())
        }
        if (helperEditMode){
            helperEditMode = false
            val helper = findHelperById(editModel, selectedHelperId!!)
            helper?.xPosition = finalBiasX
            helper?.yPosition = finalBiasY

            confirmTableChanges.visibility = View.GONE
            saveButton.visibility = View.VISIBLE
            cancelButton.visibility = View.VISIBLE
            addTableButton.visibility = View.VISIBLE
            addSceneButton.visibility = View.VISIBLE
            addHelperButton.visibility = View.VISIBLE
            editModelScenesLayout.visibility = View.VISIBLE
            Log.e("model", editModel.toString())
        }
    }

    currentScene = view.findViewById(R.id.canvas_layout)
    val horizontalScrollView = view.findViewById<HorizontalScrollView>(R.id.horizontal_scroll_view)
    modelScenesBar = horizontalScrollView.findViewById(R.id.scenes_bar)
    noStagesTODisplayTextView = TextView(fragmentContext).apply {
        text = "You need to create your first scene "
        textSize = 25f
        gravity = Gravity.CENTER
    }
    addStageButtonIfNon = ImageButton(fragmentContext).apply {
        contentDescription = "Add Stage"
        setImageResource(R.drawable.baseline_add_24)
        scaleType = ImageView.ScaleType.FIT_CENTER
        setBackgroundColor(Color.parseColor("#673AB7"))
        layoutParams = LinearLayout.LayoutParams(
            300,
            300
        ).apply {
            setMargins(0, 16, 0, 0)
            gravity = Gravity.CENTER
        }
        setOnClickListener {
            showEditModelPopUp()
        }
    }
    dynamicLinearLayout = LinearLayout(fragmentContext).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            // Připojit k rodičovskému ConstraintLayout ze všech stran
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }

        addView(noStagesTODisplayTextView)
        addView(addStageButtonIfNon)
    }
    checkIfSceneExists()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editButton = view.findViewById(R.id.edit_button)
        editButton.setOnClickListener {
            showEditModelPopUp()
        }
        // Zavoláme getAuthorization s callbackem
        getAuthorization { auth ->
            // Tento blok se zavolá až po načtení hodnoty authorization
            if (auth != null && auth == "employee") {
                editButton.visibility = View.GONE
            }
        }
        helpButton = view.findViewById(R.id.help_button)
        helpButton.setOnClickListener {
            modelInfoPopup()
        }
        // nacte model z database ktery nasledne vykresli
        fetchModel{
            checkIfSceneExists()
        }
        // nacte vsechny MenuItem polozky z Menu
        fetchAllMenuItems{}
    }
    //nacte pozici uzivatele pro authorizaci ve spolecnosti
    private fun getAuthorization(callback: (String?) -> Unit) {
        val authRef = CompanyID?.let { companyId ->
            userId?.let { uid ->
                db.child("companies").child(companyId).child("users").child(uid).child("authorization")
            } ?: run {
                Log.e("error", "chyba pri ziskani id uzivatele")
                null
            }
        }

        authRef?.get()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val dataSnapshot = task.result
                authorization = dataSnapshot.getValue(String::class.java).toString()
                Log.d("Authorization", "Hodnota authorization: ${authorization ?: "Nenalezeno"}")
                callback(authorization) // Zavolá callback s hodnotou authorization
            } else {
                Log.e("error", "Chyba pri ziskavani dat", task.exception)
                callback(null) // Zavolá callback s null v případě chyby
            }
        }
    }
// metoda pro obsluhu prázdného stolu
    private fun manageEmptyTablePopup(){
        val table = findTableById(model, selectedTableId!!)
        val name = table?.name
        emptyTableManagingDialog.setOnDismissListener {
            table?.locked = null
            val tableToUpdate = findTableById(model, selectedTableId!!)
            if (tableToUpdate != null && table != null) {
                tableToUpdate.locked = table.locked
            }
            updateModel {}
        }
        if (table?.locked == null) {
            table?.locked = userId
            updateModel {}
            emptyTableManagingDialog.setContentView(R.layout.managing_table_empty_state)
            emptyTableManagingDialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.95).toInt(),
                (resources.displayMetrics.heightPixels * 0.95).toInt()
            )
            val tableName =
                emptyTableManagingDialog.findViewById<TextView>(R.id.name_of_the_table_to_manage)
            tableName.text = name
            val newCustomersButton =
                emptyTableManagingDialog.findViewById<Button>(R.id.new_customers_button)
            newCustomersButton.setOnClickListener {
                emptyTableManagingDialog.setContentView(R.layout.managing_table_set_customers)
                val spinner =
                    emptyTableManagingDialog.findViewById<Spinner>(R.id.number_of_customers_spinner)
                val options =
                    listOf("Number of people", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
                val adapter =
                    ArrayAdapter(fragmentContext, android.R.layout.simple_spinner_item, options)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
                val confirmButton =
                    emptyTableManagingDialog.findViewById<Button>(R.id.confirm_number_of_customers_button)
                confirmButton.setOnClickListener {
                    val selectedOption = spinner.selectedItem.toString()
                    if (selectedOption.equals("Number of people", ignoreCase = true)) {
                        Toast.makeText(
                            requireContext(),
                            "Please select the number of people",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                    val numberOfPeople = convertStringToInt(spinner.selectedItem.toString())
                    table?.numberOfPeople = numberOfPeople
                    table?.state = "seated"
                    for (i in 1..numberOfPeople) {
                        val randomID = UUID.randomUUID().toString()
                        val newCustomer =
                            Customer(randomID, "Person ${i}", Order(mutableListOf(), 0.0))
                        table?.listOfCustomers?.add(newCustomer)
                    }
                    val tableToUpdate = findTableById(model, selectedTableId!!)
                    if (tableToUpdate != null && table != null) {
                        tableToUpdate.state = table.state
                        tableToUpdate.numberOfPeople = table.numberOfPeople
                        tableToUpdate.listOfCustomers = table.listOfCustomers
                    }
                    updateModel {
                        emptyTableManagingDialog.dismiss()
                    }
                }
            }
            val closeButton =
                emptyTableManagingDialog.findViewById<Button>(R.id.close_empty_table_button)
            closeButton.setOnClickListener {
                emptyTableManagingDialog.dismiss()
            }
            emptyTableManagingDialog.show()
        } else {
            Toast.makeText(
                requireContext(),
                "This table is currently being edited by someone!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    //metoda pro obsluhu zabraného stolu
    private fun manageSeatedTablePopup(){
        val table = findTableById(model, selectedTableId!!)
        val name = table?.name
        seatedTableManagingDialog.setOnDismissListener {
            table?.locked = null

            val tableToUpdate = findTableById(model, selectedTableId!!)
            if (tableToUpdate != null && table != null) {
                tableToUpdate.locked = table.locked
            }
            updateModel {}
        }
        if (table?.locked == null){
            table?.locked = userId
            updateModel {}
            seatedTableManagingDialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.95).toInt(),
                (resources.displayMetrics.heightPixels * 0.95).toInt()
            )
            // Reference na prvky
            val displayName = seatedTableManagingDialog.findViewById<TextView>(R.id.manage_table_view_name_of_the_table)
            displayName.text = name
            checkOutButton.setOnClickListener {
                if (table != null) {
                    proceedToCheckoutPopup()
                }
                seatedTableManagingDialog.dismiss()
            }
            val closeButton = seatedTableManagingDialog.findViewById<Button>(R.id.close_seated_table_manager)
            closeButton.setOnClickListener {
                seatedTableManagingDialog.dismiss()
            }
            setCheckoutButton(checkOutButton, table!!)
            drawTableOrders()

            seatedTableManagingDialog.show()
        } else {
            Toast.makeText(
                requireContext(),
                "This table is currently being edited by someone!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    //vykreslí objednávky zákazníků
    private fun drawTableOrders(){
        val table = findTableById(model, selectedTableId!!)
        tableOrdersLayout.removeAllViews()
        table?.listOfCustomers?.forEach { customer ->
           val customerLayout = LinearLayout(fragmentContext).apply {
               layoutParams = LinearLayout.LayoutParams(
                   LinearLayout.LayoutParams.WRAP_CONTENT,
                   LinearLayout.LayoutParams.WRAP_CONTENT
               ).apply {
                   setMargins(32, 0, 0, 0)
               }
               orientation = LinearLayout.VERTICAL
           }

           val customerHeaderLayout = LinearLayout(fragmentContext).apply {
               layoutParams = LinearLayout.LayoutParams(
                   LinearLayout.LayoutParams.WRAP_CONTENT,
                   LinearLayout.LayoutParams.WRAP_CONTENT
               )
               orientation = LinearLayout.HORIZONTAL
           }

           val customerView = TextView(fragmentContext).apply {
               text = customer.name
               textSize = 25f
               setPadding(16, 16, 16, 16)
               tag = customer.id
                   layoutParams = LinearLayout.LayoutParams(
                   LinearLayout.LayoutParams.WRAP_CONTENT,
                   LinearLayout.LayoutParams.WRAP_CONTENT
               ).apply {
                   setMargins(0, 0, 16, 8)
               }
           }
           customerView.setOnClickListener{
               selectedCustomerId = customerView.tag.toString()
               displayDataOfACustomerPopup(table)
           }

           val addItemsImageButton = ImageButton(fragmentContext).apply {
               setImageResource(R.drawable.baseline_add_24)
               tag = customer.id
               layoutParams = LinearLayout.LayoutParams(
                   LinearLayout.LayoutParams.WRAP_CONTENT,
                   LinearLayout.LayoutParams.WRAP_CONTENT
               )
               setPadding(16, 16, 16, 16)
               scaleType = ImageView.ScaleType.CENTER_INSIDE
               setBackgroundColor(Color.GREEN)
           }
           addItemsImageButton.setOnClickListener {
               selectedCustomerId = addItemsImageButton.tag.toString()
               addItemsToOrderPopup(table)
           }
           customerHeaderLayout.addView(customerView)
           customerHeaderLayout.addView(addItemsImageButton)
           customerLayout.addView(customerHeaderLayout)

           customer.order.menuItems.forEach { menuItem ->
               val itemHeaderLayout = LinearLayout(fragmentContext).apply {
                   layoutParams = LinearLayout.LayoutParams(
                       LinearLayout.LayoutParams.WRAP_CONTENT,
                       LinearLayout.LayoutParams.WRAP_CONTENT
                   ).apply {
                       setMargins(32, 0, 5, 0)
                   }
                   orientation = LinearLayout.HORIZONTAL
               }

               val itemView = TextView(fragmentContext).apply {
                   text = "${menuItem.name} - ${menuItem.price} Kč"
                   textSize = 20f
                   setPadding(16, 16, 16, 16)
                   tag = ItemInOrderTag(menuItem.id, customer.id)
                   layoutParams = LinearLayout.LayoutParams(
                       LinearLayout.LayoutParams.WRAP_CONTENT,
                       LinearLayout.LayoutParams.WRAP_CONTENT
                   )
               }
               itemView.setOnClickListener {
                   val params = itemView.tag as ItemInOrderTag
                   selectedItemFromOrderId = params.menuId
                   selectedCustomerId = params.CustomerId
                   displayDataOfTheItemInOrderPopup(table)
               }
               if (!menuItem.served){
                   val waitingImageButton = ImageButton(fragmentContext).apply {
                       setImageResource(R.drawable.baseline_access_time_24)
                       layoutParams = LinearLayout.LayoutParams(
                           LinearLayout.LayoutParams.WRAP_CONTENT,
                           LinearLayout.LayoutParams.WRAP_CONTENT
                       )
                       setPadding(16, 16, 16, 16)
                       scaleType = ImageView.ScaleType.CENTER_INSIDE
                       setBackgroundColor(Color.YELLOW)
                   }
                   itemHeaderLayout.addView(waitingImageButton)
               }
               itemHeaderLayout.addView(itemView)
               customerLayout.addView(itemHeaderLayout)
           }
           tableOrdersLayout.addView(customerLayout)
       }
        setCheckoutButton(checkOutButton, table!!)
       tableTotalPriceTextView.text = "Total table price: ${table?.totalTablePrice} Kč"
    }

    //metoda co nastavuje zda je k dispozici tlsčítko pro přesun na platbu objednavek
    private fun setCheckoutButton(button: Button, table: Table){
        if (table.state.equals("eating")) {
            button.isEnabled = true
            button.alpha = 1.0f
        } else {
            button.isEnabled = false
            button.alpha = 0.5f
        }
    }

    //Zobrazí potrobnějí objednávku zákazníka
    private fun displayDataOfACustomerPopup(table: Table){
        val customer = findCustomerById(table, selectedCustomerId)
        val name = customer?.name
        val orderPrice = customer?.order?.totalPrice

        val dialog = Dialog(fragmentContext)
        dialog.setContentView(R.layout.display_data_of_a_customer_layout)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.95).toInt()
        )

        val displayName = dialog.findViewById<TextView>(R.id.display_customer_name)
        displayName.text = name
        val displayTotalOrderPrice = dialog.findViewById<TextView>(R.id.display_total_price_of_the_order)
        displayTotalOrderPrice.text = "Total price: ${orderPrice} Kč"
        val layoutToDisplayOrder = dialog.findViewById<LinearLayout>(R.id.display_orders_layout)
        customer?.order?.menuItems?.forEach { menuItem ->
            val itemView = TextView(fragmentContext).apply {
                text = "${menuItem.name} - ${menuItem.price} Kč"
                textSize = 25f
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 0, 0,0)
                }
            }
            layoutToDisplayOrder.addView(itemView)
        }
        val closeButton = dialog.findViewById<Button>(R.id.close_display_of_the_customer_button)
        closeButton.setOnClickListener {
            selectedCustomerId = null
            dialog.dismiss()
        }
        dialog.show()
    }

    //zobrazí podrobněji data o položce v objednávce
    private fun displayDataOfTheItemInOrderPopup(table: Table){
        val customer = findCustomerById(table, selectedCustomerId)
        val item = findItemInOrderById(customer!!, selectedItemFromOrderId)
        val name = item?.name
        val price = item?.price

        val dialog = Dialog(fragmentContext)
        dialog.setContentView(R.layout.display_item_data_of_a_order_layout)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.95).toInt()
        )

        val displayName = dialog.findViewById<TextView>(R.id.display_the_name_of_the_item_in_order)
        displayName.text = name
        val displayPrice = dialog.findViewById<TextView>(R.id.display_price_of_the_item)
        displayPrice.text = "Price: ${price} Kč"
        val removeItemButton = dialog.findViewById<Button>(R.id.remove_item_from_order_button)
        removeItemButton.setOnClickListener {
            if (item != null) {

                customer.order.deleteItem(item.id)
                customer.order.totalPrice -= item.price
                table.totalTablePrice -= item.price
                if (!checkIfTableHasOrdered(table)) table.state = "seated"
                if (checkIfAllOrdersServed(table)) table.state = "eating"

                val tableToUpdate = findTableById(model, selectedTableId!!)
                if (tableToUpdate != null){
                    tableToUpdate.totalTablePrice = table.totalTablePrice
                    tableToUpdate.listOfCustomers = table.listOfCustomers
                }
            }
            updateModel{
                drawTableOrders()
                dialog.dismiss()
            }
        }
        val servedButton = dialog.findViewById<Button>(R.id.mark_item_as_served_button)
        servedButton.setOnClickListener {
            item?.served = true
            if (checkIfAllOrdersServed(table)) table.state = "eating"
            servedButton.visibility = View.GONE
            removeItemButton.visibility = View.GONE

            val tableToUpdate = findTableById(model, selectedTableId!!)
            if (tableToUpdate != null){
                tableToUpdate.state = table.state
                tableToUpdate.listOfCustomers = table.listOfCustomers
            }
            updateModel{
                drawTableOrders()
                dialog.dismiss()
            }
        }
        val closeButton = dialog.findViewById<Button>(R.id.close_display_of_the_item_button)
        closeButton.setOnClickListener {
            selectedCustomerId = null
            selectedItemFromOrderId = null
            dialog.dismiss()
        }
        if (item != null) {
            if (item.served){
                servedButton.visibility = View.GONE
                removeItemButton.visibility = View.GONE
            }
        }
        dialog.show()
    }

    //metoda pro přidání položky k objednávce
    private fun addItemsToOrderPopup(table: Table){
        val customer = findCustomerById(table, selectedCustomerId)

        val dialog = Dialog(fragmentContext)
        dialog.setContentView(R.layout.add_item_to_order_layout)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.95).toInt()
        )

        val editTextSearch = dialog.findViewById<EditText>(R.id.editTextSearch)
        val recyclerView = dialog.findViewById<RecyclerView>(R.id.recyclerView)
        if (customer != null) {
            liveSearch(editTextSearch, recyclerView, allMenuItems, customer.order, table)
        }
        val doneButton = dialog.findViewById<Button>(R.id.done_adding_items_button)
        doneButton.setOnClickListener {
            if (table.state.equals("seated")) table.state = "ordered"
            if (table.state.equals("eating")) table.state = "ordered"

            val tableToUpdate = findTableById(model, selectedTableId!!)
            if (tableToUpdate != null){
                tableToUpdate.state = table.state
                tableToUpdate.listOfCustomers = table.listOfCustomers
                tableToUpdate.totalTablePrice = table.totalTablePrice
            }
            updateModel{
                drawTableOrders()
                dialog.dismiss()
            }
        }
        dialog.show()
    }
    //metoda pro obsluhu stolu při placení
    private fun proceedToCheckoutPopup(){
        val table = findTableById(model, selectedTableId!!)
        val price = table?.totalTablePrice
        // HashMap pro sledování počtu výskytů jednotlivých menuItem.id
        val overAllItemsCounts = mutableMapOf<String, Int>()

        val dialog = Dialog(fragmentContext)
        dialog.setContentView(R.layout.managing_table_proceed_to_checkout)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.95).toInt()
        )

        val displayPrice = dialog.findViewById<TextView>(R.id.checkout_total_table_price)
        displayPrice.text = "Total table price: ${price} Kč"
        val continueButton = dialog.findViewById<Button>(R.id.checkout_continue_button)
        continueButton.setOnClickListener {
            if (table != null) {
                updateDataInAnalytics(table.id, overAllItemsCounts)
            }
            table?.state = "paid"
            val iterator = table?.listOfCustomers?.iterator()
            if (iterator != null) {
                while (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
            if (table != null) {
                table.totalTablePrice = 0.0
                table.listOfCustomers.forEach { customer ->
                    customer.order.totalPrice = 0.0
                }
                table.numberOfPeople = 0
            }

            val tableToUpdate = findTableById(model, selectedTableId!!)
            if (tableToUpdate != null && table != null){
                tableToUpdate.locked = null
                tableToUpdate.state = table.state
                tableToUpdate.listOfCustomers = table.listOfCustomers
                tableToUpdate.totalTablePrice = table.totalTablePrice
                tableToUpdate.numberOfPeople = table.numberOfPeople
            }
            updateModel{
                dialog.dismiss()
            }
        }
        val displayLayout = dialog.findViewById<LinearLayout>(R.id.check_out_customers_layout)
        table?.listOfCustomers?.forEach { customer ->
            // Nejprve spočítáme výskyty
            val itemCounts = mutableMapOf<String, Int>()
            customer.order.menuItems.forEach { menuItem ->
                itemCounts[menuItem.name] = itemCounts.getOrDefault(menuItem.name, 0) + 1
                overAllItemsCounts[menuItem.name] = overAllItemsCounts.getOrDefault(menuItem.name, 0) + 1
            }

            val customerLayout = LinearLayout(fragmentContext).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(32, 0, 0, 0)
                }
                orientation = LinearLayout.VERTICAL
            }
            val customerHeaderLayout = LinearLayout(fragmentContext).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(10,10,10,10)
                }
                orientation = LinearLayout.HORIZONTAL
            }
            val customerView = TextView(fragmentContext).apply {
                text = "${customer.name}: ${customer.order.totalPrice} Kč"
                textSize = 25f
                setPadding(16, 16, 16, 16)
                tag = customer.id
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins(0, 0, 16, 8)
                }
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
            }
            val paidButton = Button(fragmentContext).apply {
                tag = customer.id
                text = "Pay"
                textSize = 20f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 10, 5, 8)
                }
                setPadding(16, 16, 16, 16)
                setBackgroundColor(Color.YELLOW)
            }
            paidButton.setOnClickListener {
                selectedCustomerId = paidButton.tag.toString()
                val cust = findCustomerById(table, selectedCustomerId)
                cust?.order?.paid = true
                paidButton.text = "Paid"
                paidButton.setBackgroundColor(Color.GREEN)
                paidButton.isEnabled = false
                checkIfAllCustomersPaid(table, continueButton)
            }
            customerHeaderLayout.addView(customerView)
            customerHeaderLayout.addView(paidButton)
            customerLayout.addView(customerHeaderLayout)

            itemCounts.forEach { (itemName, count) ->
                val itemView = TextView(fragmentContext).apply {
                    text = "${count}x ${itemName}"  // Nastavení textu s počtem výskytů
                    textSize = 20f
                    setPadding(16, 16, 16, 16)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(32, 0, 0, 0)
                    }
                }
                customerLayout.addView(itemView)
            }
            displayLayout.addView(customerLayout)
        }
        checkIfAllCustomersPaid(table!!, continueButton)
        dialog.show()
    }

    //obsluha zaplaceného stolu
    private fun managePaidTablePopup(){
        val table = findTableById(model, selectedTableId!!)
        val name = table?.name
        paidTableManagingDialog.setOnDismissListener {
            table?.locked = null

            val tableToUpdate = findTableById(model, selectedTableId!!)
            if (tableToUpdate != null && table != null){
                tableToUpdate.locked = table.locked
            }
            updateModel {}
        }
        if (table?.locked == null) {
            table?.locked = userId
            updateModel {}
            paidTableManagingDialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.95).toInt(),
                (resources.displayMetrics.heightPixels * 0.95).toInt()
            )
            val displayName = paidTableManagingDialog.findViewById<TextView>(R.id.paid_table_name)
            displayName.text = name
            val doneButton = paidTableManagingDialog.findViewById<Button>(R.id.done_with_table_managing)
            doneButton.setOnClickListener {
                table?.state = "empty"

                val tableToUpdate = findTableById(model, selectedTableId!!)
                if (tableToUpdate != null && table != null){
                    tableToUpdate.state = table.state
                }
                updateModel {
                    paidTableManagingDialog.dismiss()
                }
            }
            val closeButton = paidTableManagingDialog.findViewById<Button>(R.id.close_paid_table)
            closeButton.setOnClickListener {
                paidTableManagingDialog.dismiss()
            }
            paidTableManagingDialog.show()
        } else {
            Toast.makeText(
                requireContext(),
                "This table is currently being edited by someone!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    //metoda co zjistí jestli všichni zákazníci platili
    private fun checkIfAllCustomersPaid(table: Table, button: Button): Boolean{
        table.listOfCustomers.forEach { customer ->
            if (!customer.order.paid){
                button.alpha = 0.5f
                button.isEnabled = false
                return false
            }
        }
        button.alpha = 1f
        button.isEnabled = true
        return true
    }

    //metoda pro zjištění jestli všechny položky v objednávce jsou přineseny
    private fun checkIfAllOrdersServed(table: Table): Boolean{
        table.listOfCustomers.forEach { customer ->
            customer.order.menuItems.forEach { menuItem ->
                if (!menuItem.served) return false
            }
        }
        return true
    }

    private fun checkIfTableHasOrdered(table: Table): Boolean {
        table.listOfCustomers.forEach { customer ->
            if (customer.order.menuItems.isNotEmpty()) return true
        }
        return false
    }

    private fun findCustomerById(table: Table, id: String?): Customer? {
        table.listOfCustomers.forEach { customer ->
            if (customer.id == id){
                return customer
            }
        }
        return null
    }

    private fun findItemInOrderById(customer: Customer, id: String?): MenuItem? {
        customer.order.menuItems.forEach { menuItem ->
            if (menuItem.id == id){
                return menuItem
            }
        }
        return null
    }

    private fun convertStringToInt(input: String): Int {
        return when (input) {
            "1" -> 1
            "2" -> 2
            "3" -> 3
            "4" -> 4
            "5" -> 5
            "6" -> 6
            "7" -> 7
            "8" -> 8
            "9" -> 9
            "10" -> 10
            else -> throw IllegalArgumentException("Invalid input: $input")
        }
    }

    private fun liveSearch(editTextSearch: EditText, recyclerView: RecyclerView, menuItems: List<MenuItem>, order: Order, table: Table) {
        val adapter = MenuAdapter(menuItems) { selectedItem ->
            // Akce při kliknutí na "Add"
            val randomId = UUID.randomUUID().toString()
            val menuItem = MenuItem(
                randomId,
                selectedItem.name,
                selectedItem.price,
                selectedItem.description,
                false)
            order.menuItems.add(menuItem)
            order.totalPrice += selectedItem.price
            table.totalTablePrice += selectedItem.price
            Toast.makeText(recyclerView.context, "${selectedItem.name} added", Toast.LENGTH_SHORT).show()
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)

        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase().trim()
                val filteredList = menuItems.filter { it.name.lowercase().contains(query) }
                adapter.updateData(filteredList)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun showEditModelPopUp(){
        Log.e("locked", model.locked.toString())
        editModelDialoge.setOnDismissListener {
            model.locked = null
            updateModel {}
        }
        if (model.locked == null) {
            model.locked = userId
            updateModel {
                editModel = model.deepCopy()
            }
            //vykresleni scen
            drawEditScenesToBar()
            // nacteni sceny
            if (editModel.listOfScenes.isNotEmpty()) {
                selectedStageId = editModel.listOfScenes.get(0).id
                drawEditScene()
            } else {
                val textView = TextView(fragmentContext).apply {
                    this.text = "No Scene"
                    this.textSize = 18f // Velikost textu
                    this.setTextColor(Color.LTGRAY) // Barva textu
                }
                val params = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )

                editModelSceneLayout.addView(textView, params)

                textView.post {
                    val parentWidth = editModelSceneLayout.width
                    val parentHeight = editModelSceneLayout.height
                    val textViewWidth = textView.width
                    val textViewHeight = textView.height
                    val leftMargin = (parentWidth - textViewWidth) / 2
                    val topMargin = (parentHeight - textViewHeight) / 2

                    params.leftMargin = leftMargin
                    params.topMargin = topMargin

                    textView.layoutParams = params
                }
            }
            // Nastavení velikosti dialogu
            editModelDialoge.window?.setLayout(
                (resources.displayMetrics.widthPixels * 1),
                (resources.displayMetrics.heightPixels * 1)
            )
            // Reference na prvky v popup layoutu
            saveButton = editModelDialoge.findViewById(R.id.save_model_edit)
            cancelButton = editModelDialoge.findViewById(R.id.cancel_model_edit)
            addTableButton = editModelDialoge.findViewById(R.id.add_table)
            addSceneButton = editModelDialoge.findViewById(R.id.add_scene)
            addHelperButton = editModelDialoge.findViewById(R.id.add_helper_shape)
            saveButton.setOnClickListener {
                model = editModel.deepCopy()
                updateModel {
                    editModelDialoge.dismiss()
                }
            }
            cancelButton.setOnClickListener {
                selectedStageId = null
                val builder = AlertDialog.Builder(fragmentContext)
                builder.setTitle("Cancel changes")
                builder.setMessage("Are you sure you want to cancel all the changes?")

                builder.setPositiveButton("Yes") { dialog, _ ->
                    dialog.dismiss()
                    editModelDialoge.dismiss()
                }
                builder.setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }

                val alertDialog = builder.create()
                alertDialog.show()
            }
            addTableButton.setOnClickListener {
                addTablePopup()
            }
            addSceneButton.setOnClickListener {
                addScenePopup()
            }
            addHelperButton.setOnClickListener {
                addHelperPopup()
            }

            editModelDialoge.show()
        } else {
            Toast.makeText(
                requireContext(),
                "model is already being edited by someone!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun modelInfoPopup(){
        // Vytvoření dialogu
        val dialog = Dialog(fragmentContext)
        dialog.setContentView(R.layout.info_for_model_popup)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels),
            (resources.displayMetrics.heightPixels)
        )

        val closeButton = dialog.findViewById<Button>(R.id.close_model_info_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addTablePopup(){
        // Vytvoření dialogu
        val dialog = Dialog(fragmentContext)
        dialog.setContentView(R.layout.add_table_to_model_popup)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.7).toInt(),
            (resources.displayMetrics.heightPixels * 0.6).toInt()
        )
        // Reference na prvky v popup layoutu
        val tableNameInput = dialog.findViewById<TextInputEditText>(R.id.table_name)
        val addButton = dialog.findViewById<Button>(R.id.add_table_button)
        addButton.setOnClickListener {
            if (!canAddNewObject(editModelSceneLayout)){
                Toast.makeText(dialog.context, "You cant add anymore objects", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (editModel.listOfScenes.isEmpty()){
                Toast.makeText(dialog.context, "First you need to create scene", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@setOnClickListener
            }
            val tableName = tableNameInput.text.toString().trim()
            if (tableName.isEmpty()){
                Toast.makeText(dialog.context, "You must enter the name first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (tableName.length > 20) {
                Toast.makeText(dialog.context, "Name cannot exceed 20 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val randomID = UUID.randomUUID().toString()
            val table = Table(
                randomID,
                tableName,
                "empty",
                0,
                mutableListOf(),
                0.0,
                150,
                150,
                0f,
                0f)
            val currentModelScene = selectedStageId?.let { it1 -> findSceneById(editModel, it1) }
            currentModelScene?.listOfTables?.add(table)
            Log.e("editmodel", editModel.toString())
            Log.e("model", model.toString())
            val textView = TextView(fragmentContext).apply {
                text = tableName
                textSize = 18f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.LTGRAY)

                // Nastavit ConstraintLayout.LayoutParams
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    // Nastavit šířku a výšku
                    width = 150 // Výchozí šířka
                    height = 150 // Výchozí výška

                    // Připojit k rodičovskému ConstraintLayout ze všech stran
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

                    // Nastavit bias pro levý horní roh
                    horizontalBias = 0f // 0.0 = úplně vlevo
                    verticalBias = 0f   // 0.0 = úplně nahoře
                }

                tag = TableTag(randomID, "empty")
            }
            textView.setOnClickListener(
                CustomClickListener(
                    onClick = {
                        val tableParams = textView.tag as TableTag
                        selectedTableId = tableParams.id
                        val state = tableParams.state
                        if (!state.equals("empty")){
                            Toast.makeText(dialog.context, "Can´t edit table when in use", Toast.LENGTH_SHORT).show()
                            return@CustomClickListener
                        }
                        if (!tableEditMode) {
                            tableEditMode = true
                            currentTableToEdit = textView
                            confirmTableChanges.visibility = View.VISIBLE
                            saveButton.visibility = View.GONE
                            cancelButton.visibility = View.GONE
                            addTableButton.visibility = View.GONE
                            addSceneButton.visibility = View.GONE
                            addHelperButton.visibility = View.GONE
                            editModelScenesLayout.visibility = View.GONE
                        }
                    },
                    onDoubleClick = {
                        val tableParams = textView.tag as TableTag
                        selectedTableId = tableParams.id
                        val state = tableParams.state
                        if (!state.equals("empty")){
                            Toast.makeText(dialog.context, "Can´t edit table when in use", Toast.LENGTH_SHORT).show()
                            return@CustomClickListener
                        }
                        tableOptionsPopup()
                    }
                )
            )
            editModelSceneLayout.addView(textView)

            textView.setOnTouchListener { view, event ->
                if (tableEditMode && currentTableToEdit == view) { // Povolit manipulaci pouze pro aktuálně editovaný stůl
                    val params = view.layoutParams as ConstraintLayout.LayoutParams
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // Inicializace proměnných při zahájení dotyku
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            initialBiasX = params.horizontalBias
                            initialBiasY = params.verticalBias
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = event.rawX - initialTouchX
                            val deltaY = event.rawY - initialTouchY

                            val scaleX = deltaX / editModelSceneLayout.width.toFloat()
                            val scaleY = deltaY / editModelSceneLayout.height.toFloat()

                            var newBiasX = initialBiasX + scaleX
                            var newBiasY = initialBiasY + scaleY

                            newBiasX = newBiasX.coerceIn(0f, 1f)
                            newBiasY = newBiasY.coerceIn(0f, 1f)

                            // Vypočítat navrhovanou novou pozici
                            val newLeft = (newBiasX * (editModelSceneLayout.width - view.width)).toInt()
                            val newTop = (newBiasY * (editModelSceneLayout.height - view.height)).toInt()
                            val newRight = newLeft + view.width
                            val newBottom = newTop + view.height
                            val newRect = Rect(newLeft, newTop, newRight, newBottom)

                            // Zkontrolovat kolize
                            var canMove = true
                            for (i in 0 until editModelSceneLayout.childCount) {
                                val otherView = editModelSceneLayout.getChildAt(i)
                                if (otherView != view && otherView is TextView) {
                                    val otherParams = otherView.layoutParams as ConstraintLayout.LayoutParams
                                    val otherLeft = (otherParams.horizontalBias * (editModelSceneLayout.width - otherView.width)).toInt()
                                    val otherTop = (otherParams.verticalBias * (editModelSceneLayout.height - otherView.height)).toInt()
                                    val otherRect = Rect(
                                        otherLeft,
                                        otherTop,
                                        otherLeft + otherView.width,
                                        otherTop + otherView.height
                                    )

                                    if (Rect.intersects(newRect, otherRect)) {
                                        canMove = false
                                        break
                                    }
                                }
                            }

                            if (canMove) {
                                params.horizontalBias = newBiasX
                                params.verticalBias = newBiasY
                                view.layoutParams = params
                                finalBiasX = newBiasX
                                finalBiasY = newBiasY
                            }

                            true
                        }

                        else -> false
                    }
                } else false
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun addScenePopup(){
        // Vytvoření dialogu
        val dialog = Dialog(fragmentContext)
        dialog.setContentView(R.layout.add_scene_to_model_popup)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.7).toInt(),
            (resources.displayMetrics.heightPixels * 0.6).toInt()
        )
        // Reference na prvky v popup layoutu
        val sceneNameInput = dialog.findViewById<TextInputEditText>(R.id.scene_name)
        val addButton = dialog.findViewById<Button>(R.id.add_scene_button)
        addButton.setOnClickListener {
            val sceneName = sceneNameInput.text.toString().trim()
            if (sceneName.isEmpty()){
                Toast.makeText(dialog.context, "You must enter the name first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (sceneName.length > 20) {
                Toast.makeText(dialog.context, "Name cannot exceed 20 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val randomID = UUID.randomUUID().toString()

            val scene = ModelScene(randomID, sceneName, mutableListOf(), mutableListOf())
            editModel.listOfScenes.add(scene)
            selectedStageId = randomID
            drawEditScene()

            val textViewForScene = TextView(fragmentContext).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(5, 5, 10, -5)
                }
                textSize = 28f
                text = sceneName
                setBackgroundColor(Color.WHITE)
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                isSingleLine = true
                ellipsize = TextUtils.TruncateAt.END
                tag = randomID
            }
            textViewForScene.setOnClickListener(
                CustomClickListener(
                    onClick = {
                        selectedStageId = textViewForScene.tag.toString()
                        drawEditScene()
                    },
                    onDoubleClick = {
                        selectedStageId = textViewForScene.tag.toString()
                        sceneOptionsPopup()
                    }
                )
            )
            editModelScenesLayout.addView(textViewForScene)
            drawEditScenesToBar()
            dialog.dismiss()
        }
        dialog.show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addHelperPopup(){
        if (!canAddNewObject(editModelSceneLayout)){
            Toast.makeText(fragmentContext, "You cant add anymore objects", Toast.LENGTH_SHORT).show()
            return
        }
        if (editModel.listOfScenes.isEmpty()){
            Toast.makeText(fragmentContext, "First you need to create scene", Toast.LENGTH_SHORT).show()
        } else {
            val randomId = UUID.randomUUID().toString()
            val helper = HelperShape(
                randomId,
                150,
                150,
                0f,
                0f
            )
            val scene = findSceneById(editModel, selectedStageId!!)
            scene?.listOfHelpers?.add(helper)

            val textView = TextView(fragmentContext).apply {
                textSize = 18f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.BLACK)
                tag = randomId
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {

                    width = helper.width
                    height = helper.height

                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

                    horizontalBias = helper.xPosition
                    verticalBias = helper.yPosition
                }
            }
            textView.setOnClickListener(
                CustomClickListener(
                    onClick = {
                        selectedHelperId = textView.tag.toString()
                        if (!helperEditMode) {
                            helperEditMode = true
                            currentHelperToEdit = textView
                            confirmTableChanges.visibility = View.VISIBLE
                            saveButton.visibility = View.GONE
                            cancelButton.visibility = View.GONE
                            addTableButton.visibility = View.GONE
                            addSceneButton.visibility = View.GONE
                            addHelperButton.visibility = View.GONE
                            editModelScenesLayout.visibility = View.GONE
                        }
                    },
                    onDoubleClick = {
                        selectedHelperId = textView.tag.toString()
                        helperOptionsPopup()
                    }
                )
            )
            editModelSceneLayout.addView(textView)

            textView.setOnTouchListener { view, event ->
                if (helperEditMode && currentHelperToEdit == view) { // Povolit manipulaci pouze pro aktuálně editovaný stůl
                    val params = view.layoutParams as ConstraintLayout.LayoutParams
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // Inicializace proměnných při zahájení dotyku
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            initialBiasX = params.horizontalBias
                            initialBiasY = params.verticalBias
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = event.rawX - initialTouchX
                            val deltaY = event.rawY - initialTouchY

                            val scaleX = deltaX / editModelSceneLayout.width.toFloat()
                            val scaleY = deltaY / editModelSceneLayout.height.toFloat()

                            var newBiasX = initialBiasX + scaleX
                            var newBiasY = initialBiasY + scaleY

                            newBiasX = newBiasX.coerceIn(0f, 1f)
                            newBiasY = newBiasY.coerceIn(0f, 1f)

                            // Vypočítat navrhovanou novou pozici
                            val newLeft = (newBiasX * (editModelSceneLayout.width - view.width)).toInt()
                            val newTop = (newBiasY * (editModelSceneLayout.height - view.height)).toInt()
                            val newRight = newLeft + view.width
                            val newBottom = newTop + view.height
                            val newRect = Rect(newLeft, newTop, newRight, newBottom)

                            // Zkontrolovat kolize
                            var canMove = true
                            for (i in 0 until editModelSceneLayout.childCount) {
                                val otherView = editModelSceneLayout.getChildAt(i)
                                if (otherView != view && otherView is TextView) {
                                    val otherParams = otherView.layoutParams as ConstraintLayout.LayoutParams
                                    val otherLeft = (otherParams.horizontalBias * (editModelSceneLayout.width - otherView.width)).toInt()
                                    val otherTop = (otherParams.verticalBias * (editModelSceneLayout.height - otherView.height)).toInt()
                                    val otherRect = Rect(
                                        otherLeft,
                                        otherTop,
                                        otherLeft + otherView.width,
                                        otherTop + otherView.height
                                    )

                                    if (Rect.intersects(newRect, otherRect)) {
                                        canMove = false
                                        break
                                    }
                                }
                            }

                            if (canMove) {
                                params.horizontalBias = newBiasX
                                params.verticalBias = newBiasY
                                view.layoutParams = params
                                finalBiasX = newBiasX
                                finalBiasY = newBiasY
                            }

                            true
                        }

                        else -> false
                    }
                } else false
            }
        }
    }

    private fun canAddNewObject(layout: ConstraintLayout): Boolean {

        // Celková plocha layoutu
        val layoutWidth = layout.width
        val layoutHeight = layout.height
        val totalLayoutArea = layoutWidth * layoutHeight

        // Celková plocha všech existujících prvků
        var totalOccupiedArea = 0

        // Projděte všechny existující prvky v layoutu
        for (i in 0 until layout.childCount) {
            val child = layout.getChildAt(i)
            if (child is TextView) {
                // Získání rozměrů a pozice prvku
                val width = child.width
                val height = child.height

                // Přidání plochy prvku k celkové ploše
                totalOccupiedArea += width * height
            }
        }

        // Kontrola, zda nový prvek nepřekročí 85 % plochy layoutu
        val totalAreaAfterAdding = totalOccupiedArea + 150*150
        val maxAllowedArea = totalLayoutArea * 0.9

        if (totalAreaAfterAdding > maxAllowedArea) {
            return false
        }

        // Pokud vše projde, vrátí true
        return true
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun drawEditScene() {
        if (editModel.listOfScenes.isNotEmpty()){
        editModelSceneLayout.removeAllViews()
        lateinit var scene: ModelScene
        editModel.listOfScenes.forEach { modelScene ->
            if (modelScene.id == selectedStageId) {
                scene = modelScene
            }
        }
        scene.listOfTables.forEach { table ->
            val textView = TextView(fragmentContext).apply {
                text = table.name
                textSize = 18f
                tag = TableTag(table.id, table.state)
                gravity = Gravity.CENTER
                setBackgroundColor(Color.LTGRAY)

                // Nastavit ConstraintLayout.LayoutParams
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    // Nastavit šířku a výšku
                    width = table.width
                    height = table.height

                    // Připojit k rodičovskému ConstraintLayout ze všech stran
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

                    // Nastavit bias na základě table.xPosition a table.yPosition
                    horizontalBias = table.xPosition
                    verticalBias = table.yPosition
                }
            }
            textView.setOnClickListener(
                CustomClickListener(
                    onClick = {
                        val tableParams = textView.tag as TableTag
                        selectedTableId = tableParams.id
                        val state = tableParams.state
                        if (!state.equals("empty")) {
                            Toast.makeText(
                                requireContext(),
                                "Can´t edit table when in use",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@CustomClickListener
                        }
                        if (!tableEditMode) {
                            tableEditMode = true
                            currentTableToEdit = textView
                            confirmTableChanges.visibility = View.VISIBLE
                            saveButton.visibility = View.GONE
                            cancelButton.visibility = View.GONE
                            addTableButton.visibility = View.GONE
                            addSceneButton.visibility = View.GONE
                            addHelperButton.visibility = View.GONE
                            editModelScenesLayout.visibility = View.GONE
                        }
                    },
                    onDoubleClick = {
                        val tableParams = textView.tag as TableTag
                        selectedTableId = tableParams.id
                        val state = tableParams.state
                        if (!state.equals("empty")) {
                            Toast.makeText(
                                requireContext(),
                                "Can´t edit table when in use",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@CustomClickListener
                        }
                        tableOptionsPopup()
                    }
                )
            )
            textView.setOnTouchListener { view, event ->
                if (tableEditMode && currentTableToEdit == view) { // Povolit manipulaci pouze pro aktuálně editovaný stůl
                    val params = view.layoutParams as ConstraintLayout.LayoutParams
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // Inicializace proměnných při zahájení dotyku
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            initialBiasX = params.horizontalBias
                            initialBiasY = params.verticalBias
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = event.rawX - initialTouchX
                            val deltaY = event.rawY - initialTouchY

                            val scaleX = deltaX / editModelSceneLayout.width.toFloat()
                            val scaleY = deltaY / editModelSceneLayout.height.toFloat()

                            var newBiasX = initialBiasX + scaleX
                            var newBiasY = initialBiasY + scaleY

                            newBiasX = newBiasX.coerceIn(0f, 1f)
                            newBiasY = newBiasY.coerceIn(0f, 1f)

                            // Vypočítat navrhovanou novou pozici
                            val newLeft = (newBiasX * (editModelSceneLayout.width - view.width)).toInt()
                            val newTop = (newBiasY * (editModelSceneLayout.height - view.height)).toInt()
                            val newRight = newLeft + view.width
                            val newBottom = newTop + view.height
                            val newRect = Rect(newLeft, newTop, newRight, newBottom)

                            // Zkontrolovat kolize
                            var canMove = true
                            for (i in 0 until editModelSceneLayout.childCount) {
                                val otherView = editModelSceneLayout.getChildAt(i)
                                if (otherView != view && otherView is TextView) {
                                    val otherParams = otherView.layoutParams as ConstraintLayout.LayoutParams
                                    val otherLeft = (otherParams.horizontalBias * (editModelSceneLayout.width - otherView.width)).toInt()
                                    val otherTop = (otherParams.verticalBias * (editModelSceneLayout.height - otherView.height)).toInt()
                                    val otherRect = Rect(
                                        otherLeft,
                                        otherTop,
                                        otherLeft + otherView.width,
                                        otherTop + otherView.height
                                    )

                                    if (Rect.intersects(newRect, otherRect)) {
                                        canMove = false
                                        break
                                    }
                                }
                            }

                            if (canMove) {
                                params.horizontalBias = newBiasX
                                params.verticalBias = newBiasY
                                view.layoutParams = params
                                finalBiasX = newBiasX
                                finalBiasY = newBiasY
                            }

                            true
                        }

                        else -> false
                    }
                } else false
            }
            editModelSceneLayout.addView(textView)
        }
        scene.listOfHelpers.forEach { helper ->
            val textView = TextView(fragmentContext).apply {
                textSize = 18f
                gravity = Gravity.CENTER
                setBackgroundColor(ContextCompat.getColor(fragmentContext, R.color.background_light_black))
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {

                    width = helper.width
                    height = helper.height

                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

                    horizontalBias = helper.xPosition
                    verticalBias = helper.yPosition
                }
                tag = helper.id
            }
            textView.setOnClickListener(
                CustomClickListener(
                    onClick = {
                        selectedHelperId = textView.tag.toString()
                        if (!helperEditMode) {
                            helperEditMode = true
                            currentHelperToEdit = textView
                            confirmTableChanges.visibility = View.VISIBLE
                            saveButton.visibility = View.GONE
                            cancelButton.visibility = View.GONE
                            addTableButton.visibility = View.GONE
                            addSceneButton.visibility = View.GONE
                            addHelperButton.visibility = View.GONE
                            editModelScenesLayout.visibility = View.GONE
                        }
                    },
                    onDoubleClick = {
                        selectedHelperId = textView.tag.toString()
                        helperOptionsPopup()
                    }
                )
            )
            textView.setOnTouchListener { view, event ->
                if (helperEditMode && currentHelperToEdit == view) { // Povolit manipulaci pouze pro aktuálně editovaný stůl
                    val params = view.layoutParams as ConstraintLayout.LayoutParams
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // Inicializace proměnných při zahájení dotyku
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            initialBiasX = params.horizontalBias
                            initialBiasY = params.verticalBias
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = event.rawX - initialTouchX
                            val deltaY = event.rawY - initialTouchY

                            val scaleX = deltaX / editModelSceneLayout.width.toFloat()
                            val scaleY = deltaY / editModelSceneLayout.height.toFloat()

                            var newBiasX = initialBiasX + scaleX
                            var newBiasY = initialBiasY + scaleY

                            newBiasX = newBiasX.coerceIn(0f, 1f)
                            newBiasY = newBiasY.coerceIn(0f, 1f)

                            // Vypočítat navrhovanou novou pozici
                            val newLeft = (newBiasX * (editModelSceneLayout.width - view.width)).toInt()
                            val newTop = (newBiasY * (editModelSceneLayout.height - view.height)).toInt()
                            val newRight = newLeft + view.width
                            val newBottom = newTop + view.height
                            val newRect = Rect(newLeft, newTop, newRight, newBottom)

                            // Zkontrolovat kolize
                            var canMove = true
                            for (i in 0 until editModelSceneLayout.childCount) {
                                val otherView = editModelSceneLayout.getChildAt(i)
                                if (otherView != view && otherView is TextView) {
                                    val otherParams = otherView.layoutParams as ConstraintLayout.LayoutParams
                                    val otherLeft = (otherParams.horizontalBias * (editModelSceneLayout.width - otherView.width)).toInt()
                                    val otherTop = (otherParams.verticalBias * (editModelSceneLayout.height - otherView.height)).toInt()
                                    val otherRect = Rect(
                                        otherLeft,
                                        otherTop,
                                        otherLeft + otherView.width,
                                        otherTop + otherView.height
                                    )

                                    if (Rect.intersects(newRect, otherRect)) {
                                        canMove = false
                                        break
                                    }
                                }
                            }

                            if (canMove) {
                                params.horizontalBias = newBiasX
                                params.verticalBias = newBiasY
                                view.layoutParams = params
                                finalBiasX = newBiasX
                                finalBiasY = newBiasY
                            }

                            true
                        }

                        else -> false
                    }
                } else false
            }
            editModelSceneLayout.addView(textView)
        }
    }
    }

    private fun drawEditScenesToBar(){
        editModelScenesLayout.removeAllViews()
        if (editModel.listOfScenes.isEmpty()){
            val textView = TextView(fragmentContext).apply {
                this.text = "No Scene"
                this.textSize = 18f // Velikost textu
                this.setTextColor(Color.WHITE) // Barva textu
            }
            editModelScenesLayout.addView(textView)
        } else {
            editModel.listOfScenes.forEach { scene ->
                val name = scene.name
                val id = scene.id
                val textViewForScene = TextView(fragmentContext).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(15, 15, 15, 15)
                    }
                    textSize = 20f
                    text = name
                    val backgroundDrawable = GradientDrawable().apply {
                        setColor(Color.WHITE)
                        cornerRadius = 30f
                    }
                    background = backgroundDrawable
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER
                    isSingleLine = true
                    setPadding(10,10,10,10)
                    tag = id
                }
                textViewForScene.setOnClickListener(
                    CustomClickListener(
                        onClick = {
                            selectedStageId = textViewForScene.tag.toString()
                            setSelectedScene(editModelScenesLayout, textViewForScene)
                            drawEditScene()
                        },
                        onDoubleClick = {
                            selectedStageId = textViewForScene.tag.toString()
                            sceneOptionsPopup()
                        }
                    )
                )
                editModelScenesLayout.addView(textViewForScene)
            }
            if (selectedStageId!!.isNotEmpty()) {
                val selectedTextView = editModelScenesLayout.findViewWithTag<TextView>(selectedStageId)
                selectedTextView?.let {
                    setSelectedScene(editModelScenesLayout, it)
                }
            }
        }
    }

    private fun sceneOptionsPopup(){
        val scene = findSceneById(editModel, selectedStageId!!)
        val sceneName = scene?.name

        // Nastavení velikosti dialogu
        sceneOptionsDialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            (resources.displayMetrics.heightPixels * 0.9).toInt()
        )

        // Reference na prvky v popup layoutu
        nameOfTheScene.text = sceneName
        val saveButton = sceneOptionsDialog.findViewById<Button>(R.id.save_scene_options_button)
        saveButton.setOnClickListener {
            if (newSceneName == null && sceneName != null){
                scene.name = sceneName
            }
            if (newSceneName != null){
                scene?.name = newSceneName.toString()
            }
            drawEditScenesToBar()
            newSceneName = null
            sceneOptionsDialog.dismiss()
        }
        val cancelButton = sceneOptionsDialog.findViewById<Button>(R.id.cancel_scene_options_button)
        cancelButton.setOnClickListener {
            newSceneName = null
            sceneOptionsDialog.dismiss()
        }
        val deleteButton = sceneOptionsDialog.findViewById<Button>(R.id.delete_scene_options_button)
        deleteButton.setOnClickListener {
            val builder = AlertDialog.Builder(fragmentContext)
            builder.setTitle("Delete this scene")
            builder.setMessage("Are you sure you want to delete this scene?")

            builder.setPositiveButton("Yes") { dialog, _ ->
                newSceneName = null
                editModel.deleteScene(selectedStageId!!)

                val viewToRemove = findViewWithTag(editModelScenesLayout, selectedStageId!!)
                val layout = viewToRemove?.parent
                if (layout is LinearLayout){
                    layout.removeView(viewToRemove)
                }
                //prepne na jinou scenu
                if (editModel.listOfScenes.isNotEmpty()){
                    selectedStageId = editModel.listOfScenes.get(0).id
                    drawEditScenesToBar()
                    drawEditScene()
                } else {
                    val textView = TextView(fragmentContext).apply {
                        this.text = "No Scene"
                        this.textSize = 18f // Velikost textu
                        this.setTextColor(Color.LTGRAY) // Barva textu

                    }
                    val params = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )

                    editModelSceneLayout.addView(textView, params)

                    textView.post {
                        val parentWidth = editModelSceneLayout.width
                        val parentHeight = editModelSceneLayout.height
                        val textViewWidth = textView.width
                        val textViewHeight = textView.height
                        val leftMargin = (parentWidth - textViewWidth) / 2
                        val topMargin = (parentHeight - textViewHeight) / 2

                        params.leftMargin = leftMargin
                        params.topMargin = topMargin

                        textView.layoutParams = params
                    }
                }
                dialog.dismiss()
                sceneOptionsDialog.dismiss()
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            val alertDialog = builder.create()
            alertDialog.show()
        }
        val changeNameButton = sceneOptionsDialog.findViewById<Button>(R.id.change_scene_name)
        changeNameButton.setOnClickListener {
            changeSceneName()
        }
        sceneOptionsDialog.show()
    }

    private fun changeSceneName(){
        // Vytvoření dialogu
        val dialog = Dialog(fragmentContext)
        dialog.setContentView(R.layout.change_parameters_for_menu_elements)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
        // Reference na prvky v popup layoutu
        val textView = dialog.findViewById<TextView>(R.id.parametr_view)
        textView.text = "Scene name"
        val parametrToChange = dialog.findViewById<TextInputEditText>(R.id.parameter_to_change)
        parametrToChange.hint = "New Scene name"
        val closeButton = dialog.findViewById<Button>(R.id.close_change_parameters_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        val changeTextButton = dialog.findViewById<Button>(R.id.change_group_name_button)
        changeTextButton.text = "Change name"
        changeTextButton.setOnClickListener {
            val newName = parametrToChange.text.toString().trim()
            if (newName.isEmpty()){
                Toast.makeText(dialog.context, "You must enter the name first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newName.length > 20) {
                Toast.makeText(dialog.context, "Name cannot exceed 20 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            newSceneName = newName
            nameOfTheScene.text = newSceneName
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun tableOptionsPopup(){
        val table = findTableById(editModel, selectedTableId!!)
        val scene = findSceneById(editModel, selectedStageId!!)
        val tableName = table?.name
        val tableHeight = table?.height
        val tableWidth = table?.width


        // Nastavení velikosti dialogu
        tableOptionsDialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            (resources.displayMetrics.heightPixels * 0.9).toInt()
        )

        // Reference na prvky v popup layoutu
        nameOfTheTable.text = tableName
        heightOfTheTable.text = tableHeight.toString()
        widthOfTheTable.text = tableWidth.toString()
        val saveButton = tableOptionsDialog.findViewById<Button>(R.id.save_table_options_button)
        saveButton.setOnClickListener {
            if (newTableName == null && tableName != null){
                table.name = tableName
            }
            if (newTableName != null){
                table?.name = newTableName.toString()
            }
            if (newTableHeight == null && tableHeight != null){
                table.height = tableHeight
            }
            if (newTableHeight != null){
                table?.height = newTableHeight!!.toInt()
            }
            if (newTableWidth == null && tableWidth != null){
                table.width = tableWidth
            }
            if (newTableWidth != null){
                table?.width = newTableWidth!!.toInt()
            }
            drawEditScene()
            newTableName = null
            newTableHeight = null
            newTableWidth = null
            tableOptionsDialog.dismiss()
        }
        val cancelButton = tableOptionsDialog.findViewById<Button>(R.id.cancel_table_options_button)
        cancelButton.setOnClickListener {
            newTableName = null
            newTableHeight = null
            newTableWidth = null
            tableOptionsDialog.dismiss()
        }
        val deleteButton = tableOptionsDialog.findViewById<Button>(R.id.delete_table_options_button)
        deleteButton.setOnClickListener {
            val builder = AlertDialog.Builder(fragmentContext)
            builder.setTitle("Delete this table")
            builder.setMessage("Are you sure you want to delete this table?")

            builder.setPositiveButton("Yes") { dialog, _ ->
                newTableName = null
                newTableHeight = null
                newTableWidth = null
                scene?.deleteTable(selectedTableId!!)

                val viewToRemove = findViewWithTag(editModelSceneLayout, selectedTableId!!)
                val layout = viewToRemove?.parent
                if (layout is ConstraintLayout){
                    layout.removeView(viewToRemove)
                }
                dialog.dismiss()
                tableOptionsDialog.dismiss()
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            val alertDialog = builder.create()
            alertDialog.show()
        }
        val changeNameButton = tableOptionsDialog.findViewById<Button>(R.id.change_table_name)
        changeNameButton.setOnClickListener {
            changeTableName()
        }
        val changeHeightButton = tableOptionsDialog.findViewById<Button>(R.id.change_table_height)
        changeHeightButton.setOnClickListener {
            changeTableHeight()
        }
        val changeWidthButton = tableOptionsDialog.findViewById<Button>(R.id.change_table_width)
        changeWidthButton.setOnClickListener {
            changeTableWidth()
        }
        tableOptionsDialog.show()
    }

    private fun changeTableName(){
        // Vytvoření dialogu
        val dialog = Dialog(fragmentContext)
        dialog.setContentView(R.layout.change_parameters_for_menu_elements)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
        // Reference na prvky v popup layoutu
        val textView = dialog.findViewById<TextView>(R.id.parametr_view)
        textView.text = "Table name"
        val parametrToChange = dialog.findViewById<TextInputEditText>(R.id.parameter_to_change)
        parametrToChange.hint = "New Table name"
        val closeButton = dialog.findViewById<Button>(R.id.close_change_parameters_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        val changeTextButton = dialog.findViewById<Button>(R.id.change_group_name_button)
        changeTextButton.text = "Change name"
        changeTextButton.setOnClickListener {
            val newName = parametrToChange.text.toString().trim()
            if (newName.isEmpty()){
                Toast.makeText(dialog.context, "You must enter the name first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newName.length > 20) {
                Toast.makeText(dialog.context, "Name cannot exceed 20 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            newTableName = newName
            nameOfTheTable.text = newTableName
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun changeTableHeight(){
        // Vytvoření dialogu
        val dialog = Dialog(fragmentContext)
        dialog.setContentView(R.layout.change_parameters_for_menu_elements)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
        // Reference na prvky v popup layoutu
        val textView = dialog.findViewById<TextView>(R.id.parametr_view)
        textView.text = "Table height"
        val parametrToChange = dialog.findViewById<TextInputEditText>(R.id.parameter_to_change)
        parametrToChange.hint = "New table height"
        val closeButton = dialog.findViewById<Button>(R.id.close_change_parameters_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        val changeTextButton = dialog.findViewById<Button>(R.id.change_group_name_button)
        changeTextButton.text = "Change height"
        changeTextButton.setOnClickListener {
            val newHeight = parametrToChange.text.toString().trim()
            if (newHeight.isEmpty()){
                Toast.makeText(dialog.context, "You must enter the height first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val tableHeight = try {
                val height = newHeight.toInt()
                if (height in 100..500) {
                    height
                } else {
                    Toast.makeText(dialog.context, "Height must be between 100 and 500!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(dialog.context, "Enter valid height!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val isPositionOk = validateTablePosition("height", newHeight.toInt())
            if (!isPositionOk){
                Toast.makeText(dialog.context, "New height cannot be applied!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            newTableHeight = tableHeight
            heightOfTheTable.text = newTableHeight.toString()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun changeTableWidth(){
        // Vytvoření dialogu
        val dialog = Dialog(fragmentContext)
        dialog.setContentView(R.layout.change_parameters_for_menu_elements)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
        // Reference na prvky v popup layoutu
        val textView = dialog.findViewById<TextView>(R.id.parametr_view)
        textView.text = "Table width"
        val parametrToChange = dialog.findViewById<TextInputEditText>(R.id.parameter_to_change)
        parametrToChange.hint = "New table width"
        val closeButton = dialog.findViewById<Button>(R.id.close_change_parameters_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        val changeTextButton = dialog.findViewById<Button>(R.id.change_group_name_button)
        changeTextButton.text = "Change width"
        changeTextButton.setOnClickListener {
            val newWidth = parametrToChange.text.toString().trim()
            if (newWidth.isEmpty()){
                Toast.makeText(dialog.context, "You must enter the width first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val tableWidth = try {
                val height = newWidth.toInt()
                if (height in 100..500) {
                    height
                } else {
                    Toast.makeText(dialog.context, "Width must be between 100 and 500!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(dialog.context, "Enter valid width!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val isPositionOk = validateTablePosition("width", newWidth.toInt())
            if (!isPositionOk){
                Toast.makeText(dialog.context, "New width cannot be applied!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            newTableWidth = tableWidth
            widthOfTheTable.text = newTableWidth.toString()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun helperOptionsPopup(){
        val helper = findHelperById(editModel, selectedHelperId!!)
        val scene = findSceneById(editModel, selectedStageId!!)
        val helperHeight = helper?.height
        val helperWidth = helper?.width

        // Nastavení velikosti dialogu
        helperOptionsDialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            (resources.displayMetrics.heightPixels * 0.9).toInt()
        )

        // Reference na prvky v popup layoutu
        heightOfTheHelper.text = helperHeight.toString()
        widthOfTheHelper.text = helperWidth.toString()
        val saveButton = helperOptionsDialog.findViewById<Button>(R.id.save_helper_options_button)
        saveButton.setOnClickListener {
            if (newHelperHeight == null && helperHeight != null) {
                helper.height = helperHeight
            }
            if (newHelperHeight != null) {
                helper?.height = newHelperHeight!!.toInt()
            }
            if (newHelperWidth == null && helperWidth != null) {
                helper.width = helperWidth
            }
            if (newHelperWidth != null) {
                helper?.width = newHelperWidth!!.toInt()
            }
            drawEditScene()
            newHelperHeight = null
            newHelperWidth= null
            helperOptionsDialog.dismiss()
        }
        val cancelButton = helperOptionsDialog.findViewById<Button>(R.id.cancel_helper_options_button)
        cancelButton.setOnClickListener {
            newHelperHeight = null
            newHelperWidth= null
            helperOptionsDialog.dismiss()
        }
        val deleteButton = helperOptionsDialog.findViewById<Button>(R.id.delete_helper_options_button)
        deleteButton.setOnClickListener {
            val builder = AlertDialog.Builder(fragmentContext)
            builder.setTitle("Cancel changes")
            builder.setMessage("Are you sure you want to cancel all the changes?")

            builder.setPositiveButton("Yes") { dialog, _ ->
                newHelperHeight = null
                newHelperWidth = null
                scene?.deleteHelper(selectedHelperId!!)

                val viewToRemove = findViewWithTag(editModelSceneLayout, selectedHelperId!!)
                val layout = viewToRemove?.parent
                if (layout is FrameLayout){
                    layout.removeView(viewToRemove)
                }
                dialog.dismiss()
                helperOptionsDialog.dismiss()
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }

            val alertDialog = builder.create()
            alertDialog.show()
        }
        val changeHeightButton = helperOptionsDialog.findViewById<Button>(R.id.change_helper_height)
        changeHeightButton.setOnClickListener {
            changeHelperHeight()
        }
        val changeWidthButton = helperOptionsDialog.findViewById<Button>(R.id.change_helper_width)
        changeWidthButton.setOnClickListener {
            changeHelperWidth()
        }
        helperOptionsDialog.show()
    }

    private fun changeHelperHeight(){
        // Vytvoření dialogu
        val dialog = Dialog(fragmentContext)
        dialog.setContentView(R.layout.change_parameters_for_menu_elements)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
        // Reference na prvky v popup layoutu
        val textView = dialog.findViewById<TextView>(R.id.parametr_view)
        textView.text = "Helper height"
        val parametrToChange = dialog.findViewById<TextInputEditText>(R.id.parameter_to_change)
        parametrToChange.hint = "New helper height"
        val closeButton = dialog.findViewById<Button>(R.id.close_change_parameters_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        val changeTextButton = dialog.findViewById<Button>(R.id.change_group_name_button)
        changeTextButton.text = "Change height"
        changeTextButton.setOnClickListener {
            val newHeight = parametrToChange.text.toString().trim()
            if (newHeight.isEmpty()){
                Toast.makeText(dialog.context, "You must enter the height first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val helperHeight = try {
                val height = newHeight.toInt()
                if (height in 100..500) {
                    height
                } else {
                    Toast.makeText(dialog.context, "Height must be between 100 and 500!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(dialog.context, "Enter valid height!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val isPositionOk = validateHelperPosition("height", newHeight.toInt())
            if (!isPositionOk){
                Toast.makeText(dialog.context, "New height cannot be applied!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            newHelperHeight = helperHeight
            heightOfTheHelper.text = newHelperHeight.toString()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun changeHelperWidth(){
        // Vytvoření dialogu
        val dialog = Dialog(fragmentContext)
        dialog.setContentView(R.layout.change_parameters_for_menu_elements)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
        // Reference na prvky v popup layoutu
        val textView = dialog.findViewById<TextView>(R.id.parametr_view)
        textView.text = "Helper width"
        val parametrToChange = dialog.findViewById<TextInputEditText>(R.id.parameter_to_change)
        parametrToChange.hint = "New helper width"
        val closeButton = dialog.findViewById<Button>(R.id.close_change_parameters_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        val changeTextButton = dialog.findViewById<Button>(R.id.change_group_name_button)
        changeTextButton.text = "Change width"
        changeTextButton.setOnClickListener {
            val newWidth = parametrToChange.text.toString().trim()
            if (newWidth.isEmpty()){
                Toast.makeText(dialog.context, "You must enter the width first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val helperWidth = try {
                val height = newWidth.toInt()
                if (height in 100..500) {
                    height
                } else {
                    Toast.makeText(dialog.context, "Width must be between 100 and 500!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(dialog.context, "Enter valid width!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val isPositionOk = validateHelperPosition("width", newWidth.toInt())
            if (!isPositionOk){
                Toast.makeText(dialog.context, "New width cannot be applied!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            newHelperWidth = helperWidth
            widthOfTheHelper.text = newHelperWidth.toString()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun validateTablePosition(whichPar: String ,parameter: Int): Boolean {

        // Najdi aktuální prvek podle jeho ID
        val selectedTable = findTableById(editModel, selectedTableId!!) ?: return false

        // Získej parametry aktuálního prvku
        val selectedBiasX = selectedTable.xPosition
        val selectedBiasY = selectedTable.yPosition

        val selectedLeft = (selectedBiasX * editModelSceneLayout.width).toInt()
        val selectedTop = (selectedBiasY * editModelSceneLayout.height).toInt()
        var selectedRight = 0
        var selectedBottom = 0

        if (whichPar.equals("width")){
            selectedRight = selectedLeft + parameter
            selectedBottom = selectedTop + selectedTable.height
        }
        if (whichPar.equals("height")){
            selectedRight = selectedLeft + selectedTable.width
            selectedBottom = selectedTop + parameter
        }

        // Zkontroluj, jestli prvek nevyčnívá z layoutu
        if (selectedLeft < 0 || selectedTop < 0 ||
            selectedRight > editModelSceneLayout.width ||
            selectedBottom > editModelSceneLayout.height
        ) {
            return false // Prvek vyčnívá z layoutu
        }

        // Projdi všechny ostatní prvky a zkontroluj překryvy
        for (child in editModelSceneLayout.children) {
            if (child is TextView) {
                when (child.tag) {
                    is TableTag -> {
                        val tableTag = child.tag as TableTag
                        val otherTable = findTableById(editModel, tableTag.id) ?: continue
                        if (otherTable.id == selectedTableId) continue

                        // Získej parametry ostatního prvku
                        val otherBiasX = otherTable.xPosition
                        val otherBiasY = otherTable.yPosition
                        val otherLeft = (otherBiasX * editModelSceneLayout.width).toInt()
                        val otherTop = (otherBiasY * editModelSceneLayout.height).toInt()
                        val otherRight = otherLeft + otherTable.width
                        val otherBottom = otherTop + otherTable.height

                        // Zkontroluj překryv mezi prvky
                        val isOverlapping = selectedLeft < otherRight &&
                                selectedRight > otherLeft &&
                                selectedTop < otherBottom &&
                                selectedBottom > otherTop

                        if (isOverlapping) {
                            return false // Prvky se překrývají
                        }
                    }
                    is String -> {
                        // Je to pomocný prvek
                        val helperId = child.tag as String
                        val otherHelper = findHelperById(editModel, helperId) ?: continue
                        if (otherHelper.id == selectedHelperId) continue

                        // Získej parametry ostatního prvku
                        val otherBiasX = otherHelper.xPosition
                        val otherBiasY = otherHelper.yPosition
                        val otherLeft = (otherBiasX * editModelSceneLayout.width).toInt()
                        val otherTop = (otherBiasY * editModelSceneLayout.height).toInt()
                        val otherRight = otherLeft + otherHelper.width
                        val otherBottom = otherTop + otherHelper.height

                        // Zkontroluj překryv mezi prvky
                        val isOverlapping = !(selectedRight <= otherLeft || // Není vlevo
                                selectedLeft >= otherRight || // Není vpravo
                                selectedBottom <= otherTop || // Není nahoře
                                selectedTop >= otherBottom)   // Není dole

                        if (isOverlapping) {
                            return false // Prvky se překrývají
                        }
                    }
                    else -> {
                        // Neznámý typ (můžeš hodit výjimku nebo ignorovat)
                        continue
                    }
                }
            }
        }
        // Vše je v pořádku
        return true
    }
    private fun validateHelperPosition(whichPar: String ,parameter: Int): Boolean {

        // Najdi aktuální prvek podle jeho ID
        val selectedHelper = findHelperById(editModel, selectedHelperId!!) ?: return false

        // Získej parametry aktuálního prvku
        val selectedBiasX = selectedHelper.xPosition
        val selectedBiasY = selectedHelper.yPosition
        val selectedLeft = (selectedBiasX * editModelSceneLayout.width).toInt()
        val selectedTop = (selectedBiasY * editModelSceneLayout.height).toInt()
        var selectedRight = 0
        var selectedBottom = 0
        if (whichPar.equals("width")){
            selectedRight = selectedLeft + parameter
            selectedBottom = selectedTop + selectedHelper.height
        }
        if (whichPar.equals("height")){
            selectedRight = selectedLeft + selectedHelper.width
            selectedBottom = selectedTop + parameter
        }

        // Zkontroluj, jestli prvek nevyčnívá z layoutu
        if (selectedLeft < 0 || selectedTop < 0 ||
            selectedRight > editModelSceneLayout.width ||
            selectedBottom > editModelSceneLayout.height
        ) {
            return false // Prvek vyčnívá z layoutu
        }

        // Projdi všechny ostatní prvky a zkontroluj překryvy
        for (child in editModelSceneLayout.children) {
            if (child is TextView) {
                when (child.tag) {
                    is TableTag -> {
                        // Je to tabulka
                        val tableTag = child.tag as TableTag
                        val otherTable = findTableById(editModel, tableTag.id.toString()) ?: continue
                        if (otherTable.id == selectedTableId) continue

                        // Získej parametry ostatního prvku
                        val otherBiasX = otherTable.xPosition
                        val otherBiasY = otherTable.yPosition
                        val otherLeft = (otherBiasX * editModelSceneLayout.width).toInt()
                        val otherTop = (otherBiasY * editModelSceneLayout.height).toInt()
                        val otherRight = otherLeft + otherTable.width
                        val otherBottom = otherTop + otherTable.height

                        // Zkontroluj překryv mezi prvky
                        val isOverlapping = selectedLeft < otherRight &&
                                selectedRight > otherLeft &&
                                selectedTop < otherBottom &&
                                selectedBottom > otherTop

                        if (isOverlapping) {
                            return false // Prvky se překrývají
                        }
                    }
                    is String -> {
                        // Je to pomocný prvek
                        val helperId = child.tag as String
                        val otherHelper = findHelperById(editModel, helperId) ?: continue
                        if (otherHelper.id == selectedHelperId) continue

                        // Získej parametry ostatního prvku
                        val otherBiasX = otherHelper.xPosition
                        val otherBiasY = otherHelper.yPosition
                        val otherLeft = (otherBiasX * editModelSceneLayout.width).toInt()
                        val otherTop = (otherBiasY * editModelSceneLayout.height).toInt()
                        val otherRight = otherLeft + otherHelper.width
                        val otherBottom = otherTop + otherHelper.height

                        // Zkontroluj překryv mezi prvky
                        val isOverlapping = !(selectedRight <= otherLeft || // Není vlevo
                                selectedLeft >= otherRight || // Není vpravo
                                selectedBottom <= otherTop || // Není nahoře
                                selectedTop >= otherBottom)   // Není dole

                        if (isOverlapping) {
                            return false // Prvky se překrývají
                        }
                    }
                    else -> {
                        // Neznámý typ (můžeš hodit výjimku nebo ignorovat)
                        continue
                    }
                }
            }
        }
        // Vše je v pořádku
        return true
    }

    private fun updateModel(onComplete: () -> Unit){
        val companyModelRef = CompanyID?.let {
            db.child("companies").child(it).child("Model")
        }
        companyModelRef
            ?.updateChildren(model.toMap())
            ?.addOnSuccessListener {
                updateAnalytics()
                onComplete()
            }
            ?.addOnFailureListener {
                Toast.makeText(
                    context,
                    "Failed to save the changes!",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateAnalytics(){
        val analyticsTablesList = mutableListOf<String>()

        val tablesRef = CompanyID?.let { db.child("companies").child(it).child("Analytics").child("tables") }
        tablesRef?.get()?.addOnSuccessListener { snapshot ->
            analyticsTablesList.clear() // Vyčištění listu

            for (itemSnapshot in snapshot.children) {
                val itemId = itemSnapshot.key // Každý klíč je ID položky
                if (itemId != null) {
                    analyticsTablesList.add(itemId)
                }
            }
            Log.d("Analytics", "Načtené ID položek: $analyticsTablesList")

            val finalAnalyticsTablesList = mutableListOf<String>()
            analyticsTableTraversal(model, analyticsTablesList, finalAnalyticsTablesList)

            analyticsTablesList.forEach { table ->
                tablesRef.child(table).removeValue()
            }
            finalAnalyticsTablesList.forEach { table ->
                val tableMap = mapOf(
                    table to mapOf(
                        "numberOfServedTimes" to 0
                    )
                )
                tablesRef.updateChildren(tableMap)
            }

        }?.addOnFailureListener { error ->
            Log.e("Firebase", "Chyba při načítání Analytics: ${error.message}")
        }
    }

    private fun analyticsTableTraversal(model: Model, list: MutableList<String>, finalList: MutableList<String>){
        model.listOfScenes.forEach { modelScene ->
            modelScene.listOfTables.forEach { table ->
                if (list.contains(table.id)){
                    list.remove(table.id)
                } else {
                    finalList.add(table.id)
                }
            }
        }
    }

    private fun fetchModel(onComplete: () -> Unit) {
        val companyModelRef = CompanyID?.let {
            db.child("companies").child(it).child("Model")
        } ?: return

        companyModelRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val fetchedModel = snapshot.getValue(Model::class.java)
                    if (fetchedModel != null) {
                        model.listOfScenes = fetchedModel.listOfScenes
                        editModel.listOfScenes = fetchedModel.listOfScenes
                        model.locked = fetchedModel.locked
                        setListenerForTableState()
                        updateModelUI()
                        updateEditModelUI()
                        onComplete()
                    } else {
                        Toast.makeText(context, "Failed to parse model data.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Model not found.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error fetching model: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateEditModelUI(){
        drawEditScene()
        drawEditScenesToBar()
    }

    private fun updateModelUI(){
        drawScene()
        drawScenesToBar()
    }
    private fun drawScene() {
        if (model.listOfScenes.isNotEmpty()){
        if (selectedStageId == null) {
            selectedStageId = model.listOfScenes.get(0).id
        }
        currentScene.removeAllViews()
        lateinit var scene: ModelScene
        model.listOfScenes.forEach { modelScene ->
            if (modelScene.id == selectedStageId) {
                scene = modelScene
            }
        }
        scene.listOfTables.forEach { table ->
            val textView = TextView(fragmentContext).apply {
                text = table.name
                textSize = 18f
                tag = TableTag(table.id, table.state)
                gravity = Gravity.CENTER
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    // Nastavit šířku a výšku
                    width = table.width
                    height = table.height

                    // Připojit k rodičovskému ConstraintLayout ze všech stran
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

                    // Nastavit bias na základě table.xPosition a table.yPosition
                    horizontalBias = table.xPosition
                    verticalBias = table.yPosition
                }
            }
            setColorForState(table, textView)
            textView.setOnClickListener(
                CustomClickListener(
                    onClick = {
                        if (model.locked == null) {
                            val tableParams = textView.tag as TableTag
                            selectedTableId = tableParams.id
                            tableManager(tableParams.state)
                        } else {
                            Toast.makeText(
                                context,
                                "Model is currently being edited! Please wait",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onDoubleClick = {

                    }
                )
            )
            currentScene.addView(textView)
        }
        scene.listOfHelpers.forEach { helper ->
            val textView = TextView(fragmentContext).apply {
                textSize = 18f
                gravity = Gravity.CENTER
                setBackgroundColor(ContextCompat.getColor(fragmentContext, R.color.background_light_black))
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    width = helper.width
                    height = helper.height

                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

                    horizontalBias = helper.xPosition
                    verticalBias = helper.yPosition
                }
            }
            currentScene.addView(textView)
        }
    }
    }

    private fun tableManager(state: String){
        when (state) {
            "empty" -> manageEmptyTablePopup()
            "seated" -> manageSeatedTablePopup()
            "ordered" -> manageSeatedTablePopup()
            "eating" -> manageSeatedTablePopup()
            "paid" -> managePaidTablePopup()
            else -> return
        }
    }

    private fun setColorForState(table: Table, textView: TextView){
        val color = when (table.state) {
            "empty" -> Color.LTGRAY
            "seated" -> Color.YELLOW
            "ordered" -> Color.parseColor("#FFA500")
            "eating" -> Color.GREEN
            "paid" -> Color.BLUE
            else -> Color.WHITE
        }
        textView.setBackgroundColor(color)
    }

    private fun drawScenesToBar(){
        modelScenesBar.removeAllViews()
        if (model.listOfScenes.isEmpty()){
            val textView = TextView(fragmentContext).apply {
                this.text = "No Scene"
                this.textSize = 18f // Velikost textu
                this.setTextColor(Color.WHITE) // Barva textu
            }
            modelScenesBar.addView(textView)
        } else {
            model.listOfScenes.forEach { scene ->
                val name = scene.name
                val id = scene.id
                val textViewForScene = TextView(fragmentContext).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(15, 15, 15, 15)
                    }
                    textSize = 20f
                    text = name
                    val backgroundDrawable = GradientDrawable().apply {
                        setColor(Color.WHITE)
                        cornerRadius = 30f
                    }
                    background = backgroundDrawable
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER
                    isSingleLine = true
                    setPadding(10,10,10,10)
                    tag = id
                }
                textViewForScene.setOnClickListener(
                    CustomClickListener(
                        onClick = {
                            setSelectedScene(modelScenesBar, textViewForScene)
                            selectedStageId = textViewForScene.tag.toString()
                            drawScene()
                        },
                        onDoubleClick = {
                        }
                    )
                )
                modelScenesBar.addView(textViewForScene)
            }
            if (selectedStageId!!.isNotEmpty()) {
                val selectedTextView = modelScenesBar.findViewWithTag<TextView>(selectedStageId)
                selectedTextView?.let {
                    setSelectedScene(modelScenesBar ,it)
                }
            }
        }
    }

    private fun setSelectedScene(whichLayout: LinearLayout, selectedTextView: TextView) {
        for (i in 0 until whichLayout.childCount) {
            val child = whichLayout.getChildAt(i)
            if (child is TextView) {
                if (child == selectedTextView) {
                    val backgroundDrawable = GradientDrawable().apply {
                        setColor(Color.GREEN)
                        cornerRadius = 30f
                    }
                    child.background = backgroundDrawable
                } else {
                    val backgroundDrawable = GradientDrawable().apply {
                        setColor(Color.WHITE)
                        cornerRadius = 30f
                    }
                    child.background = backgroundDrawable
                }
            }
        }
    }

    private fun fetchAllMenuItems(onComplete: () -> Unit){
        val companyMenuRef = CompanyID?.let {
            db.child("companies").child(it).child("Menu")
        }

        companyMenuRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Deserialize snapshot into MenuGroup object
                    val fetchedMenu = snapshot.getValue(MenuGroup::class.java)
                    if (fetchedMenu != null) {
                        // Assign to local variable or state
                        val menu = fetchedMenu
                        menu.items.forEach { menuItem ->
                            allMenuItems.add(menuItem)
                        }
                        menu.subGroups.forEach { subGroup ->
                            recursiveMenuBrowse(subGroup)
                        }
                        onComplete()
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
    }

    private fun recursiveMenuBrowse(menuGroup: MenuGroup){
        menuGroup.items.forEach { menuItem ->
            allMenuItems.add(menuItem)
        }
        menuGroup.subGroups.forEach { subGroup ->
            recursiveMenuBrowse(subGroup)
        }
    }

    private fun findTableById(model: Model, id: String): Table?{
        val scene = findSceneById(model, selectedStageId!!)
        scene?.listOfTables?.forEach { table ->
            if (table.id == id) return table
        }
        return null
    }
    private fun findHelperById(model: Model,id: String): HelperShape?{
        val scene = findSceneById(model, selectedStageId!!)
        scene?.listOfHelpers?.forEach { helper ->
            if (helper.id == id) return helper
        }
        return null
    }

    private fun findSceneById(model: Model, id: String): ModelScene? {
        model.listOfScenes.forEach { scene ->
            if (scene.id == id){
                return scene
            }
        }
        return null
    }

    private fun findViewWithTag(root: ViewGroup, tag: String): View? {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)

            if (child.tag == tag) {
                return child
            }
        }
        return null
    }

    // metoda ktera zkotroluje jestli jsou vytvorene sceny pro zobrazeni
    private fun checkIfSceneExists(){
        if (model.listOfScenes.isEmpty()){
            currentScene.removeAllViews()
            currentScene.addView(dynamicLinearLayout)
        }
    }

    private fun setListenerForTableState(){
        val companyId = CompanyID ?: return

        model.listOfScenes.forEachIndexed { sceneIndex, modelScene ->
            modelScene.listOfTables.forEachIndexed { tableIndex, modelTable ->
                val tableId = modelTable.id
                val tableName = modelTable.name
                val tableStateRef = db.child("companies").child(companyId).child("Model")
                    .child("listOfScenes").child(sceneIndex.toString()).child("listOfTables")
                    .child(tableIndex.toString()).child("state")

                val existingListener = tableStateListeners[tableId]
                if (existingListener != null) {
                    tableStateRef.removeEventListener(existingListener)
                    tableStateListeners.remove(tableId)
                }

                val newListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val newState = snapshot.getValue(String::class.java) ?: return

                        val notificationsRef = db.child("companies").child(companyId).child("Notifications")

                        if (newState !in listOf("seated", "eating", "paid")){
                            notificationsRef.orderByChild("tableId").equalTo(tableId).get().addOnSuccessListener { data ->
                                data.children.forEach { it.ref.removeValue() }
                                return@addOnSuccessListener
                            }
                            return
                        }

                        notificationsRef.orderByChild("tableId").equalTo(tableId).get().addOnSuccessListener { data ->
                            data.children.forEach { it.ref.removeValue() }
                        }

                        val period: Int
                        when(newState){
                            "seated" -> period = seatedTableNotificationPeriod
                            "eating" -> period = eatingTableNotificationPeriod
                            "paid" -> period = paidTableNotificationPeriod
                            else -> period = 5
                        }

                        val newNotificationRef = notificationsRef.push()
                        val notification = Notification(
                            id = newNotificationRef.key!!,
                            tableId = tableId,
                            tableName = tableName,
                            type = newState,
                            timeToSend = System.currentTimeMillis() + (period * 60 * 1000), //prvni rika kolik minut
                            send = false
                        )
                        newNotificationRef.setValue(notification)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle errors appropriately, e.g., log the error
                        println("Error listening for table state changes: ${error.message}")
                    }
                }
                tableStateRef.addValueEventListener(newListener)
                tableStateListeners[tableId] = newListener
            }
        }
    }

    private fun removeTableStateListeners() {
        val companyId = CompanyID ?: return

        tableStateListeners.forEach { (tableId, listener) ->
            val indices = findTableIndices(model, tableId)
            if (indices != null){
                val (sceneIndex, tableIndex) = indices
                val tableStateRef = db.child("companies").child(companyId).child("Model")
                    .child("listOfScenes").child(sceneIndex.toString())
                    .child("listOfTables").child(tableIndex.toString())
                    .child("state")
                tableStateRef.removeEventListener(listener)
            }
        }
        tableStateListeners.clear()
    }

    private fun findTableIndices(model: Model, tableId: String): Pair<Int, Int>? {
        model.listOfScenes.forEachIndexed { sceneIndex, scene ->
            scene.listOfTables.forEachIndexed { tableIndex, table ->
                if (table.id == tableId) {
                    return Pair(sceneIndex, tableIndex)
                }
            }
        }
        return null // Return null if the tableId is not found
    }

    //listener pro zmeny v settings
    private fun listenForSettingsChanges() {
        val databaseRef = CompanyID?.let { db.child("companies").child(it).child("settings") }

        databaseRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                seatedTableNotificationPeriod = snapshot.child("seatedNotification").getValue(Int::class.java) ?: 5
                eatingTableNotificationPeriod = snapshot.child("eatingNotification").getValue(Int::class.java) ?: 5
                paidTableNotificationPeriod = snapshot.child("paidNotification").getValue(Int::class.java) ?: 5

            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SettingsListener", "Failed to read settings", error.toException())
            }
        })
    }

    private fun updateDataInAnalytics(id: String, mutableMap: MutableMap<String, Int>){
        //tables update
        val tablesRef = CompanyID?.let {
            db.child("companies").child(it).child("Analytics").child("tables").child(id).child("numberOfServedTimes")
        }
        tablesRef?.get()?.addOnSuccessListener { snapshot ->
            val currentValue = snapshot.getValue(Int::class.java) ?: 0
            val newValue = currentValue + 1

            tablesRef.setValue(newValue)
        }?.addOnFailureListener { error ->
            Log.e("Firebase", "Chyba při načítání numberOfServedTimes: ${error.message}")
        }

        //user update
        val userRef = if (userId != null) {
            CompanyID?.let {
                db.child("companies").child(it).child("Analytics").child("users").child(userId).child("numberOfServedTables")
            }
        } else {
            null
        }

        userRef?.let { ref ->
            ref.get().addOnSuccessListener { snapshot ->
                val currentValue = snapshot.getValue(Int::class.java) ?: 0
                val newValue = currentValue + 1

                ref.setValue(newValue)
            }.addOnFailureListener { error ->
                Log.e("Firebase", "Chyba při načítání numberOfServedTables: ${error.message}")
            }
        }

        //items update
        val itemsRef = CompanyID?.let {
            db.child("companies").child(it).child("Analytics").child("items")
        }
        mutableMap.forEach{ (key, value) ->
            val itemId = findItemIdByName(key)
            val ref = itemId?.let { itemsRef?.child(it)?.child("numberOfServedTimes") }
            ref?.get()?.addOnSuccessListener { snapshot ->
                val currentValue = snapshot.getValue(Int::class.java) ?: 0
                val newValue = currentValue + value

                ref.setValue(newValue)
            }?.addOnFailureListener { error ->
                Log.e("Firebase", "Chyba při načítání numberOfServedTimes: ${error.message}")
            }
        }
    }

    private fun findItemIdByName(name: String): String? {
        allMenuItems.forEach { menuItem ->
            if (menuItem.name.equals(name)) return menuItem.id
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        removeTableStateListeners()
    }

    companion object {
        private const val COMPANY_ID = "COMPANY_ID"
        @JvmStatic
        fun newInstance(companyId: String) =
            Model_view().apply {
                arguments = Bundle().apply {
                    putString(COMPANY_ID, companyId)
                }
            }
    }
}