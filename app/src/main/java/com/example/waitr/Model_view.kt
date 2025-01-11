package com.example.waitr

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
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
        val saveButton = editModelDialoge.findViewById<TextView>(R.id.save_model_edit)
        val cancelButton = editModelDialoge.findViewById<TextView>(R.id.cancel_model_edit)
        val addTableButton = editModelDialoge.findViewById<TextView>(R.id.add_table)
        val addSceneButton = editModelDialoge.findViewById<TextView>(R.id.add_scene)
        val addHelperButton = editModelDialoge.findViewById<TextView>(R.id.add_helper_shape)
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
            val tableName = tableNameInput.text.toString().trim()
            if (tableName.isEmpty()){
                Toast.makeText(dialog.context, "You must enter the name first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val randomID = UUID.randomUUID().toString()


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

            val textViewForScene = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(5, -5, 5, -5)
                }
                setAutoSizeTextTypeUniformWithConfiguration(1, 1000, 1, TypedValue.COMPLEX_UNIT_DIP)
                text = context.getString(R.string.scene1)
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

    private fun addHelperPopup(){

    }
    private fun sceneOptionsPopup(){

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