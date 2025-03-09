package com.example.waitr

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

//promenne
var CompanyID: String? = null

class Analytics : Fragment() {
    //globalni promenne
    

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
        // inicializace ui prvku
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_analytics, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance(companyId: String) =
            Analytics().apply {
                arguments = Bundle().apply {
                    putString(CompanyID, companyId)
                }
            }
    }
}