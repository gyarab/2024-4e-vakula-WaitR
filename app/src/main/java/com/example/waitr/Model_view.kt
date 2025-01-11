package com.example.waitr

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
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
import androidx.transition.Scene
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
    private var selectedFrameLayoutId: String? = null
    private var model: Model = Model(mutableListOf())
    private var editModel: Model = Model(mutableListOf())
    private var listOfScenesAsLayouts: MutableList<FrameLayout> = mutableListOf()
    private var editListOfScenesAsLayouts: MutableList<FrameLayout> = mutableListOf()
    private var tableEditMode: Boolean = false
    private lateinit var saveButton: TextView
    private lateinit var cancelButton: TextView
    private lateinit var addTableButton: TextView
    private lateinit var addSceneButton: TextView
    private lateinit var addHelperButton: TextView
    private lateinit var confirmTableChanges: ImageButton
    private lateinit var currentTableToEdit: TextView
    private var finalX: Int = 0
    private var finalY: Int = 0
    private var finalWidth: Int = 0
    private var finalHeight: Int = 0

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
            //TODO
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
            val currentScene = selectedFrameLayoutId?.let { it1 -> findSceneById(editModel, it1) }
            currentScene?.listOfTables?.add(table)

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
            }
            textView.setOnClickListener(
                CustomClickListener(
                    onClick = {
                        tableEditMode = true
                        currentTableToEdit = textView
                        confirmTableChanges.visibility = View.VISIBLE
                        saveButton.visibility = View.GONE
                        cancelButton.visibility = View.GONE
                        addTableButton.visibility = View.GONE
                        addSceneButton.visibility = View.GONE
                        addHelperButton.visibility = View.GONE
                        editModelScenesLayout.visibility = View.GONE
                    },
                    onDoubleClick = {

                    }
                )
            )
            editModelSceneLayout.addView(textView)

            // Logika pro drag & drop a úpravu dimenzí
            var initialX = 0f
            var initialY = 0f
            var initialTouchX = 0f
            var initialTouchY = 0f

            textView.setOnTouchListener { view, event ->
                if (tableEditMode) {
                    val params = view.layoutParams as FrameLayout.LayoutParams
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.leftMargin.toFloat()
                            initialY = params.topMargin.toFloat()
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = event.rawX - initialTouchX
                            val deltaY = event.rawY - initialTouchY

                            // Dočasné nové souřadnice
                            val newLeft = (initialX + deltaX).toInt()
                                .coerceIn(0, editModelSceneLayout.width - view.width)
                            val newTop = (initialY + deltaY).toInt()
                                .coerceIn(0, editModelSceneLayout.height - view.height)

                            // Kontrola překryvu
                            val hasOverlap = editModelSceneLayout.children.any { child ->
                                if (child == view) return@any false // Ignorovat sebe
                                val otherParams = child.layoutParams as FrameLayout.LayoutParams
                                val otherRect = Rect(
                                    otherParams.leftMargin,
                                    otherParams.topMargin,
                                    otherParams.leftMargin + child.width,
                                    otherParams.topMargin + child.height
                                )
                                val newRect = Rect(
                                    newLeft,
                                    newTop,
                                    newLeft + view.width,
                                    newTop + view.height
                                )
                                Rect.intersects(otherRect, newRect)
                            }

                            if (!hasOverlap) {
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

            editModelSceneLayout.setOnTouchListener { _, event ->
                if (tableEditMode){
                    when (event.action) {
                        MotionEvent.ACTION_MOVE -> {
                            // Pokud se dotyká mimo TextView, mění se rozměry
                            val centerX = textView.left + textView.width / 2
                            val centerY = textView.top + textView.height / 2

                            val params = textView.layoutParams as FrameLayout.LayoutParams
                            val newWidth: Int
                            val newHeight: Int

                            if (event.x > centerX) {
                                // Změna šířky
                                val widthPercent =
                                    ((event.x - textView.left) / editModelSceneLayout.width).coerceIn(
                                        0.1f,
                                        0.9f
                                    )
                                newWidth = (widthPercent * editModelSceneLayout.width).toInt()
                            } else {
                                newWidth = textView.layoutParams.width
                            }

                            if (event.y > centerY) {
                                // Změna výšky
                                val heightPercent =
                                    ((event.y - textView.top) / editModelSceneLayout.height).coerceIn(
                                        0.1f,
                                        0.9f
                                    )
                                newHeight = (heightPercent * editModelSceneLayout.height).toInt()
                            } else {
                                newHeight = textView.layoutParams.height
                            }

                            // Vytvoření obdélníku pro nové rozměry TextView
                            val newRect = Rect(
                                textView.left,
                                textView.top,
                                textView.left + newWidth,
                                textView.top + newHeight
                            )

                            // Kontrola překryvu s ostatními TextView
                            val hasOverlap = editModelSceneLayout.children.any { child ->
                                if (child == textView) return@any false // Ignorovat aktuální TextView
                                val otherParams = child.layoutParams as FrameLayout.LayoutParams
                                val otherRect = Rect(
                                    otherParams.leftMargin,
                                    otherParams.topMargin,
                                    otherParams.leftMargin + child.width,
                                    otherParams.topMargin + child.height
                                )
                                Rect.intersects(otherRect, newRect)
                            }

                            if (!hasOverlap) {
                                finalWidth = newWidth
                                finalHeight = newHeight
                                // Nastavení nových rozměrů, pokud nedojde k překryvu
                                params.width = newWidth
                                params.height = newHeight
                                textView.layoutParams = params
                                textView.requestLayout()
                            }
                            true
                        }

                        else -> false
                    }
                } else false
            }

            confirmTableChanges.setOnClickListener {
                tableEditMode = false
                table.height = finalHeight
                table.width = finalWidth
                table.xPosition = finalX
                table.yPosition = finalY

                confirmTableChanges.visibility = View.GONE
                saveButton.visibility = View.VISIBLE
                cancelButton.visibility = View.VISIBLE
                addTableButton.visibility = View.VISIBLE
                addSceneButton.visibility = View.VISIBLE
                addHelperButton.visibility = View.VISIBLE
                editModelScenesLayout.visibility = View.VISIBLE
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
            val frameLayout = FrameLayout(requireContext())
            textViewForScene.setOnClickListener(
                CustomClickListener(
                    onClick = {
                        currentScene = frameLayout
                        selectedFrameLayoutId = textViewForScene.tag.toString()
                        switchFrameLayoutContent(frameLayout, editModelSceneLayout)
                    },
                    onDoubleClick = {
                        currentScene = frameLayout
                        selectedFrameLayoutId = textViewForScene.tag.toString()
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
                200,
                200,
                0,
                0
            )
            val currentScene = selectedFrameLayoutId?.let { it1 -> findSceneById(editModel, it1) }
            currentScene?.listOfHelpers?.add(helper)

            val textView = TextView(context).apply {
                textSize = 18f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.BLACK)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    width = 200 // Výchozí šířka
                    height = 200 // Výchozí výška
                }
            }
            textView.setOnClickListener(
                CustomClickListener(
                    onClick = {
                        tableEditMode = true
                        currentTableToEdit = textView
                        confirmTableChanges.visibility = View.VISIBLE
                        saveButton.visibility = View.GONE
                        cancelButton.visibility = View.GONE
                        addTableButton.visibility = View.GONE
                        addSceneButton.visibility = View.GONE
                        addHelperButton.visibility = View.GONE
                        editModelScenesLayout.visibility = View.GONE
                    },
                    onDoubleClick = {

                    }
                )
            )
            editModelSceneLayout.addView(textView)

            // Logika pro drag & drop a úpravu dimenzí
            var initialX = 0f
            var initialY = 0f
            var initialTouchX = 0f
            var initialTouchY = 0f

            textView.setOnTouchListener { view, event ->
                if (tableEditMode) {
                    val params = view.layoutParams as FrameLayout.LayoutParams
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.leftMargin.toFloat()
                            initialY = params.topMargin.toFloat()
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = event.rawX - initialTouchX
                            val deltaY = event.rawY - initialTouchY

                            // Dočasné nové souřadnice
                            val newLeft = (initialX + deltaX).toInt()
                                .coerceIn(0, editModelSceneLayout.width - view.width)
                            val newTop = (initialY + deltaY).toInt()
                                .coerceIn(0, editModelSceneLayout.height - view.height)

                            // Kontrola překryvu
                            val hasOverlap = editModelSceneLayout.children.any { child ->
                                if (child == view) return@any false // Ignorovat sebe
                                val otherParams = child.layoutParams as FrameLayout.LayoutParams
                                val otherRect = Rect(
                                    otherParams.leftMargin,
                                    otherParams.topMargin,
                                    otherParams.leftMargin + child.width,
                                    otherParams.topMargin + child.height
                                )
                                val newRect = Rect(
                                    newLeft,
                                    newTop,
                                    newLeft + view.width,
                                    newTop + view.height
                                )
                                Rect.intersects(otherRect, newRect)
                            }

                            if (!hasOverlap) {
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

            editModelSceneLayout.setOnTouchListener { _, event ->
                if (tableEditMode) {
                    when (event.action) {
                        MotionEvent.ACTION_MOVE -> {
                            // Pokud se dotyká mimo TextView, mění se rozměry
                            val centerX = currentTableToEdit.left + currentTableToEdit.width / 2
                            val centerY = currentTableToEdit.top + currentTableToEdit.height / 2

                            val params = currentTableToEdit.layoutParams as FrameLayout.LayoutParams
                            val newWidth: Int
                            val newHeight: Int

                            if (event.x > centerX) {
                                // Změna šířky
                                val widthPercent =
                                    ((event.x - currentTableToEdit.left) / editModelSceneLayout.width).coerceIn(
                                        0.1f,
                                        0.9f
                                    )
                                newWidth = (widthPercent * editModelSceneLayout.width).toInt()
                            } else {
                                newWidth = currentTableToEdit.layoutParams.width
                            }

                            if (event.y > centerY) {
                                // Změna výšky
                                val heightPercent =
                                    ((event.y - currentTableToEdit.top) / editModelSceneLayout.height).coerceIn(
                                        0.1f,
                                        0.9f
                                    )
                                newHeight = (heightPercent * editModelSceneLayout.height).toInt()
                            } else {
                                newHeight = currentTableToEdit.layoutParams.height
                            }

                            // Vytvoření obdélníku pro nové rozměry TextView
                            val newRect = Rect(
                                currentTableToEdit.left,
                                currentTableToEdit.top,
                                currentTableToEdit.left + newWidth,
                                currentTableToEdit.top + newHeight
                            )

                            // Kontrola překryvu s ostatními TextView
                            val hasOverlap = editModelSceneLayout.children.any { child ->
                                if (child == currentTableToEdit) return@any false // Ignorovat aktuální TextView
                                val otherParams = child.layoutParams as FrameLayout.LayoutParams
                                val otherRect = Rect(
                                    otherParams.leftMargin,
                                    otherParams.topMargin,
                                    otherParams.leftMargin + child.width,
                                    otherParams.topMargin + child.height
                                )
                                Rect.intersects(otherRect, newRect)
                            }

                            if (!hasOverlap) {
                                finalWidth = newWidth
                                finalHeight = newHeight
                                // Nastavení nových rozměrů, pokud nedojde k překryvu
                                params.width = newWidth
                                params.height = newHeight
                                currentTableToEdit.layoutParams = params
                                currentTableToEdit.requestLayout()
                            }
                            true
                        }

                        else -> false
                    }
                } else false
            }

            confirmTableChanges.setOnClickListener {
                tableEditMode = false
                helper.height = finalHeight
                helper.width = finalWidth
                helper.xPosition = finalX
                helper.yPosition = finalY

                confirmTableChanges.visibility = View.GONE
                saveButton.visibility = View.VISIBLE
                cancelButton.visibility = View.VISIBLE
                addTableButton.visibility = View.VISIBLE
                addSceneButton.visibility = View.VISIBLE
                addHelperButton.visibility = View.VISIBLE
                editModelScenesLayout.visibility = View.VISIBLE
            }
        }
    }

    private fun sceneOptionsPopup(){

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