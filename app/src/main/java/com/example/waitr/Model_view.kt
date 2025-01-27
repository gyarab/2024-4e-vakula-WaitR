package com.example.waitr

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

//nadefinovane objekty...

class Model_view : Fragment() {
    //promenne
    private var CompanyID: String? = null
    private lateinit var editButton: ImageButton
    private lateinit var helpButton: ImageButton
    private lateinit var modelScenesBar: LinearLayout
    private lateinit var currentScene: FrameLayout
    private val db = FirebaseDatabase.getInstance("https://waitr-dee9a-default-rtdb.europe-west1.firebasedatabase.app/").reference // Using Realtime Database reference
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser
    private val userId = currentUser?.uid
    private lateinit var noStagesTODisplayTextView: TextView
    private lateinit var addStageButtonIfNon: ImageButton
    private lateinit var dynamicLinearLayout: LinearLayout
    private lateinit var editModelDialoge: Dialog
    private lateinit var editModelScenesLayout: LinearLayout
    private lateinit var editModelSceneLayout: FrameLayout
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
    private var finalX: Int = 0
    private var finalY: Int = 0
    private var selectedStageId: String? = null
    private var selectedTableId: String? = null
    private var selectedHelperId: String? = null
    private var helperEditMode: Boolean = false
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var newTableName: String? = null
    private var newTableHeight: Int? = null
    private var newTableWidth: Int? = null
    private var newHelperHeight: Int? = null
    private var newHelperWidth: Int? = null
    private var newSceneName: String? = null

// zde psat pouze kod nesouvisejici s UI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    // Cekati na predani argumentu z aktivity do promene na CompanyID
    arguments?.getString(COMPANY_ID)?.let {
        CompanyID = it
    }
    }
