package com.example.waitr

import android.annotation.SuppressLint
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
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID

//nadefinovane objekty...

class Model_view : Fragment() {
    //promenne
    private var CompanyID: String? = null
    private lateinit var editButton: ImageButton
    private lateinit var helpButton: ImageButton
    private lateinit var currentScene: FrameLayout
    private lateinit var platno: FrameLayout
    private val db = FirebaseDatabase.getInstance("https://waitr-dee9a-default-rtdb.europe-west1.firebasedatabase.app/").reference // Using Realtime Database reference
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser
    private val userId = currentUser?.uid
    private lateinit var noStagesTODisplayTextView: TextView
    private lateinit var addStageButtonIfNon: ImageButton
    private var dynamicLinearLayout: LinearLayout? = null
    private lateinit var viewModel: ModelViewModel
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
    private var finalWidth: Int = 0
    private var finalHeight: Int = 0
    private var selectedStageId: String? = null
    private var selectedTableId: String? = null
    private var selectedHelperId: String? = null
    private var helperEditMode: Boolean = false
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f

// zde psat pouze kod nesouvisejici s UI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    viewModel = ViewModelProvider(this).get(ModelViewModel::class.java)

    // Cekati na predani argumentu z aktivity do promene na CompanyID
    arguments?.getString(COMPANY_ID)?.let {
        CompanyID = it
    }
    checkIfSceneExists()
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

    platno = view.findViewById(R.id.canvas_layout)
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
    }

    private fun showEditModelPopUp(){
        editModel = model
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
            //TODO
            editModelDialoge.dismiss()
        }
        cancelButton.setOnClickListener {
            //TODO
            editModelDialoge.dismiss()
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

    private fun sceneOptionsPopup(){

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

    private fun switchFrameLayoutContent(sourceLayout: FrameLayout, targetLayout: FrameLayout) {
        // Odstrani všechny děti z cílového FrameLayout
        targetLayout.removeAllViews()

        // Přida všechny děti ze zdrojového FrameLayout do cílového FrameLayout
        for (i in 0 until sourceLayout.childCount) {
            val child = sourceLayout.getChildAt(i)
            val clone = cloneView(child)
            targetLayout.addView(clone)
        }
    }

    private fun cloneView(view: View): View {
        val parent = view.parent
        if (parent is ViewGroup) {
            parent.removeView(view)
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        updateUI(viewModel.isSceneAvailable)
    }
    // metoda ktera zkotroluje jestli jsou vytvorene sceny pro zobrazeni
    private fun checkIfSceneExists(){
        val ref = CompanyID?.let { db.child("companies").child(it) }
        ref?.get()?.addOnSuccessListener { dataSnapshot ->
            if (isAdded) {
                viewModel.isSceneAvailable = dataSnapshot.hasChild("ModelView")
                updateUI(viewModel.isSceneAvailable)
            }
        }?.addOnFailureListener { exception ->
            Log.e("Firebase", "Error getting data: ", exception)
        }
    }
    private fun updateUI(isSceneAvailable: Boolean) {
        if (isSceneAvailable) {
            dynamicLinearLayout?.let { platno.removeView(it) }
            dynamicLinearLayout = null
        } else {
            if (dynamicLinearLayout == null) {
                dynamicLinearLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    addView(noStagesTODisplayTextView)
                    addView(addStageButtonIfNon)
                }
            }
            dynamicLinearLayout?.parent?.let {
                (it as ViewGroup).removeView(dynamicLinearLayout)
            }
            platno.addView(dynamicLinearLayout)
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