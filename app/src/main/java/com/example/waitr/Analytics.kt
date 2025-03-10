package com.example.waitr

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

data class X(
    val id: String,
    val name: String
)

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
    private val allUsersList = mutableListOf<X>()
    private lateinit var analyticsLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            CompanyID = it.getString(CompanyID)
        }
        fetchAllMenuItems {}
        fetchAllTables {}
        fetchAllUsers {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_analytics, container, false)
        // inicializace ui prvku
        analyticsLayout = view.findViewById(R.id.analytics_layout)
        // metoda pro ziskani dat a vykresleni
        fetchAnalyticsData {
            drawAnalytics()
        }
        // Inflate the layout for this fragment
        return view
    }

    //metoda pro nakreslnei ui elementu do layoutu
    private fun drawAnalytics(){
        analyticsLayout.removeAllViews()

        val tableAnalyticsLayout = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        val tableAnalyticsTextview = TextView(requireContext()).apply {
            text = "Top 5 most used tables:"
            textSize = 30f
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val tableBarChart = drawTableBarChard()
        tableAnalyticsLayout.addView(tableAnalyticsTextview)
        tableAnalyticsLayout.addView(tableBarChart)
        analyticsLayout.addView(tableAnalyticsLayout)

        val itemAnalyticsLayout = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        val itemAnalyticsTextview = TextView(requireContext()).apply {
            text = "Top 5 most popular items:"
            textSize = 30f
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val itemBarChard = drawItemBarChard()
        itemAnalyticsLayout.addView(itemAnalyticsTextview)
        itemAnalyticsLayout.addView(itemBarChard)
        analyticsLayout.addView(itemAnalyticsLayout)

        val userServedAnalyticsLayout = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        val userServedAnalyticsTextview = TextView(requireContext()).apply {
            text = "Top 5 users with most served tables:"
            textSize = 30f
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val userServedBarChard = drawUserServedBarChard()
        userServedAnalyticsLayout.addView(userServedAnalyticsTextview)
        userServedAnalyticsLayout.addView(userServedBarChard)
        analyticsLayout.addView(userServedAnalyticsLayout)

        val userActivityAnalyticsLayout = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        val userActivityAnalyticsTextview = TextView(requireContext()).apply {
            text = "Top 5 most active users:"
            textSize = 30f
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val userActivityBarChard = drawUserActivityBarChard()
        userActivityAnalyticsLayout.addView(userActivityAnalyticsTextview)
        userActivityAnalyticsLayout.addView(userActivityBarChard)
        analyticsLayout.addView(userActivityAnalyticsLayout)
    }

    private fun drawTableBarChard(): BarChart {
        val tableBarChart = BarChart(context)
        tableBarChart.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            600
        )
        val top5Tables = sortedTablesData.take(5)

        val tableEntries = top5Tables.mapIndexed { index, table ->
            BarEntry(index.toFloat(), table.numberOfTimesServed.toFloat())
        }

        val tableDataSet = BarDataSet(tableEntries, "Počet návštěv").apply {
            color = Color.rgb(173, 216, 230)
            valueTextSize = 12f
            valueTextColor = Color.BLACK
            setDrawValues(true)
        }

        val tableBarData = BarData(tableDataSet).apply {
            barWidth = 0.7f
        }

        tableBarChart.data = tableBarData

        val xAxis = tableBarChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(top5Tables.map { findNameOfTheTable(it.id) })
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            labelCount = top5Tables.size
        }

        val maxVisits = top5Tables.maxOfOrNull { it.numberOfTimesServed }?.toFloat() ?: 10f
        val yAxis = tableBarChart.axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = maxVisits + (maxVisits * 0.1f)
            granularity = when {
                maxVisits < 10 -> 1f
                maxVisits < 50 -> 5f
                else -> 10f
            }
            setDrawGridLines(true)
        }

        tableBarChart.axisRight.isEnabled = false

        tableBarChart.legend.isEnabled = false

        tableBarChart.animateY(1000)

        tableBarChart.invalidate()
        return tableBarChart
    }

    private fun drawItemBarChard(): BarChart {
        val itemBarChart = BarChart(context)
        itemBarChart.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            600
        )
        val top5Items = sortedItemsData.take(5)

        val itemEntries = top5Items.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.numberOfTimesServed.toFloat())
        }

        val itemDataSet = BarDataSet(itemEntries, "Počet návštěv").apply {
            color = Color.rgb(173, 216, 230)
            valueTextSize = 12f
            valueTextColor = Color.BLACK
            setDrawValues(true)
        }

        val itemBarData = BarData(itemDataSet).apply {
            barWidth = 0.7f
        }

        itemBarChart.data = itemBarData

        val xAxis = itemBarChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(top5Items.map { findNameOfTheItems(it.id) })
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            labelCount = top5Items.size
        }

        val maxVisits = top5Items.maxOfOrNull { it.numberOfTimesServed }?.toFloat() ?: 10f
        val yAxis = itemBarChart.axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = maxVisits + (maxVisits * 0.1f)
            granularity = when {
                maxVisits < 10 -> 1f
                maxVisits < 50 -> 5f
                else -> 10f
            }
            setDrawGridLines(true)
        }

        itemBarChart.axisRight.isEnabled = false

        itemBarChart.legend.isEnabled = false

        itemBarChart.animateY(1000)

        itemBarChart.invalidate()
        return itemBarChart
    }

    private fun drawUserServedBarChard(): BarChart {
        val usersServedBarChart = BarChart(context)
        usersServedBarChart.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            600
        )
        val top5userServed = sortedByTablesUsersData.take(5)

        val userServedEntries = top5userServed.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.numberOfServedTables.toFloat())
        }

        val userServedDataSet = BarDataSet(userServedEntries, "Počet návštěv").apply {
            color = Color.rgb(173, 216, 230)
            valueTextSize = 12f
            valueTextColor = Color.BLACK
            setDrawValues(true)
        }

        val userServedBarData = BarData(userServedDataSet).apply {
            barWidth = 0.7f
        }

        usersServedBarChart.data = userServedBarData

        val xAxis = usersServedBarChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(top5userServed.map { findNameOfTheUser(it.id) })
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            labelCount = top5userServed.size
        }

        val maxVisits = top5userServed.maxOfOrNull { it.numberOfServedTables }?.toFloat() ?: 10f
        val yAxis = usersServedBarChart.axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = maxVisits + (maxVisits * 0.1f)
            granularity = when {
                maxVisits < 10 -> 1f
                maxVisits < 50 -> 5f
                else -> 10f
            }
            setDrawGridLines(true)
        }

        usersServedBarChart.axisRight.isEnabled = false

        usersServedBarChart.legend.isEnabled = false

        usersServedBarChart.animateY(1000)

        usersServedBarChart.invalidate()
        return usersServedBarChart
    }

    private fun drawUserActivityBarChard(): BarChart {
        val usersActivityBarChart = BarChart(context)
        usersActivityBarChart.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            600
        )
        val top5userActivity = sortedByActivityUsersData.take(5)

        val userActivityEntries = top5userActivity.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.activity.toFloat())
        }

        val usersActivityDataSet = BarDataSet(userActivityEntries, "Počet návštěv").apply {
            color = Color.rgb(173, 216, 230)
            valueTextSize = 12f
            valueTextColor = Color.BLACK
            setDrawValues(true)
        }

        val usersActivityBarData = BarData(usersActivityDataSet).apply {
            barWidth = 0.7f
        }

        usersActivityBarChart.data = usersActivityBarData

        val xAxis = usersActivityBarChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(top5userActivity.map { findNameOfTheUser(it.id) })
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            labelCount = top5userActivity.size
        }

        val maxVisits = top5userActivity.maxOfOrNull { it.activity }?.toFloat() ?: 10f
        val yAxis = usersActivityBarChart.axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = maxVisits + (maxVisits * 0.1f)
            granularity = when {
                maxVisits < 10 -> 1f
                maxVisits < 50 -> 5f
                else -> 10f
            }
            setDrawGridLines(true)
        }

        usersActivityBarChart.axisRight.isEnabled = false

        usersActivityBarChart.legend.isEnabled = false

        usersActivityBarChart.animateY(1000)

        usersActivityBarChart.invalidate()
        return usersActivityBarChart
    }

    private fun findNameOfTheTable(id: String): String? {
        allTablesList.forEach { table ->
            if (table.id.equals(id)) return table.name
        }
        return null
    }
    private fun findNameOfTheItems(id: String): String? {
        allMenuItemsList.forEach { menuItem ->
            if (menuItem.id.equals(id)) return menuItem.name
        }
        return null
    }
    private fun findNameOfTheUser(id: String): String? {
        allUsersList.forEach { user ->
            if (user.id.equals(id)) return user.name
        }
        return null
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
                val numberOfTimesServed = tableSnapshot.child("numberOfServedTimes").getValue(Int::class.java) ?: 0
                tablesData.add(TablesandItems_data(id, numberOfTimesServed))
            }
            sortedTablesData.clear()
            sortedTablesData.addAll(tablesData.sortedByDescending { it.numberOfTimesServed })
        }?.addOnFailureListener { error ->
            Log.e("Firebase", "Chyba při načítání tables: ${error.message}")
        }

        analyticsRef?.child("items")?.get()?.addOnSuccessListener { snapshot ->
            snapshot.children.forEach { itemSnapshot ->
                val id = itemSnapshot.key ?: return@forEach
                val numberOfTimesServed = itemSnapshot.child("numberOfServedTimes").getValue(Int::class.java) ?: 0
                itemsData.add(TablesandItems_data(id, numberOfTimesServed))
            }
            sortedItemsData.clear()
            sortedItemsData.addAll(itemsData.sortedByDescending { it.numberOfTimesServed })
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
            sortedByTablesUsersData.clear()
            sortedByTablesUsersData.addAll(usersData.sortedByDescending { it.numberOfServedTables })
            sortedByActivityUsersData.clear()
            sortedByActivityUsersData.addAll(usersData.sortedByDescending { it.activity })
            onComplete()
        }?.addOnFailureListener { error ->
            Log.e("Firebase", "Chyba při načítání users: ${error.message}")
        }
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

    private fun fetchAllUsers(onComplete: () -> Unit){
        val usersRef = CompanyID?.let {
            db.child("companies").child(it).child("users")
        }

        usersRef?.get()?.addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                snapshot.children.forEach { userSnapshot ->
                    val id = userSnapshot.key ?: return@forEach
                    val username = userSnapshot.child("username").getValue(String::class.java) ?: ""
                    allUsersList.add(X(id, username))
                }
                onComplete()
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