// zde psat kod souvisejici s UI
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
    // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_model_view, container, false)
    // nastaveni promenne editModelDialoge
    editModelDialoge = Dialog(requireContext())
    editModelDialoge.setContentView(R.layout.edit_model_view_popup)
    val scrollableView = editModelDialoge.findViewById<HorizontalScrollView>(R.id.edit_model_horizontal_scroll_view)
    editModelScenesLayout = scrollableView.findViewById(R.id.linearlayout_for_scenes)
    editModelSceneLayout = editModelDialoge.findViewById(R.id.edit_model_canvas_layout)
    confirmTableChanges = editModelDialoge.findViewById(R.id.confirm_table_changes_button)
    confirmTableChanges.setOnClickListener {
        if (tableEditMode){
            tableEditMode = false
            val table = findTableById(selectedTableId!!)
            table?.xPosition = finalX
            table?.yPosition = finalY

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
            val helper = findHelperById(selectedHelperId!!)
            helper?.xPosition = finalX
            helper?.yPosition = finalY

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
    noStagesTODisplayTextView = TextView(context).apply {
        text = "You need to create your first scene "
        textSize = 25f
        gravity = Gravity.CENTER
    }
    addStageButtonIfNon = ImageButton(context).apply {
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
    dynamicLinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
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
        helpButton = view.findViewById(R.id.help_button)
        helpButton.setOnClickListener {
            //TODO dodelat help tlacitko
        }
        // nacte model z database ktery nasledne vykresli
        fetchModel()
        checkIfSceneExists()
    }

    private fun showEditModelPopUp(){
        editModel = model
        //vykresleni scen
        drawEditScenesToBar()
        // nacteni sceny
        if (editModel.listOfScenes.isNotEmpty()){
            selectedStageId = editModel.listOfScenes.get(0).id
            drawEditScene()
        } else {
            val textView = TextView(context).apply {
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
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.95).toInt()
        )
        // Reference na prvky v popup layoutu
        saveButton = editModelDialoge.findViewById(R.id.save_model_edit)
        cancelButton = editModelDialoge.findViewById(R.id.cancel_model_edit)
        addTableButton = editModelDialoge.findViewById(R.id.add_table)
        addSceneButton = editModelDialoge.findViewById(R.id.add_scene)
        addHelperButton = editModelDialoge.findViewById(R.id.add_helper_shape)
        saveButton.setOnClickListener {
            model = editModel
            updateModel()
            editModelDialoge.dismiss()
        }
        cancelButton.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
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
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addTablePopup(){
        // Vytvoření dialogu
        val dialog = Dialog(requireContext())
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
            val randomID = UUID.randomUUID().toString()
            val table = Table(
                randomID,
                tableName,
                "prazdny",
                0,
                mutableListOf(),
                0,
                150,
                150,
                0,
                0)
            val currentModelScene = selectedStageId?.let { it1 -> findSceneById(editModel, it1) }
            currentModelScene?.listOfTables?.add(table)
            Log.e("model", editModel.toString())

            val textView = TextView(context).apply {
                text = tableName
                textSize = 18f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.LTGRAY)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    width = 150 // Výchozí šířka
                    height = 150 // Výchozí výška
                }
                tag = randomID
            }
            textView.setOnClickListener(
                CustomClickListener(
                    onClick = {
                        selectedTableId = textView.tag.toString()
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
                        selectedTableId = textView.tag.toString()
                        tableOptionsPopup()
                    }
                )
            )
            editModelSceneLayout.addView(textView)

            textView.setOnTouchListener { view, event ->
                if (tableEditMode && currentTableToEdit == view) { // Povolit manipulaci pouze pro aktuálně editovaný stůl
                    val params = view.layoutParams as FrameLayout.LayoutParams
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // Inicializace proměnných při zahájení dotyku
                            initialX = params.leftMargin.toFloat()
                            initialY = params.topMargin.toFloat()
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = event.rawX - initialTouchX
                            val deltaY = event.rawY - initialTouchY

                            // Vypočítat nové souřadnice
                            val newLeft = (initialX + deltaX).toInt()
                                .coerceIn(0, editModelSceneLayout.width - view.width)
                            val newTop = (initialY + deltaY).toInt()
                                .coerceIn(0, editModelSceneLayout.height - view.height)

                            val tempParams = FrameLayout.LayoutParams(params)
                            tempParams.leftMargin = newLeft
                            tempParams.topMargin = newTop

                            // Zkontrolovat, zda nové umístění nepřekrývá jiné prvky
                            var canMove = true
                            for (i in 0 until editModelSceneLayout.childCount) {
                                val otherView = editModelSceneLayout.getChildAt(i)
                                if (otherView != view && otherView is TextView) {
                                    val otherParams = otherView.layoutParams as FrameLayout.LayoutParams
                                    val otherRect = Rect(
                                        otherParams.leftMargin,
                                        otherParams.topMargin,
                                        otherParams.leftMargin + otherView.width,
                                        otherParams.topMargin + otherView.height
                                    )
                                    val newRect = Rect(
                                        tempParams.leftMargin,
                                        tempParams.topMargin,
                                        tempParams.leftMargin + view.width,
                                        tempParams.topMargin + view.height
                                    )
                                    if (Rect.intersects(newRect, otherRect)) {
                                        canMove = false
                                        break
                                    }
                                }
                            }

                            if (canMove) {
                                // Pokud nedochází k překryvu, aktualizuj souřadnice
                                params.leftMargin = newLeft
                                params.topMargin = newTop
                                view.layoutParams = params

                                finalX = newLeft
                                finalY = newTop
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
        val dialog = Dialog(requireContext())
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
            val randomID = UUID.randomUUID().toString()

            val scene = ModelScene(randomID, sceneName, mutableListOf(), mutableListOf())
            editModel.listOfScenes.add(scene)
            selectedStageId = randomID
            drawEditScene()

            val textViewForScene = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(5, 5, 10, -5)
                }
                //setAutoSizeTextTypeUniformWithConfiguration(28, 100, 1, TypedValue.COMPLEX_UNIT_DIP)
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
        if (editModel.listOfScenes.isEmpty()){
            Toast.makeText(requireContext(), "First you need to create scene", Toast.LENGTH_SHORT).show()
        } else {
            val randomId = UUID.randomUUID().toString()
            val helper = HelperShape(
                randomId,
                150,
                150,
                0,
                0
            )
            val scene = findSceneById(editModel, selectedStageId!!)
            scene?.listOfHelpers?.add(helper)

            val textView = TextView(context).apply {
                textSize = 18f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.BLACK)
                tag = randomId
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    width = 150
                    height = 150
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
                    val params = view.layoutParams as FrameLayout.LayoutParams
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // Inicializace proměnných při zahájení dotyku
                            initialX = params.leftMargin.toFloat()
                            initialY = params.topMargin.toFloat()
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = event.rawX - initialTouchX
                            val deltaY = event.rawY - initialTouchY

                            // Vypočítat nové souřadnice
                            val newLeft = (initialX + deltaX).toInt()
                                .coerceIn(0, editModelSceneLayout.width - view.width)
                            val newTop = (initialY + deltaY).toInt()
                                .coerceIn(0, editModelSceneLayout.height - view.height)

                            val tempParams = FrameLayout.LayoutParams(params)
                            tempParams.leftMargin = newLeft
                            tempParams.topMargin = newTop

                            // Zkontrolovat, zda nové umístění nepřekrývá jiné prvky
                            var canMove = true
                            for (i in 0 until editModelSceneLayout.childCount) {
                                val otherView = editModelSceneLayout.getChildAt(i)
                                if (otherView != view && otherView is TextView) {
                                    val otherParams = otherView.layoutParams as FrameLayout.LayoutParams
                                    val otherRect = Rect(
                                        otherParams.leftMargin,
                                        otherParams.topMargin,
                                        otherParams.leftMargin + otherView.width,
                                        otherParams.topMargin + otherView.height
                                    )
                                    val newRect = Rect(
                                        tempParams.leftMargin,
                                        tempParams.topMargin,
                                        tempParams.leftMargin + view.width,
                                        tempParams.topMargin + view.height
                                    )
                                    if (Rect.intersects(newRect, otherRect)) {
                                        canMove = false
                                        break
                                    }
                                }
                            }

                            if (canMove) {
                                // Pokud nedochází k překryvu, aktualizuj souřadnice
                                params.leftMargin = newLeft
                                params.topMargin = newTop
                                view.layoutParams = params

                                finalX = newLeft
                                finalY = newTop
                            }

                            true
                        }

                        else -> false
                    }
                } else false
            }
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun drawEditScene(){
        editModelSceneLayout.removeAllViews()
        lateinit var scene: ModelScene
        editModel.listOfScenes.forEach { modelScene ->
            if (modelScene.id == selectedStageId){
                scene = modelScene
            }
        }
        scene.listOfTables.forEach { table ->
            val textView = TextView(context).apply {
                text = table.name
                textSize = 18f
                tag = table.id
                gravity = Gravity.CENTER
                setBackgroundColor(Color.LTGRAY)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    width = table.width
                    height = table.height
                    setMargins(table.xPosition, table.yPosition, 0, 0)
                }
            }
            textView.setOnClickListener(
                CustomClickListener(
                    onClick = {
                        selectedTableId = textView.tag.toString()
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
                        selectedTableId = textView.tag.toString()
                        tableOptionsPopup()
                    }
                )
            )
            textView.setOnTouchListener { view, event ->
                if (tableEditMode && currentTableToEdit == view) { // Povolit manipulaci pouze pro aktuálně editovaný stůl
                    val params = view.layoutParams as FrameLayout.LayoutParams
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // Inicializace proměnných při zahájení dotyku
                            initialX = params.leftMargin.toFloat()
                            initialY = params.topMargin.toFloat()
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = event.rawX - initialTouchX
                            val deltaY = event.rawY - initialTouchY

                            // Vypočítat nové souřadnice
                            val newLeft = (initialX + deltaX).toInt()
                                .coerceIn(0, editModelSceneLayout.width - view.width)
                            val newTop = (initialY + deltaY).toInt()
                                .coerceIn(0, editModelSceneLayout.height - view.height)

                            val tempParams = FrameLayout.LayoutParams(params)
                            tempParams.leftMargin = newLeft
                            tempParams.topMargin = newTop

                            // Zkontrolovat, zda nové umístění nepřekrývá jiné prvky
                            var canMove = true
                            for (i in 0 until editModelSceneLayout.childCount) {
                                val otherView = editModelSceneLayout.getChildAt(i)
                                if (otherView != view && otherView is TextView) {
                                    val otherParams = otherView.layoutParams as FrameLayout.LayoutParams
                                    val otherRect = Rect(
                                        otherParams.leftMargin,
                                        otherParams.topMargin,
                                        otherParams.leftMargin + otherView.width,
                                        otherParams.topMargin + otherView.height
                                    )
                                    val newRect = Rect(
                                        tempParams.leftMargin,
                                        tempParams.topMargin,
                                        tempParams.leftMargin + view.width,
                                        tempParams.topMargin + view.height
                                    )
                                    if (Rect.intersects(newRect, otherRect)) {
                                        canMove = false
                                        break
                                    }
                                }
                            }

                            if (canMove) {
                                // Pokud nedochází k překryvu, aktualizuj souřadnice
                                params.leftMargin = newLeft
                                params.topMargin = newTop
                                view.layoutParams = params

                                finalX = newLeft
                                finalY = newTop
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
            val textView = TextView(context).apply {
                textSize = 18f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.BLACK)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    width = helper.width
                    height = helper.height
                    setMargins(helper.xPosition, helper.yPosition, 0, 0)
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
                    val params = view.layoutParams as FrameLayout.LayoutParams
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // Inicializace proměnných při zahájení dotyku
                            initialX = params.leftMargin.toFloat()
                            initialY = params.topMargin.toFloat()
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = event.rawX - initialTouchX
                            val deltaY = event.rawY - initialTouchY

                            // Vypočítat nové souřadnice
                            val newLeft = (initialX + deltaX).toInt()
                                .coerceIn(0, editModelSceneLayout.width - view.width)
                            val newTop = (initialY + deltaY).toInt()
                                .coerceIn(0, editModelSceneLayout.height - view.height)

                            val tempParams = FrameLayout.LayoutParams(params)
                            tempParams.leftMargin = newLeft
                            tempParams.topMargin = newTop

                            // Zkontrolovat, zda nové umístění nepřekrývá jiné prvky
                            var canMove = true
                            for (i in 0 until editModelSceneLayout.childCount) {
                                val otherView = editModelSceneLayout.getChildAt(i)
                                if (otherView != view && otherView is TextView) {
                                    val otherParams = otherView.layoutParams as FrameLayout.LayoutParams
                                    val otherRect = Rect(
                                        otherParams.leftMargin,
                                        otherParams.topMargin,
                                        otherParams.leftMargin + otherView.width,
                                        otherParams.topMargin + otherView.height
                                    )
                                    val newRect = Rect(
                                        tempParams.leftMargin,
                                        tempParams.topMargin,
                                        tempParams.leftMargin + view.width,
                                        tempParams.topMargin + view.height
                                    )
                                    if (Rect.intersects(newRect, otherRect)) {
                                        canMove = false
                                        break
                                    }
                                }
                            }

                            if (canMove) {
                                // Pokud nedochází k překryvu, aktualizuj souřadnice
                                params.leftMargin = newLeft
                                params.topMargin = newTop
                                view.layoutParams = params

                                finalX = newLeft
                                finalY = newTop
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

    private fun drawEditScenesToBar(){
        editModelScenesLayout.removeAllViews()
        if (editModel.listOfScenes.isEmpty()){
            val textView = TextView(context).apply {
                this.text = "No Scene"
                this.textSize = 18f // Velikost textu
                this.setTextColor(Color.WHITE) // Barva textu
            }
            editModelScenesLayout.addView(textView)
        } else {
            editModel.listOfScenes.forEach { scene ->
                val name = scene.name
                val id = scene.id
                val textViewForScene = TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(5, 5, 10, -5)
                    }
                    //setAutoSizeTextTypeUniformWithConfiguration(28, 100, 1, TypedValue.COMPLEX_UNIT_DIP)
                    textSize = 28f
                    text = name
                    setBackgroundColor(Color.WHITE)
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER
                    isSingleLine = true
                    ellipsize = TextUtils.TruncateAt.END
                    tag = id
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
            }
        }
    }

    private fun sceneOptionsPopup(){
        val scene = findSceneById(editModel, selectedStageId!!)
        val sceneName = scene?.name

        // Vytvoření dialogu
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.scene_options_popup)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            (resources.displayMetrics.heightPixels * 0.9).toInt()
        )

        // Reference na prvky v popup layoutu
        val nameOfTheTable = dialog.findViewById<TextView>(R.id.name_of_the_table)
        nameOfTheTable.text = sceneName
        val saveButton = dialog.findViewById<Button>(R.id.save_scene_options_button)
        saveButton.setOnClickListener {
            if (newSceneName == null && sceneName != null){
                scene.name = sceneName
            }
            if (newSceneName != null){
                scene?.name = newSceneName.toString()
            }
            newSceneName = null
            dialog.dismiss()
        }
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_scene_options_button)
        cancelButton.setOnClickListener {
            newSceneName = null
            dialog.dismiss()
        }
        val deleteButton = dialog.findViewById<Button>(R.id.delete_scene_options_button)
        deleteButton.setOnClickListener {
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
                drawEditScene()
            } else {
                val textView = TextView(context).apply {
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
        }
        val changeNameButton = dialog.findViewById<Button>(R.id.change_scene_name)
        changeNameButton.setOnClickListener {
            changeSceneName()
        }
        dialog.show()
    }

    private fun changeSceneName(){
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
        textView.text = "Scene name"
        val parametrToChange = dialog.findViewById<TextInputEditText>(R.id.parameter_to_change)
        parametrToChange.hint = "New Scene name"
        val changeTextButton = dialog.findViewById<Button>(R.id.change_group_name_button)
        changeTextButton.text = "Change name"
        changeTextButton.setOnClickListener {
            val newName = parametrToChange.text.toString().trim()
            if (newName.isEmpty()){
                Toast.makeText(dialog.context, "You must enter the name first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            newSceneName = newName
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun tableOptionsPopup(){
        val table = findTableById(selectedTableId!!)
        val scene = findSceneById(editModel, selectedStageId!!)
        val tableName = table?.name
        val tableHeight = table?.height
        val tableWidth = table?.width

        // Vytvoření dialogu
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.table_options_popup)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            (resources.displayMetrics.heightPixels * 0.9).toInt()
        )

        // Reference na prvky v popup layoutu
        val nameOfTheTable = dialog.findViewById<TextView>(R.id.name_of_the_table)
        nameOfTheTable.text = tableName
        val heightOfTheTable = dialog.findViewById<TextView>(R.id.height_of_the_table)
        heightOfTheTable.text = tableHeight.toString()
        val widthOfTheTable = dialog.findViewById<TextView>(R.id.width_of_the_table)
        widthOfTheTable.text = tableWidth.toString()
        val saveButton = dialog.findViewById<Button>(R.id.save_table_options_button)
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
            newTableName = null
            newTableHeight = null
            newTableWidth = null
            dialog.dismiss()
        }
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_table_options_button)
        cancelButton.setOnClickListener {
            newTableName = null
            newTableHeight = null
            newTableWidth = null
            dialog.dismiss()
        }
        val deleteButton = dialog.findViewById<Button>(R.id.delete_table_options_button)
        deleteButton.setOnClickListener {
            newTableName = null
            newTableHeight = null
            newTableWidth = null
            scene?.deleteTable(selectedTableId!!)

            val viewToRemove = findViewWithTag(editModelSceneLayout, selectedTableId!!)
            val layout = viewToRemove?.parent
            if (layout is FrameLayout){
                layout.removeView(viewToRemove)
            }
            dialog.dismiss()
        }
        val changeNameButton = dialog.findViewById<Button>(R.id.change_table_name)
        changeNameButton.setOnClickListener {
            changeTableName()
        }
        val changeHeightButton = dialog.findViewById<Button>(R.id.change_table_height)
        changeHeightButton.setOnClickListener {
            changeTableHeight()
        }
        val changeWidthButton = dialog.findViewById<Button>(R.id.change_table_width)
        changeWidthButton.setOnClickListener {
            changeTableWidth()
        }
        dialog.show()
    }

    private fun changeTableName(){
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
        textView.text = "Table name"
        val parametrToChange = dialog.findViewById<TextInputEditText>(R.id.parameter_to_change)
        parametrToChange.hint = "New Table name"
        val changeTextButton = dialog.findViewById<Button>(R.id.change_group_name_button)
        changeTextButton.text = "Change name"
        changeTextButton.setOnClickListener {
            val newName = parametrToChange.text.toString().trim()
            if (newName.isEmpty()){
                Toast.makeText(dialog.context, "You must enter the name first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            newTableName = newName
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun changeTableHeight(){
        // Vytvoření dialogu
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.change_parameters_for_menu_elements)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.8).toInt(),
            (resources.displayMetrics.heightPixels * 0.7).toInt()
        )
        // Reference na prvky v popup layoutu
        val textView = dialog.findViewById<TextView>(R.id.parametr_view)
        textView.text = "Table height"
        val parametrToChange = dialog.findViewById<TextInputEditText>(R.id.parameter_to_change)
        parametrToChange.hint = "New table height"
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
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun changeTableWidth(){
        // Vytvoření dialogu
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.change_parameters_for_menu_elements)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.8).toInt(),
            (resources.displayMetrics.heightPixels * 0.7).toInt()
        )
        // Reference na prvky v popup layoutu
        val textView = dialog.findViewById<TextView>(R.id.parametr_view)
        textView.text = "Table width"
        val parametrToChange = dialog.findViewById<TextInputEditText>(R.id.parameter_to_change)
        parametrToChange.hint = "New table width"
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
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun helperOptionsPopup(){
        val helper = findHelperById(selectedHelperId!!)
        val scene = findSceneById(editModel, selectedStageId!!)
        val helperHeight = helper?.height
        val helperWidth = helper?.width

        // Vytvoření dialogu
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.helper_options_popup)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            (resources.displayMetrics.heightPixels * 0.9).toInt()
        )

        // Reference na prvky v popup layoutu
        val heightOfTheHelper = dialog.findViewById<TextView>(R.id.height_of_the_helper)
        heightOfTheHelper.text = helperHeight.toString()
        val widthOfTheHelper = dialog.findViewById<TextView>(R.id.width_of_the_helper)
        widthOfTheHelper.text = helperWidth.toString()
        val saveButton = dialog.findViewById<Button>(R.id.save_helper_options_button)
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
            newHelperHeight = null
            newHelperWidth= null
            dialog.dismiss()
        }
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_helper_options_button)
        cancelButton.setOnClickListener {
            newHelperHeight = null
            newHelperWidth= null
            dialog.dismiss()
        }
        val deleteButton = dialog.findViewById<Button>(R.id.delete_helper_options_button)
        deleteButton.setOnClickListener {
            newHelperHeight = null
            newHelperWidth = null
            scene?.deleteHelper(selectedHelperId!!)

            val viewToRemove = findViewWithTag(editModelSceneLayout, selectedHelperId!!)
            val layout = viewToRemove?.parent
            if (layout is FrameLayout){
                layout.removeView(viewToRemove)
            }
            dialog.dismiss()
        }
        val changeHeightButton = dialog.findViewById<Button>(R.id.change_helper_height)
        changeHeightButton.setOnClickListener {
            changeHelperHeight()
        }
        val changeWidthButton = dialog.findViewById<Button>(R.id.change_helper_width)
        changeWidthButton.setOnClickListener {
            changeHelperWidth()
        }
        dialog.show()
    }

    private fun changeHelperHeight(){
        // Vytvoření dialogu
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.change_parameters_for_menu_elements)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.8).toInt(),
            (resources.displayMetrics.heightPixels * 0.7).toInt()
        )
        // Reference na prvky v popup layoutu
        val textView = dialog.findViewById<TextView>(R.id.parametr_view)
        textView.text = "Helper height"
        val parametrToChange = dialog.findViewById<TextInputEditText>(R.id.parameter_to_change)
        parametrToChange.hint = "New helper height"
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
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun changeHelperWidth(){
        // Vytvoření dialogu
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.change_parameters_for_menu_elements)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.8).toInt(),
            (resources.displayMetrics.heightPixels * 0.7).toInt()
        )
        // Reference na prvky v popup layoutu
        val textView = dialog.findViewById<TextView>(R.id.parametr_view)
        textView.text = "Helper width"
        val parametrToChange = dialog.findViewById<TextInputEditText>(R.id.parameter_to_change)
        parametrToChange.hint = "New helper width"
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
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun validateTablePosition(whichPar: String ,parameter: Int): Boolean {

        // Najdi aktuální prvek podle jeho ID
        val selectedTable = findTableById(selectedTableId!!) ?: return false

        // Získej parametry aktuálního prvku
        val selectedLeft = selectedTable.xPosition
        val selectedTop = selectedTable.yPosition
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
            val otherTable = findTableById(child.id.toString()) ?: continue

            // Vynech aktuálně kontrolovaný prvek
            if (otherTable.id == selectedTableId) continue

            // Získej parametry ostatního prvku
            val otherLeft = otherTable.xPosition
            val otherTop = otherTable.yPosition
            val otherRight = otherLeft + otherTable.width
            val otherBottom = otherTop + otherTable.height

            // Zkontroluj překryv mezi prvky
            val isOverlapping = !(selectedRight <= otherLeft || // Není vlevo
                    selectedLeft >= otherRight || // Není vpravo
                    selectedBottom <= otherTop || // Není nahoře
                    selectedTop >= otherBottom)   // Není dole

            if (isOverlapping) {
                return false // Prvky se překrývají
            }
        }

        // Vše je v pořádku
        return true
    }
    private fun validateHelperPosition(whichPar: String ,parameter: Int): Boolean {

        // Najdi aktuální prvek podle jeho ID
        val selectedHelper = findHelperById(selectedHelperId!!) ?: return false

        // Získej parametry aktuálního prvku
        val selectedLeft = selectedHelper.xPosition
        val selectedTop = selectedHelper.yPosition
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
            val otherTable = findHelperById(child.id.toString()) ?: continue

            // Vynech aktuálně kontrolovaný prvek
            if (otherTable.id == selectedHelperId) continue

            // Získej parametry ostatního prvku
            val otherLeft = otherTable.xPosition
            val otherTop = otherTable.yPosition
            val otherRight = otherLeft + otherTable.width
            val otherBottom = otherTop + otherTable.height

            // Zkontroluj překryv mezi prvky
            val isOverlapping = !(selectedRight <= otherLeft || // Není vlevo
                    selectedLeft >= otherRight || // Není vpravo
                    selectedBottom <= otherTop || // Není nahoře
                    selectedTop >= otherBottom)   // Není dole

            if (isOverlapping) {
                return false // Prvky se překrývají
            }
        }

        // Vše je v pořádku
        return true
    }

    private fun updateModel(){
        val companyModelRef = CompanyID?.let {
            db.child("companies").child(it).child("Model")
        }
        companyModelRef
            ?.updateChildren(model.toMap())
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
        fetchModel()
    }

    private fun fetchModel(){
        val companyModelRef = CompanyID?.let {
            db.child("companies").child(it).child("Model")
        }

        companyModelRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Deserialize snapshot into Model object
                    val fetchedModel = snapshot.getValue(Model::class.java)
                    if (fetchedModel != null) {
                        // Assign to local variable or state
                        model = fetchedModel
                        editModel = fetchedModel
                        updateModelUI()
                        updateEditModelUI()

                    } else {
                        Toast.makeText(
                            context,
                            "Failed to parse model data.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Model not found.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    context,
                    "Error loading model: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
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

    private fun drawScene(){
        if (selectedStageId == null){
            selectedStageId = model.listOfScenes.get(0).id
        }
        currentScene.removeAllViews()
        lateinit var scene: ModelScene
        model.listOfScenes.forEach { modelScene ->
            if (modelScene.id == selectedStageId){
                scene = modelScene
            }
        }
        scene.listOfTables.forEach { table ->
            val textView = TextView(context).apply {
                text = table.name
                textSize = 18f
                tag = table.id
                gravity = Gravity.CENTER
                setBackgroundColor(Color.LTGRAY)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    width = table.width
                    height = table.height
                    setMargins(table.xPosition, table.yPosition, 0, 0)
                }
            }
            textView.setOnClickListener(
                CustomClickListener(
                    onClick = {
                        //TODO
                    },
                    onDoubleClick = {

                    }
                )
            )
            currentScene.addView(textView)
        }
        scene.listOfHelpers.forEach { helper ->
            val textView = TextView(context).apply {
                textSize = 18f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.BLACK)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    width = helper.width
                    height = helper.height
                    setMargins(helper.xPosition, helper.yPosition, 0, 0)
                }
            }
            currentScene.addView(textView)
        }
    }

    private fun drawScenesToBar(){
        modelScenesBar.removeAllViews()
        if (model.listOfScenes.isEmpty()){
            val textView = TextView(context).apply {
                this.text = "No Scene"
                this.textSize = 18f // Velikost textu
                this.setTextColor(Color.WHITE) // Barva textu
            }
            modelScenesBar.addView(textView)
        } else {
            model.listOfScenes.forEach { scene ->
                val name = scene.name
                val id = scene.id
                val textViewForScene = TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(5, 5, 10, -5)
                    }
                    //setAutoSizeTextTypeUniformWithConfiguration(28, 100, 1, TypedValue.COMPLEX_UNIT_DIP)
                    textSize = 28f
                    text = name
                    setBackgroundColor(Color.WHITE)
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER
                    isSingleLine = true
                    ellipsize = TextUtils.TruncateAt.END
                    tag = id
                }
                textViewForScene.setOnClickListener(
                    CustomClickListener(
                        onClick = {
                            selectedStageId = textViewForScene.tag.toString()
                            drawScene()
                        },
                        onDoubleClick = {
                        }
                    )
                )
                modelScenesBar.addView(textViewForScene)
            }
        }
    }

    private fun findTableById(id: String): Table?{
        val scene = findSceneById(editModel, selectedStageId!!)
        scene?.listOfTables?.forEach { table ->
            if (table.id == id) return table
        }
        return null
    }
    private fun findHelperById(id: String): HelperShape?{
        val scene = findSceneById(editModel, selectedStageId!!)
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

    companion object {
        private const val COMPANY_ID = "COMPANY_ID"
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment Model_view.
         */
        @JvmStatic
        fun newInstance(companyId: String) =
            Model_view().apply {
                arguments = Bundle().apply {
                    putString(COMPANY_ID, companyId)
                }
            }
    }
}