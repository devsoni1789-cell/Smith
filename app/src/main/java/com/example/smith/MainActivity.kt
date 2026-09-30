package com.example.smith

import android.app.*
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private lateinit var db: DbHelper
    private lateinit var root: LinearLayout
    private val light = Color.rgb(248, 245, 241)
    private val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val today get() = fmt.format(Date())
    private val dp get() = resources.displayMetrics.density

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = DbHelper(this)
        showHome()
    }

    private fun tv(text:String,size:Float=16f,bold:Boolean=false):TextView = TextView(this).apply {
        this.text=text; textSize=size; setTextColor(Color.rgb(35,35,35))
        if(bold) setTypeface(null,android.graphics.Typeface.BOLD)
        setPadding((16*dp).toInt(),(10*dp).toInt(),(16*dp).toInt(),(10*dp).toInt())
    }
    private fun btn(text:String,onClick:()->Unit):Button = Button(this).apply {
        this.text=text; setOnClickListener{onClick()}; isAllCaps=false; minHeight=(48*dp).toInt()
    }
    private fun field(hint:String,value:String=""):EditText = EditText(this).apply {
        this.hint=hint; setText(value); textSize=16f
        setPadding((12*dp).toInt(),0,(12*dp).toInt(),0)
    }
    private fun base(title:String) {
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(light)}
        val bar=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        bar.setBackgroundColor(Color.WHITE)
        bar.addView(tv(title,21f,true),LinearLayout.LayoutParams(0,64.dp(),1f))
        bar.addView(btn("⌕"){showSearch()},LinearLayout.LayoutParams(60.dp(),64.dp()))
        root.addView(bar); setContentView(root)
    }
    private fun LinearLayout.add(v:View,h:Int=-2){addView(v,LinearLayout.LayoutParams(-1,h))}
    private fun Int.dp():Int=(this*dp).toInt()
    private fun scroll(content:View)=ScrollView(this).apply{addView(content)}

    private fun bottom():LinearLayout {
        val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setBackgroundColor(Color.WHITE)}
        listOf("Home","Work","Parties","Reports").forEach{label->
            nav.addView(btn(label){when(label){"Home"->showHome();"Work"->showWorks();"Parties"->showParties();else->showReports()}},
                LinearLayout.LayoutParams(0,58.dp(),1f))
        }
        return nav
    }

    private fun showHome() {
        base("Smith • Goldsmith Work Book")
        val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val d=db.dashboard()
        body.add(tv("Business overview",20f,true))
        body.add(tv("Active jobs: ${d["pending"]?.toInt()}
Gold currently recorded with me: ${moneyWeight(d["gold"]?:0.0)} g
Total labour recorded: ₹${money(d["labour"]?:0.0)}",17f))
        body.add(btn("+  New Work"){showWorkDialog(null)})
        body.add(btn("Work Register"){showWorks()})
        body.add(btn("Customers / Jewellery Shops"){showParties()})
        body.add(btn("Reports"){showReports()})
        body.add(btn("Backup / Restore"){backupDialog()})
        root.add(scroll(body),LinearLayout.LayoutParams(-1,0,1f));root.add(bottom())
    }

    private fun showWorks(search:String="",status:String="") {
        base("Work Register")
        val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        body.add(btn("+ New Work"){showWorkDialog(null)})
        val s=field("Search shop, customer, work no, item…",search)
        body.add(s,58.dp()); body.add(btn("Search"){showWorks(s.text.toString(),status)})
        if(status.isNotBlank()) body.add(tv("Filter: ${status}",16f,true))
        db.works(search,status).forEach{w->
            val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.WHITE)}
            card.add(tv("${w["workNo"]}  •  ${w["partyName"]}",18f,true))
            card.add(tv("${w["itemName"]}  |  ${w["workType"]}
${w["receivedDate"]}  |  ${w["status"]}
Gold: ${moneyWeight(w["goldReceived"] as Double)} g  |  Charges: ₹${money(w["totalCharges"] as Double)}  |  Balance: ₹${money(w["balance"] as Double)}"))
            card.setOnClickListener{showWorkDetails(w["id"] as Long)}
            body.add(card,LinearLayout.LayoutParams(-1,-2).apply{setMargins(8.dp(),6.dp(),8.dp(),6.dp())})
        }
        root.add(scroll(body),LinearLayout.LayoutParams(-1,0,1f));root.add(bottom())
    }

    private fun showWorkDetails(id:Long) {
        val w=db.work(id) ?: return
        val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val party=db.partyName((w["partyId"] as Number).toLong())
        body.add(tv("${w["workNo"]} • ${party}",22f,true))
        body.add(tv("Status: ${w["status"]}
Received: ${w["receivedDate"]}
Expected: ${w["expectedDate"] ?: "-"}
Delivered: ${w["deliveryDate"] ?: "-"}

Item: ${w["itemName"]}
Type: ${w["workType"]}
Description: ${w["description"] ?: "-"}
Quantity: ${w["quantity"]}
Purity: ${w["purity"] ?: "-"}

Gold received: ${moneyWeight((w["goldReceived"] as Number).toDouble())} g
Gold returned: ${moneyWeight((w["goldReturned"] as Number).toDouble())} g
Wastage/difference: ${moneyWeight((w["wastage"] as Number).toDouble())} g
Stone weight: ${moneyWeight((w["stoneWeight"] as Number).toDouble())} g

Labour: ₹${money((w["labourAmount"] as Number).toDouble())}
Other charges: ₹${money((w["otherCharges"] as Number).toDouble())}
Discount: ₹${money((w["discount"] as Number).toDouble())}
Total charges: ₹${money((w["totalCharges"] as Number).toDouble())}
Paid: ₹${money(db.paid(id))}
Balance: ₹${money(db.balance(id))}",16f))
        body.add(btn("Add Payment"){paymentDialog(id)})
        body.add(btn("Edit Work"){showWorkDialog(id)})
        body.add(btn("Delete Work"){confirmDelete(id)})
        body.add(tv("Payment history",18f,true))
        db.payments(id).forEach{p->body.add(tv("${p["date"]} • ₹${money(p["amount"] as Double)} • ${p["method"]}
${p["note"]}"))}
        base("Work Details"); root.add(scroll(body),LinearLayout.LayoutParams(-1,0,1f));root.add(bottom())
    }

    private fun confirmDelete(id:Long){
        AlertDialog.Builder(this).setTitle("Delete work?")
            .setMessage("This will delete the work record and its payments.")
            .setNegativeButton("Cancel",null).setPositiveButton("Delete"){_,_->db.deleteWork(id);showWorks()}.show()
    }

    private fun showParties(search:String="") {
        base("Shops & Customers")
        val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        body.add(btn("+ Add Shop / Customer"){partyDialog()})
        val s=field("Search name or mobile…",search);body.add(s,58.dp());body.add(btn("Search"){showParties(s.text.toString())})
        db.parties(search).forEach{p->
            val id=p["id"] as Long
            body.add(btn("${p["name"]}
${p["type"]}  •  ${p["mobile"]}"){partyHistory(id)})
        }
        root.add(scroll(body),LinearLayout.LayoutParams(-1,0,1f));root.add(bottom())
    }

    private fun partyHistory(id:Long){
        val name=db.partyName(id)
        val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        body.add(tv(name,22f,true))
        db.works().forEach{w->
            val ww=db.work(w["id"] as Long)
            if((ww?.get("partyId") as? Number)?.toLong()==id)
                body.add(btn("${w["workNo"]} • ${w["itemName"]}
${w["receivedDate"]} • ${w["status"]}
₹${money(w["totalCharges"] as Double)} | Due ₹${money(w["balance"] as Double)}"){showWorkDetails(w["id"] as Long)})
        }
        body.add(btn("Back"){showParties()})
        base("Party History");root.add(scroll(body),LinearLayout.LayoutParams(-1,0,1f));root.add(bottom())
    }

    private fun partyDialog(){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val name=field("Shop / Customer name");val mobile=field("Mobile number")
        val address=field("Address (optional)");val notes=field("Notes (optional)")
        box.add(name,58.dp());box.add(mobile,58.dp());box.add(address,58.dp());box.add(notes,58.dp())
        val type=Spinner(this);type.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("Jewellery Shop","Direct Customer"));box.add(type)
        AlertDialog.Builder(this).setTitle("Add Shop / Customer").setView(box)
            .setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->
                if(name.text.toString().trim().isNotEmpty()){db.addParty(name.text.toString().trim(),type.selectedItem.toString(),mobile.text.toString(),address.text.toString(),notes.text.toString());showParties()}
            }.show()
    }

    private fun showWorkDialog(editId:Long?) {
        val old=editId?.let{db.work(it)}
        val parties=db.parties()
        if(parties.isEmpty()){AlertDialog.Builder(this).setTitle("Add a shop/customer first").setMessage("Create a party before entering work.").setPositiveButton("Add now"){_,_->partyDialog()}.setNegativeButton("Cancel",null).show();return}
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val party=Spinner(this);party.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,parties.map{it["name"] as String})
        val partyIdOld=(old?.get("partyId") as? Number)?.toLong()
        val idx=parties.indexOfFirst{it["id"]==partyIdOld};if(idx>=0)party.setSelection(idx)
        box.add(tv("Party"));box.add(party)
        val item=field("Jewellery / item name",old?.get("itemName") as? String ?: "")
        val desc=field("Work description",old?.get("description") as? String ?: "")
        val gold=field("Gold received (grams)",num(old?.get("goldReceived")))
        val returned=field("Gold returned (grams)",num(old?.get("goldReturned")))
        val wastage=field("Wastage / difference (grams)",num(old?.get("wastage")))
        val stone=field("Stone weight (grams)",num(old?.get("stoneWeight")))
        val labour=field("Labour amount ₹",num(old?.get("labourAmount")))
        val other=field("Other charges ₹",num(old?.get("otherCharges")))
        val discount=field("Discount ₹",num(old?.get("discount")))
        val notes=field("Notes",old?.get("notes") as? String ?: "")
        val date=field("Received date",old?.get("receivedDate") as? String ?: today)
        val expected=field("Expected delivery date",old?.get("expectedDate") as? String ?: "")
        val delivered=field("Delivery date",old?.get("deliveryDate") as? String ?: "")
        val qty=field("Quantity",num(old?.get("quantity")?:1))
        val purity=field("Gold purity (optional)",old?.get("purity") as? String ?: "")
        val types=Spinner(this);types.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("New Jewellery Making","Repair","Polish","Resize","Stone Setting","Engraving","Cleaning","Melting","Other"))
        val oldType=old?.get("workType") as? String ?: "";val ti=(0 until types.adapter.count).firstOrNull{types.adapter.getItem(it)==oldType}?:0;types.setSelection(ti)
        val status=Spinner(this);status.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("Received","In Progress","Ready","Delivered","Cancelled"))
        val oldStatus=old?.get("status") as? String ?: "Received";val si=(0 until status.adapter.count).firstOrNull{status.adapter.getItem(it)==oldStatus}?:0;status.setSelection(si)
        listOf(item,desc,date,expected,delivered,qty,purity,gold,returned,wastage,stone,labour,other,discount,notes).forEach{box.add(it,58.dp())}
        box.add(tv("Work type"));box.add(types);box.add(tv("Status"));box.add(status)
        val sc=ScrollView(this).apply{addView(box)}
        val dlg=AlertDialog.Builder(this).setTitle(if(editId==null)"New Work" else "Edit Work").setView(sc).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create()
        dlg.setOnShowListener{
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
                val g=gold.text.toString().toDoubleOrNull()?:0.0
                val r=returned.text.toString().toDoubleOrNull()?:0.0
                val l=labour.text.toString().toDoubleOrNull()?:0.0
                val o=other.text.toString().toDoubleOrNull()?:0.0
                val dis=discount.text.toString().toDoubleOrNull()?:0.0
                val total=(l+o-dis).coerceAtLeast(0.0)
                val v=android.content.ContentValues().apply{
                    put("partyId",parties[party.selectedItemPosition]["id"] as Long)
                    put("receivedDate",date.text.toString());put("expectedDate",expected.text.toString());put("deliveryDate",delivered.text.toString())
                    put("workType",types.selectedItem.toString());put("itemName",item.text.toString());put("description",desc.text.toString())
                    put("quantity",qty.text.toString().toIntOrNull()?:1);put("purity",purity.text.toString());put("goldReceived",g);put("goldReturned",r)
                    put("wastage",wastage.text.toString().toDoubleOrNull() ?: (g-r));put("stoneWeight",stone.text.toString().toDoubleOrNull()?:0.0)
                    put("labourType","Manual");put("labourRate",0.0);put("labourAmount",l);put("otherCharges",o);put("discount",dis);put("totalCharges",total)
                    put("status",status.selectedItem.toString());put("notes",notes.text.toString());put("updatedAt",System.currentTimeMillis().toString())
                }
                if(editId==null){v.put("workNo",db.nextWorkNo());v.put("createdAt",System.currentTimeMillis().toString());db.addWork(v)}else db.updateWork(editId,v)
                dlg.dismiss();showWorks()
            }
        };dlg.show()
    }

    private fun paymentDialog(workId:Long){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val amount=field("Amount ₹");val date=field("Date",today);val note=field("Note")
        val method=Spinner(this);method.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("Cash","UPI","Bank","Other"))
        box.add(date,58.dp());box.add(amount,58.dp());box.add(method);box.add(note,58.dp())
        AlertDialog.Builder(this).setTitle("Add Payment").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->
            val a=amount.text.toString().toDoubleOrNull()?:0.0;if(a>0)db.addPayment(workId,date.text.toString(),a,method.selectedItem.toString(),note.text.toString())
            showWorkDetails(workId)
        }.show()
    }

    private fun showReports(){
        base("Reports")
        val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val d=db.dashboard()
        body.add(tv("Business totals",22f,true))
        body.add(tv("Active jobs: ${d["pending"]?.toInt()}
Gold currently recorded: ${moneyWeight(d["gold"]?:0.0)} g
Total labour: ₹${money(d["labour"]?:0.0)}
Total charges recorded: ₹${money(d["charges"]?:0.0)}",18f))
        body.add(tv("Quick status filters",19f,true))
        listOf("Received","In Progress","Ready","Delivered","Cancelled").forEach{s->body.add(btn(s){showWorks("",s)})}
        body.add(btn("Export / Backup"){backupDialog()})
        root.add(scroll(body),LinearLayout.LayoutParams(-1,0,1f));root.add(bottom())
    }

    private fun showSearch(){
        val e=field("Search anything")
        AlertDialog.Builder(this).setTitle("Search Work").setView(e).setNegativeButton("Cancel",null).setPositiveButton("Search"){_,_->showWorks(e.text.toString())}.show()
    }

    private fun backupDialog(){
        AlertDialog.Builder(this).setTitle("Backup / Restore").setItems(arrayOf("Export JSON backup","Import JSON backup")){_,which->if(which==0)exportBackup()else importBackup()}.show()
    }

    private var pendingExport:String?=null
    private fun exportBackup(){
        val obj=JSONObject();val pa=JSONArray();val wo=JSONArray();val pay=JSONArray()
        db.readableDatabase.rawQuery("SELECT * FROM parties",null).use{c->while(c.moveToNext()){val o=JSONObject();for(i in 0 until c.columnCount)o.put(c.getColumnName(i),c.getString(i));pa.put(o)}}
        db.readableDatabase.rawQuery("SELECT * FROM works",null).use{c->while(c.moveToNext()){val o=JSONObject();for(i in 0 until c.columnCount)o.put(c.getColumnName(i),c.getString(i));wo.put(o)}}
        db.readableDatabase.rawQuery("SELECT * FROM payments",null).use{c->while(c.moveToNext()){val o=JSONObject();for(i in 0 until c.columnCount)o.put(c.getColumnName(i),c.getString(i));pay.put(o)}}
        obj.put("parties",pa);obj.put("works",wo);obj.put("payments",pay)
        pendingExport=obj.toString(2)
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{type="application/json";putExtra(Intent.EXTRA_TITLE,"smith-backup-${today.replace('/','-')}.json")},900)
    }

    private fun importBackup(){
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="application/json";addCategory(Intent.CATEGORY_OPENABLE)},901)
    }

    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK||data?.data==null)return
        val uri=data.data!!
        try{
            if(requestCode==900){contentResolver.openOutputStream(uri)!!.use{it.write(pendingExport!!.toByteArray())};Toast.makeText(this,"Backup exported",Toast.LENGTH_LONG).show()}
            if(requestCode==901){
                val text=contentResolver.openInputStream(uri)!!.use{BufferedReader(InputStreamReader(it)).readText()}
                val o=JSONObject(text);val d=db.writableDatabase
                d.beginTransaction()
                try{
                    val ps=o.optJSONArray("parties")?:JSONArray()
                    for(i in 0 until ps.length()){val x=ps.getJSONObject(i);val v=android.content.ContentValues();for(k in listOf("name","type","mobile","address","notes","createdAt"))if(x.has(k))v.put(k,x.optString(k));d.insert("parties",null,v)}
                    d.setTransactionSuccessful()
                }finally{d.endTransaction()}
                Toast.makeText(this,"Backup imported. Existing data was kept.",Toast.LENGTH_LONG).show();showHome()
            }
        }catch(e:Exception){Toast.makeText(this,"Backup could not be imported: ${e.message}",Toast.LENGTH_LONG).show()}
    }

    private fun money(v:Double)=String.format(Locale.US,"%,.2f",v)
    private fun moneyWeight(v:Double)=String.format(Locale.US,"%,.3f",v)
    private fun num(v:Any?)=(v as? Number)?.toString()?:""
}
