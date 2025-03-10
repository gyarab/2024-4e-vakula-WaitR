package com.example.waitr

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Analytics : Fragment() {
    //globalni promenne
    var CompanyID: String? = null
    private val db = FirebaseDatabase.getInstance("https://waitr-dee9a-default-rtdb.europe-west1.firebasedatabase.app/").reference // Using Realtime Database reference
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser
    private val userId = currentUser?.uid
    private val sortedTablesData = mutableListOf<TablesandItems_data>()
    private val sortedItemsData = mutableListOf<TablesandItems_data>()
    private val sortedByTablesUsersData = mutableListOf<Users_data>()
    private val sortedByActivityUsersData = mutableListOf<Users_data>()
    private val allTablesList = mutableListOf<Table>()
    private val allMenuItemsList = mutableListOf<MenuItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            CompanyID = it.getString(CompanyID)
        }
        fetchAllMenuItems {}
        fetchAllTables {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // inicializace ui prvku

        // metoda pro ziskani dat a vykresleni
        fetchAnalyticsData {
            drawAnalytics()
        }
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_analytics, container, false)
    }
    //metoda pro nakreslnei ui elementu do layoutu
    private fun drawAnalytics(){

    }
    //metoda na nacteni dat do listu
    private fun fetchAnalyticsData(onComplete: () -> Unit){
        val analyticsRef = CompanyID?.let { db.child("companies").child(it).child("Analytics") }
        val tablesData = mutableListOf<TablesandItems_data>()
        val itemsData = mutableListOf<TablesandItems_data>()
        val usersData = mutableListOf<Users_data>()

        analyticsRef?.child("tables")?.get()?.addOnSuccessListener { snapshot ->
            snapshot.children.forEach { tableSnapshot ->
                val id = tableSnapshot.key ?: return@forEach
                val numberOfTimesServed = tableSnapshot.child("numberOfTimesServed").getValue(Int::class.java) ?: 0
                tablesData.add(TablesandItems_data(id, numberOfTimesServed))
            }
        }?.addOnFailureListener { error ->
            Log.e("Firebase", "Chyba při načítání tables: ${error.message}")
        }

        analyticsRef?.child("items")?.get()?.addOnSuccessListener { snapshot ->
            snapshot.children.forEach { itemSnapshot ->
                val id = itemSnapshot.key ?: return@forEach
                val numberOfTimesServed = itemSnapshot.child("numberOfTimesServed").getValue(Int::class.java) ?: 0
                itemsData.add(TablesandItems_data(id, numberOfTimesServed))
            }
        }?.addOnFailureListener { error ->
            Log.e("Firebase", "Chyba při načítání items: ${error.message}")
        }

        analyticsRef?.child("users")?.get()?.addOnSuccessListener { snapshot ->
            snapshot.children.forEach { userSnapshot ->
                val id = userSnapshot.key ?: return@forEach
                val numberOfServedTables = userSnapshot.child("numberOfServedTables").getValue(Int::class.java) ?: 0
                val activity = userSnapshot.child("activity").getValue(Int::class.java) ?: 0
                usersData.add(Users_data(id, numberOfServedTables, activity))
            }
        }?.addOnFailureListener { error ->
            Log.e("Firebase", "Chyba při načítání users: ${error.message}")
        }
        sortedTablesData.clear()
        sortedTablesData.addAll(tablesData.sortedByDescending { it.numberOfTimesServed })
        sortedItemsData.clear()
        sortedItemsData.addAll(itemsData.sortedByDescending { it.numberOfTimesServed })
        sortedByTablesUsersData.clear()
        sortedByTablesUsersData.addAll(usersData.sortedByDescending { it.numberOfServedTables })
        sortedByActivityUsersData.clear()
        sortedByActivityUsersData.addAll(usersData.sortedByDescending { it.activity })
        onComplete()
    }

    private fun fetchAllMenuItems(onComplete: () -> Unit) {
        val companyMenuRef = CompanyID?.let {
            db.child("companies").child(it).child("Menu")
        }

        companyMenuRef?.get()?.addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val fetchedMenu = snapshot.getValue(MenuGroup::class.java)
                if (fetchedMenu != null) {
                    allMenuItemsList.clear() // Vyčištění seznamu před načtením

                    allMenuItemsList.addAll(fetchedMenu.items)
                    fetchedMenu.subGroups.forEach { subGroup ->
                        recursiveMenuBrowse(subGroup)
                    }

                    onComplete()
                } else {
                    Toast.makeText(context, "Failed to parse menu data.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Menu not found.", Toast.LENGTH_SHORT).show()
            }
        }?.addOnFailureListener { error ->
            Toast.makeText(context, "Error loading menu: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun recursiveMenuBrowse(menuGroup: MenuGroup) {
        allMenuItemsList.addAll(menuGroup.items)
        menuGroup.subGroups.forEach { subGroup ->
            recursiveMenuBrowse(subGroup)
        }
    }

    private fun fetchAllTables(onComplete: () -> Unit){
        val companyMenuRef = CompanyID?.let {
            db.child("companies").child(it).child("Model")
        }

        companyMenuRef?.get()?.addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val fetchedModel = snapshot.getValue(Model::class.java)
                if (fetchedModel != null) {
                    allTablesList.clear() // Vyčištění seznamu před načtením
                    fetchedModel.listOfScenes.forEach { modelScene ->
                        modelScene.listOfTables.forEach { table ->
                            allTablesList.add(table)
                        }
                    }
                    onComplete()
                } else {
                    Toast.makeText(context, "Failed to parse menu data.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Menu not found.", Toast.LENGTH_SHORT).show()
            }
        }?.addOnFailureListener { error ->
            Toast.makeText(context, "Error loading menu: ${error.message}", Toast.LENGTH_SHORT).show()
        }
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