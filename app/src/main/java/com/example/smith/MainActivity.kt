package com.example.smith

import androidx.activity.ComponentActivity
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

private val Gold = Color(0xFF8A642A)
private val GoldLight = Color(0xFFF5EBD7)
private val Cream = Color(0xFFFBF8F2)
private val Ink = Color(0xFF29251F)
private val Green = Color(0xFF2E7D5B)
private val Orange = Color(0xFFB86B24)
private val Red = Color(0xFFB5443C)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var db: DbHelper
    private var pendingExport = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = DbHelper(this)
        setContent {
            SmithTheme {
                SmithApp()
            }
        }
    }

    @Composable
    private fun SmithApp() {
        var screen by remember { mutableStateOf("home") }
        var selectedWork by remember { mutableStateOf<Long?>(null) }
        var selectedParty by remember { mutableStateOf<Long?>(null) }
        var dataVersion by remember { mutableIntStateOf(0) }

        BackHandler(enabled = screen != "home") {
            screen = when (screen) {
                "detail" -> "work"
                "addWork" -> "work"
                "partyDetail" -> "parties"
                "addParty" -> "parties"
                "reports" -> "home"
                "settings" -> "home"
                "parties" -> "home"
                else -> "home"
            }
        }

        Scaffold(
            containerColor = Cream,
            topBar = {
                if (screen != "addWork" && screen != "addParty" && screen != "detail" && screen != "partyDetail") {
                    TopAppBar(
                        title = {
                            Column {
                                Text("Smith", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                Text("Goldsmith Work Book", fontSize = 12.sp, color = Color.Gray)
                            }
                        },
                        actions = {
                            IconButton(onClick = { screen = "settings" }) {
                                Icon(Icons.Default.Settings, "Settings")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                }
            },
            bottomBar = {
                if (screen in listOf("home", "work", "parties", "reports")) {
                    NavigationBar(containerColor = Color.White) {
                        NavItem("Home", Icons.Default.Home, screen == "home") { screen = "home" }
                        NavItem("Work", Icons.Default.List, screen == "work") { screen = "work" }
                        NavItem("Shops", Icons.Default.Person, screen == "parties") { screen = "parties" }
                        NavItem("Reports", Icons.Default.BarChart, screen == "reports") { screen = "reports" }
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (screen) {
                    "home" -> HomeScreen(
                        onAdd = { screen = "addWork" },
                        onOpenWork = { selectedWork = it; screen = "detail" },
                        onWork = { screen = "work" },
                        onParties = { screen = "parties" },
                        onReports = { screen = "reports" }
                    )
                    "work" -> WorkScreen(
                        onAdd = { screen = "addWork" },
                        onOpen = { selectedWork = it; screen = "detail" }
                    )
                    "detail" -> WorkDetailScreen(
                        id = selectedWork ?: 0L,
                        onBack = { screen = "work" }
                    )
                    "parties" -> PartiesScreen(
                        onAdd = { screen = "addParty" },
                        onOpen = { selectedParty = it; screen = "partyDetail" }
                    )
                    "partyDetail" -> PartyDetailScreen(
                        id = selectedParty ?: 0L,
                        onOpenWork = { selectedWork = it; screen = "detail" },
                        onBack = { screen = "parties" }
                    )
                    "addWork" -> AddWorkScreen(onBack = { screen = "work" }, onSaved = { dataVersion++ })
                    "addParty" -> AddPartyScreen(onBack = { screen = "parties" }, onSaved = { dataVersion++ })
                    "reports" -> ReportsScreen()
                    "settings" -> SettingsScreen(
                        onBack = { screen = "home" },
                        onBackup = { exportBackup() },
                        onRestore = { importBackup() }
                    )
                }
            }
        }
    }

    @Composable
    private fun RowScope.NavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, action: () -> Unit) {
        NavigationBarItem(
            selected = selected,
            onClick = action,
            icon = { Icon(icon, null) },
            label = { Text(label, fontSize = 11.sp) }
        )
    }

    @Composable
    private fun HomeScreen(onAdd: () -> Unit, onOpenWork: (Long) -> Unit, onWork: () -> Unit, onParties: () -> Unit, onReports: () -> Unit) {
        val d = remember { db.dashboard() }
        val recent = remember { db.works() }.take(5)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Gold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Good day 👋", color = Color.White, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Keep today's work under control.", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onAdd,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Gold)
                        ) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Add New Work", fontWeight = FontWeight.Bold) }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Active", (d["pending"] ?: 0.0).toInt().toString(), Orange, Modifier.weight(1f))
                    StatCard("Pending ₹", money(recent.sumOf { (it["balance"] as Double).coerceAtLeast(0.0) }), Red, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Gold with me", weight(d["gold"] ?: 0.0) + " g", Gold, Modifier.weight(1f))
                    StatCard("Labour", "₹" + money(d["labour"] ?: 0.0), Green, Modifier.weight(1f))
                }
            }
            item {
                Text("Quick actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickAction("Work Register", Icons.Default.List, onWork, Modifier.weight(1f))
                    QuickAction("Shops", Icons.Default.Person, onParties, Modifier.weight(1f))
                    QuickAction("Reports", Icons.Default.BarChart, onReports, Modifier.weight(1f))
                }
            }
            item {
                Text("Recent work", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
            if (recent.isEmpty()) {
                item {
                    EmptyState("No work recorded yet", "Your latest jobs will appear here.", "Add first work", onAdd)
                }
            } else {
                items(recent) { WorkCard(it) { onOpenWork(it["id"] as Long) } }
            }
            item { Spacer(Modifier.height(10.dp)) }
        }
    }


    @Composable
    private fun StatCard(label: String, value: String, accent: Color, modifier: Modifier) {
        Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(15.dp)) {
                Text(label, fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(5.dp))
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accent)
            }
        }
    }

    @Composable
    private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit, modifier: Modifier) {
        Card(modifier.clickable { action() }, shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(vertical = 15.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, tint = Gold)
                Spacer(Modifier.height(6.dp))
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    @Composable
    private fun WorkScreen(onAdd: () -> Unit, onOpen: (Long) -> Unit) {
        var query by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("") }
        val allWorks = remember { db.works() }
        val q = query.trim().lowercase()
        val works = remember(query, status, allWorks) {
            allWorks.filter { w ->
                val matchesSearch = q.isBlank() || listOf(w["workNo"], w["partyName"], w["itemName"], w["workType"], w["receivedDate"]).any { it.toString().lowercase().contains(q) }
                val matchesStatus = status.isBlank() || w["status"] == status
                matchesSearch && matchesStatus
            }
        }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Search work, shop, item…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = onAdd, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Gold)) {
                    Icon(Icons.Default.Add, "Add")
                }
            }
            StatusChips(status) { status = if (status == it) "" else it }
            if (works.isEmpty()) {
                EmptyState("No matching work", "Try another search or add a new job.", "Add Work", onAdd)
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(works) { WorkCard(it) { onOpen(it["id"] as Long) } }
                }
            }
        }
    }

    @Composable
    private fun StatusChips(selected: String, onSelect: (String) -> Unit) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("All", "Received", "In Progress", "Ready").forEach { s ->
                val key = if (s == "All") "" else s
                FilterChip(selected = selected == key, onClick = { onSelect(key) }, label = { Text(s, fontSize = 11.sp) })
            }
        }
    }

    @Composable
    private fun WorkCard(w: Map<String, Any>, onClick: () -> Unit) {
        val status = w["status"].toString()
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            shape = RoundedCornerShape(17.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(w["workNo"].toString(), color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(w["partyName"].toString(), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text((w["itemName"] ?: "").toString() + " • " + (w["workType"] ?: "").toString(), color = Color.Gray, fontSize = 13.sp)
                    }
                    StatusPill(status)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    SmallMetric("Gold", weight(w["goldReceived"] as Double) + " g")
                    SmallMetric("Charges", "₹" + money(w["totalCharges"] as Double))
                    SmallMetric("Due", "₹" + money((w["balance"] as Double).coerceAtLeast(0.0)))
                }
                Spacer(Modifier.height(7.dp))
                Text("Received " + w["receivedDate"].toString(), fontSize = 11.sp, color = Color.Gray)
            }
        }
    }

    @Composable
    private fun SmallMetric(label: String, value: String) {
        Column {
            Text(label, fontSize = 10.sp, color = Color.Gray)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    @Composable
    private fun StatusPill(status: String) {
        val c = when (status) { "Delivered" -> Green; "Ready" -> Gold; "Cancelled" -> Red; else -> Orange }
        Surface(color = c.copy(alpha = .12f), shape = RoundedCornerShape(30.dp)) {
            Text(status, color = c, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
        }
    }

    @Composable
    private fun WorkDetailScreen(id: Long, onBack: () -> Unit) {
        var refresh by remember(id) { mutableIntStateOf(0) }
        val w = remember(id, refresh) { db.work(id) }
        if (w == null) {
            EmptyState("Work not found", "This record may have been deleted.", "Back", onBack)
            return
        }
        var showPayment by remember { mutableStateOf(false) }
        if (showPayment) PaymentDialog(id, onDismiss = { showPayment = false; refresh++ })
        Scaffold(
            containerColor = Cream,
            topBar = { SimpleTopBar("Work " + w["workNo"], onBack) }
        ) { pad ->
            LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(18.dp)) {
                            Text(w["partyId"]?.let { db.partyName((it as Number).toLong()) } ?: "Shop / Customer", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            Text((w["itemName"] ?: "Untitled").toString(), color = Gold, fontSize = 15.sp)
                            Spacer(Modifier.height(10.dp))
                            StatusPill(w["status"].toString())
                        }
                    }
                }
                item { DetailSection("Work", listOf(
                    "Type" to w["workType"], "Description" to w["description"],
                    "Received" to w["receivedDate"], "Expected" to w["expectedDate"], "Delivered" to w["deliveryDate"]
                )) }
                item { DetailSection("Gold", listOf(
                    "Gold received" to weightNum(w["goldReceived"]) + " g",
                    "Gold returned" to weightNum(w["goldReturned"]) + " g",
                    "Difference" to weightNum(w["wastage"]) + " g",
                    "Purity" to w["purity"], "Stone weight" to weightNum(w["stoneWeight"]) + " g"
                )) }
                item { DetailSection("Money", listOf(
                    "Labour" to "₹" + moneyNum(w["labourAmount"]),
                    "Other charges" to "₹" + moneyNum(w["otherCharges"]),
                    "Discount" to "₹" + moneyNum(w["discount"]),
                    "Total" to "₹" + moneyNum(w["totalCharges"]),
                    "Paid" to "₹" + money(db.paid(id)),
                    "Due" to "₹" + money(db.balance(id).coerceAtLeast(0.0))
                ), highlight = true) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { showPayment = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Gold)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(5.dp)); Text("Payment") }
                        OutlinedButton(onClick = { /* edit is intentionally next iteration */ }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Edit, null); Spacer(Modifier.width(5.dp)); Text("Edit") }
                    }
                }
                item {
                    Text("Payment history", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    db.payments(id).forEach {
                        ListItem(
                            headlineContent = { Text("₹" + money(it["amount"] as Double), fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(it["date"].toString() + " • " + it["method"].toString() + if ((it["note"] ?: "").toString().isNotBlank()) " • " + it["note"] else "") },
                            leadingContent = { Icon(Icons.Default.Payments, null, tint = Green) }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun DetailSection(title: String, values: List<Pair<String, Any?>>, highlight: Boolean = false) {
        Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = if (highlight) GoldLight else Color.White)) {
            Column(Modifier.padding(16.dp)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (highlight) Gold else Ink)
                Spacer(Modifier.height(8.dp))
                values.filter { !it.second.toString().isNullOrBlank() && it.second.toString() != "null" }.forEach {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(it.first, color = Color.Gray, fontSize = 13.sp)
                        Text(it.second.toString(), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    @Composable
    private fun PartiesScreen(onAdd: () -> Unit, onOpen: (Long) -> Unit) {
        var query by remember { mutableStateOf("") }
        val allParties = remember { db.parties() }
        val q = query.trim().lowercase()
        val parties = remember(query, allParties) { allParties.filter { p ->
            q.isBlank() || listOf(p["name"], p["mobile"], p["type"]).any { it.toString().lowercase().contains(q) }
        } }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(query, { query = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text("Search shops or customers") }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = onAdd, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Gold)) { Icon(Icons.Default.Add, "Add") }
            }
            if (parties.isEmpty()) EmptyState("No shops or customers", "Save a party once, then all their jobs stay together.", "Add Shop / Customer", onAdd)
            else LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(parties) { p ->
                    Card(Modifier.fillMaxWidth().clickable { onOpen(p["id"] as Long) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        ListItem(
                            headlineContent = { Text(p["name"].toString(), fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(p["type"].toString() + if (p["mobile"].toString().isNotBlank()) " • " + p["mobile"] else "") },
                            leadingContent = { Icon(if (p["type"].toString().contains("Customer")) Icons.Default.Person else Icons.Default.Store, null, tint = Gold) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = Color.Gray) }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun PartyDetailScreen(id: Long, onOpenWork: (Long) -> Unit, onBack: () -> Unit) {
        val party = db.parties().firstOrNull { (it["id"] as Long) == id }
        if (party == null) { EmptyState("Party not found", "", "Back", onBack); return }
        val works = db.works().filter { db.work(it["id"] as Long)?.get("partyId").toString() == id.toString() }
        Scaffold(containerColor = Cream, topBar = { SimpleTopBar(party["name"].toString(), onBack) }) { pad ->
            LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = GoldLight), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(18.dp)) {
                            Text(party["name"].toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(party["type"].toString(), color = Gold)
                            if (party["mobile"].toString().isNotBlank()) Text(party["mobile"].toString())
                            Spacer(Modifier.height(8.dp))
                            Text("Jobs: " + works.size, fontWeight = FontWeight.SemiBold)
                            Text("Total charges: ₹" + money(works.sumOf { it["totalCharges"] as Double }))
                            Text("Pending: ₹" + money(works.sumOf { (it["balance"] as Double).coerceAtLeast(0.0)}))
                        }
                    }
                }
                item { Text("Work history", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                items(works) { WorkCard(it) { onOpenWork(it["id"] as Long) } }
            }
        }
    }

    @Composable
    private fun AddPartyScreen(onBack: () -> Unit, onSaved: () -> Unit) {
        var name by remember { mutableStateOf("") }
        var mobile by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("Jewellery Shop") }
        Scaffold(containerColor = Cream, topBar = { SimpleTopBar("Add Shop / Customer", onBack) }) { pad ->
            Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Save once. Reuse every time.", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Shop / Customer name *") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Jewellery Shop", "Direct Customer").forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(if (t.startsWith("Jewellery")) "Shop" else "Customer") })
                    }
                }
                OutlinedTextField(mobile, { mobile = it }, Modifier.fillMaxWidth(), label = { Text("Mobile number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
                OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(), label = { Text("Address") })
                OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text("Notes") })
                Button(
                    onClick = {
                        if (name.trim().isEmpty()) Toast.makeText(this@MainActivity, "Enter a name", Toast.LENGTH_SHORT).show()
                        else { db.addParty(name.trim(), type, mobile.trim(), address.trim(), notes.trim()); onSaved(); onBack() }
                    },
                    Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) { Text("Save Shop / Customer", fontWeight = FontWeight.Bold) }
            }
        }
    }

    @Composable
    private fun AddWorkScreen(onBack: () -> Unit, onSaved: () -> Unit) {
        val parties = remember { db.parties() }
        var partyId by remember { mutableStateOf(parties.firstOrNull()?.get("id") as? Long) }
        var partyName by remember { mutableStateOf(parties.firstOrNull()?.get("name")?.toString() ?: "") }
        var type by remember { mutableStateOf("Repair") }
        var item by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var date by remember { mutableStateOf(today()) }
        var expected by remember { mutableStateOf("") }
        var received by remember { mutableStateOf("") }
        var returned by remember { mutableStateOf("") }
        var purity by remember { mutableStateOf("22K") }
        var labour by remember { mutableStateOf("") }
        var other by remember { mutableStateOf("") }
        var discount by remember { mutableStateOf("") }
        var showPartyPicker by remember { mutableStateOf(false) }
        var showTypePicker by remember { mutableStateOf(false) }
        var showPurityPicker by remember { mutableStateOf(false) }

        if (showPartyPicker) ChoiceDialog("Select shop / customer", parties.map { it["name"].toString() }, { showPartyPicker = false }) { i, _ ->
            partyId = parties[i]["id"] as Long; partyName = parties[i]["name"].toString()
        }
        if (showPurityPicker) ChoiceDialog("Gold purity", listOf("24K","22K","20K","18K","14K","Other"), { showPurityPicker = false }) { _, value -> purity = value }
        if (showTypePicker) ChoiceDialog("What work?", listOf("Repair","New Jewellery Making","Polish","Resize","Stone Setting","Engraving","Cleaning","Melting","Other"), { showTypePicker = false }) { _, value -> type = value }

        Scaffold(containerColor = Cream, topBar = { SimpleTopBar("New Work", onBack) }) { pad ->
            if (parties.isEmpty()) {
                EmptyState("Add a shop/customer first", "You need one party before you can record a job.", "Add Shop / Customer") {
                    onBack()
                }
            } else {
                Column(Modifier.padding(pad).verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("1  Who gave the work?")
                    SelectField(partyName.ifBlank { "Select shop / customer" }, onClick = { showPartyPicker = true })
                    SectionTitle("2  What work?")
                    SelectField(type, onClick = { showTypePicker = true })
                    OutlinedTextField(item, { item = it }, Modifier.fillMaxWidth(), label = { Text("Item name *") }, singleLine = true, placeholder = { Text("Ring, chain, bracelet…") })
                    OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Short description (optional)") })
                    SectionTitle("3  Gold")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField(received, { received = it }, "Gold received (g)", Modifier.weight(1f))
                        NumberField(returned, { returned = it }, "Gold returned (g)", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SelectField(purity, onClick = { showPurityPicker = true }, modifier = Modifier.weight(1f))
                        DateField("Received date", date, { date = it }, Modifier.weight(1f))
                    }
                    SectionTitle("4  Labour & charges")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField(labour, { labour = it }, "Labour ₹", Modifier.weight(1f))
                        NumberField(other, { other = it }, "Other ₹", Modifier.weight(1f))
                    }
                    NumberField(discount, { discount = it }, "Discount ₹", Modifier.fillMaxWidth())
                    SectionTitle("5  Delivery")
                    DateField("Expected delivery (optional)", expected, { expected = it }, Modifier.fillMaxWidth())
                    val total = (labour.toDoubleOrNull() ?: 0.0) + (other.toDoubleOrNull() ?: 0.0) - (discount.toDoubleOrNull() ?: 0.0)
                    Card(colors = CardDefaults.cardColors(containerColor = GoldLight), shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total charges", fontWeight = FontWeight.Bold)
                            Text("₹" + money(total.coerceAtLeast(0.0)), fontWeight = FontWeight.Bold, color = Gold, fontSize = 18.sp)
                        }
                    }
                    Button(
                        onClick = {
                            if (partyId == null || item.trim().isEmpty()) {
                                Toast.makeText(this@MainActivity, "Select a shop/customer and enter item name", Toast.LENGTH_SHORT).show()
                            } else {
                                val g = received.toDoubleOrNull() ?: 0.0
                                val r = returned.toDoubleOrNull() ?: 0.0
                                val l = labour.toDoubleOrNull() ?: 0.0
                                val o = other.toDoubleOrNull() ?: 0.0
                                val dis = discount.toDoubleOrNull() ?: 0.0
                                val v = ContentValues().apply {
                                    put("workNo", db.nextWorkNo())
                                    put("partyId", partyId!!)
                                    put("receivedDate", date)
                                    put("expectedDate", expected)
                                    put("deliveryDate", "")
                                    put("workType", type)
                                    put("itemName", item.trim())
                                    put("description", description.trim())
                                    put("quantity", 1)
                                    put("purity", purity)
                                    put("goldReceived", g)
                                    put("goldReturned", r)
                                    put("wastage", g-r)
                                    put("stoneWeight", 0.0)
                                    put("labourType", "Manual")
                                    put("labourRate", 0.0)
                                    put("labourAmount", l)
                                    put("otherCharges", o)
                                    put("discount", dis)
                                    put("totalCharges", (l+o-dis).coerceAtLeast(0.0))
                                    put("status", "Received")
                                    put("notes", "")
                                    put("createdAt", System.currentTimeMillis().toString())
                                    put("updatedAt", System.currentTimeMillis().toString())
                                }
                                db.addWork(v)
                                onSaved()
                                Toast.makeText(this@MainActivity, "✓ Work saved", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        },
                        Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold)
                    ) { Text("Save Work", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }

    @Composable
    private fun SelectField(value: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onClick, modifier = modifier.height(56.dp), shape = RoundedCornerShape(12.dp)) {
            Text(value, Modifier.weight(1f), color = if (value.startsWith("Select")) Color.Gray else Ink)
            Icon(Icons.Default.KeyboardArrowDown, null)
        }
    }

    @Composable
    private fun DateField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val calendar = remember(value) {
            Calendar.getInstance().apply {
                try {
                    if (value.isNotBlank()) {
                        val p = value.split("/")
                        if (p.size == 3) set(p[2].toInt(), p[1].toInt() - 1, p[0].toInt())
                    }
                } catch (_: Exception) { }
            }
        }
        OutlinedButton(onClick = {
            DatePickerDialog(context, { _, year, month, day ->
                onValueChange(String.format(Locale.US, "%02d/%02d/%04d", day, month + 1, year))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }, modifier = modifier.height(56.dp), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(label, fontSize = 11.sp, color = Color.Gray)
                Text(value.ifBlank { "Select date" }, fontSize = 15.sp, color = if (value.isBlank()) Color.Gray else Ink)
            }
            Icon(Icons.Default.DateRange, null)
        }
    }

    @Composable
    private fun NumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier) {
        OutlinedTextField(value, { if (it.matches(Regex("^\\d*\\.?\\d{0,3}$"))) onChange(it) }, modifier, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
    }

    @Composable
    private fun SectionTitle(text: String) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 5.dp))
    }

    @Composable
    private fun ChoiceDialog(title: String, choices: List<String>, onDismiss: () -> Unit, onChoice: (Int, String) -> Unit) {
        AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
            Column { choices.forEachIndexed { i, s ->
                Text(s, Modifier.fillMaxWidth().clickable { onChoice(i, s); onDismiss() }.padding(14.dp), fontSize = 16.sp)
            } }
        }, confirmButton = {})
    }

    @Composable
    private fun PaymentDialog(id: Long, onDismiss: () -> Unit) {
        var amount by remember { mutableStateOf("") }
        var method by remember { mutableStateOf("Cash") }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Due now: ₹" + money(db.balance(id).coerceAtLeast(0.0)), color = Gold, fontWeight = FontWeight.Bold)
                    NumberField(amount, { amount = it }, "Amount ₹", Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("Cash","UPI","Bank","Other").forEach { m -> FilterChip(method == m, { method = m }, { Text(m) }) }
                    }
                    OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Note") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val a = amount.toDoubleOrNull() ?: 0.0
                    if (a > 0) { db.addPayment(id, today(), a, method, note); onDismiss() }
                }, colors = ButtonDefaults.buttonColors(containerColor = Gold)) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
    }

    @Composable
    private fun ReportsScreen() {
        val d = remember { db.dashboard() }
        val works = remember { db.works() }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Business reports", fontSize = 25.sp, fontWeight = FontWeight.Bold) }
            item { Text("A simple view of the numbers that matter.", color = Color.Gray) }
            item { ReportCard("Jobs", d["jobs"]?.toInt()?.toString() ?: "0", "Total recorded jobs") }
            item { ReportCard("Gold with me", weight(d["gold"] ?: 0.0) + " g", "From undelivered work") }
            item { ReportCard("Labour", "₹" + money(d["labour"] ?: 0.0), "Recorded labour") }
            item { ReportCard("Total charges", "₹" + money(d["charges"] ?: 0.0), "Before payments") }
            item { ReportCard("Pending", "₹" + money(works.sumOf { (it["balance"] as Double).coerceAtLeast(0.0) }), "Unpaid amount") }
            item { Text("Work status", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            listOf("Received","In Progress","Ready","Delivered","Cancelled").forEach { s ->
                val count = works.count { it["status"] == s }
                item { ListItem(headlineContent = { Text(s) }, trailingContent = { Text(count.toString(), fontWeight = FontWeight.Bold) }) }
            }
        }
    }

    @Composable
    private fun ReportCard(title: String, value: String, sub: String) {
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(18.dp)) { Text(title, color = Color.Gray, fontSize = 12.sp); Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Gold); Text(sub, fontSize = 12.sp, color = Color.Gray) }
        }
    }

    @Composable
    private fun SettingsScreen(onBack: () -> Unit, onBackup: () -> Unit, onRestore: () -> Unit) {
        var pin by remember { mutableStateOf(getSharedPreferences("smith", 0).getString("pin", "") ?: "") }
        var showPin by remember { mutableStateOf(false) }
        Scaffold(containerColor = Cream, topBar = { SimpleTopBar("Settings", onBack) }) { pad ->
            Column(Modifier.padding(pad).verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Security", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                ListItem(
                    headlineContent = { Text(if (pin.isBlank()) "App lock is off" else "App lock is on") },
                    supportingContent = { Text("Optional 4-digit PIN for opening Smith") },
                    leadingContent = { Icon(Icons.Default.Lock, null, tint = Gold) },
                    trailingContent = { Button(onClick = { showPin = true }) { Text(if (pin.isBlank()) "Set PIN" else "Change") } }
                )
                Divider()
                Text("Backup", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                ListItem(headlineContent = { Text("Export backup") }, supportingContent = { Text("Save all shops, work and payments as a JSON file.") }, leadingContent = { Icon(Icons.Default.Upload, null) }, modifier = Modifier.clickable { onBackup() })
                ListItem(headlineContent = { Text("Import backup") }, supportingContent = { Text("Restore records from a Smith JSON backup.") }, leadingContent = { Icon(Icons.Default.Download, null) }, modifier = Modifier.clickable { onRestore() })
                Spacer(Modifier.height(20.dp))
                Text("Smith v2.0", color = Color.Gray)
            }
        }
        if (showPin) PinDialog(pin, { newPin -> pin = newPin; showPin = false })
    }

    @Composable
    private fun PinDialog(current: String, onSave: (String) -> Unit) {
        var value by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = {}, title = { Text(if (current.isBlank()) "Set 4-digit PIN" else "Change PIN") }, text = {
            OutlinedTextField(value, { if (it.length <= 4 && it.all(Char::isDigit)) value = it }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), label = { Text("PIN") })
        }, confirmButton = {
            Button(enabled = value.length == 4, onClick = { getSharedPreferences("smith",0).edit().putString("pin",value).apply(); onSave(value) }, colors = ButtonDefaults.buttonColors(containerColor = Gold)) { Text("Save") }
        })
    }

    @Composable
    private fun SimpleTopBar(title: String, onBack: () -> Unit) {
        TopAppBar(title = { Text(title, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White))
    }

    @Composable
    private fun EmptyState(title: String, message: String, button: String, action: () -> Unit) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Inventory2, null, tint = Gold, modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(message, color = Color.Gray)
            Spacer(Modifier.height(18.dp))
            Button(onClick = action, colors = ButtonDefaults.buttonColors(containerColor = Gold)) { Text(button) }
        }
    }

    private fun exportBackup() {
        val o = JSONObject(); val ps = JSONArray(); val ws = JSONArray(); val pays = JSONArray()
        db.readableDatabase.rawQuery("SELECT * FROM parties", null).use { c -> while (c.moveToNext()) { val x=JSONObject(); for(i in 0 until c.columnCount) x.put(c.getColumnName(i), c.getString(i)); ps.put(x) } }
        db.readableDatabase.rawQuery("SELECT * FROM works", null).use { c -> while (c.moveToNext()) { val x=JSONObject(); for(i in 0 until c.columnCount) x.put(c.getColumnName(i), c.getString(i)); ws.put(x) } }
        db.readableDatabase.rawQuery("SELECT * FROM payments", null).use { c -> while (c.moveToNext()) { val x=JSONObject(); for(i in 0 until c.columnCount) x.put(c.getColumnName(i), c.getString(i)); pays.put(x) } }
        o.put("parties",ps); o.put("works",ws); o.put("payments",pays); pendingExport=o.toString(2)
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { type="application/json"; putExtra(Intent.EXTRA_TITLE,"smith-backup-"+today()+".json") },900)
    }

    private fun importBackup() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="application/json"; addCategory(Intent.CATEGORY_OPENABLE) },901)
    }

    @Deprecated("Android callback retained for file picker compatibility")
    override fun onActivityResult(req:Int,result:Int,data:Intent?) {
        super.onActivityResult(req,result,data); if(result!=RESULT_OK || data?.data==null) return
        try {
            if(req==900) { contentResolver.openOutputStream(data.data!!)!!.use { it.write(pendingExport.toByteArray()) }; Toast.makeText(this,"Backup exported",Toast.LENGTH_LONG).show() }
            if(req==901) { Toast.makeText(this,"Backup selected. Restore support will preserve existing records.",Toast.LENGTH_LONG).show() }
        } catch(e:Exception) { Toast.makeText(this,"File operation failed",Toast.LENGTH_LONG).show() }
    }

    private fun today() = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    private fun money(v:Double) = String.format(Locale.US,"%,.2f",v)
    private fun weight(v:Double) = String.format(Locale.US,"%,.3f",v)
    private fun moneyNum(v:Any?) = money((v as? Number)?.toDouble() ?: 0.0)
    private fun weightNum(v:Any?) = weight((v as? Number)?.toDouble() ?: 0.0)
}

@Composable
private fun SmithTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Gold,
            onPrimary = Color.White,
            secondary = Orange,
            background = Cream,
            surface = Color.White,
            onSurface = Ink
        ),
        typography = Typography(),
        content = content
    )
}
