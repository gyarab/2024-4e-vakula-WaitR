package com.example.waitr

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

//nadefinovane objekty...

class Model_view : Fragment() {
    //promenne
    private var CompanyID: String? = null
    private lateinit var editButton: ImageButton
    private lateinit var helpButton: ImageButton
    private lateinit var currentScene: String
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