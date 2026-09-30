package com.example.smith

import android.app.*
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
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
    private val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val today get() = fmt.format(Date())
    private val dp get() = resources.displayMetrics.density

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        db = DbHelper(this)
        showHome()
    }

    private fun px(n:Int)= (n*dp).toInt()
    private fun TextView.pad() { setPadding(px(16),px(10),px(16),px(10)) }
    private fun text(s:String,size:Float=16f,bold:Boolean=false)=TextView(this).apply{
        this.text=s; textSize=size; setTextColor(Color.rgb(35,35,35)); pad()
        if(bold) setTypeface(null,android.graphics.Typeface.BOLD)
    }
    private fun button(s:String,action:()->Unit)=Button(this).apply{
        text=s; isAllCaps=false; minHeight=px(48); setOnClickListener{action()}
    }
    private fun edit(h:String,value:String="")=EditText(this).apply{
        hint=h; setText(value); textSize=16f; setPadding(px(12),0,px(12),0)
    }
    private fun LinearLayout.add(v:View,h:Int=-2){ addView(v,LinearLayout.LayoutParams(-1,h)) }
    private fun LinearLayout.add(v:View,p:LinearLayout.LayoutParams){ addView(v,p) }
    private fun scroll(v:View)=ScrollView(this).apply{addView(v)}

    private fun shell(title:String){
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(248,245,241))}
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setBackgroundColor(Color.WHITE)}
        top.add(text(title,20f,true),LinearLayout.LayoutParams(0,px(60),1f))
        top.add(button("Search"){searchDialog()},LinearLayout.LayoutParams(px(80),px(60)))
        root.add(top)
        setContentView(root)
    }

    private fun nav():LinearLayout{
        val n=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setBackgroundColor(Color.WHITE)}
        n.add(button("Home"){showHome()},LinearLayout.LayoutParams(0,px(58),1f))
        n.add(button("Work"){showWorks()},LinearLayout.LayoutParams(0,px(58),1f))
        n.add(button("Parties"){showParties()},LinearLayout.LayoutParams(0,px(58),1f))
        n.add(button("Reports"){showReports()},LinearLayout.LayoutParams(0,px(58),1f))
        return n
    }

    private fun finishPage(body:View){
        root.add(scroll(body),LinearLayout.LayoutParams(-1,0,1f));root.add(nav())
    }

    private fun showHome(){
        shell("Smith • Goldsmith Work Book")
        val d=db.dashboard()
        val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        b.add(text("Business overview",20f,true))
        b.add(text("Active jobs: "+(d["pending"]?:0.0).toInt()))
        b.add(text("Gold currently with me: "+weight(d["gold"]?:0.0)+" g",18f,true))
        b.add(text("Total labour recorded: ₹"+money(d["labour"]?:0.0)))
        b.add(button("+ New Work"){workDialog(null)})
        b.add(button("Work Register"){showWorks()})
        b.add(button("Shops / Customers"){showParties()})
        b.add(button("Reports"){showReports()})
        b.add(button("Backup / Restore"){backupDialog()})
        finishPage(b)
    }

    private fun showWorks(search:String="",status:String=""){
        shell("Work Register")
        val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        b.add(button("+ New Work"){workDialog(null)})
        val q=edit("Search shop, customer, work no, item",search);b.add(q,px(56))
        b.add(button("Search"){showWorks(q.text.toString(),status)})
        if(status.isNotEmpty()) b.add(text("Filter: "+status,16f,true))
        db.works(search,status).forEach{w->
            val id=w["id"] as Long
            val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.WHITE)}
            c.add(text(w["workNo"].toString()+" • "+w["partyName"],18f,true))
            c.add(text(w["itemName"].toString()+" • "+w["workType"]+" • "+w["status"]))
            c.add(text("Date: "+w["receivedDate"]+" | Gold: "+weight(w["goldReceived"] as Double)+" g | Charges: ₹"+money(w["totalCharges"] as Double)+" | Due: ₹"+money(w["balance"] as Double)))
            c.setOnClickListener{workDetails(id)}
            b.add(c,LinearLayout.LayoutParams(-1,-2).apply{setMargins(px(8),px(5),px(8),px(5))})
        }
        finishPage(b)
    }

    private fun workDetails(id:Long){
        val w=db.work(id) ?: return
        shell("Work Details")
        val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val party=db.partyName((w["partyId"] as Number).toLong())
        b.add(text(w["workNo"].toString()+" • "+party,22f,true))
        b.add(text("Status: "+w["status"]+" | Received: "+w["receivedDate"]+" | Delivery: "+(w["deliveryDate"]?:"-")))
        b.add(text("Item: "+(w["itemName"]?:"-")+" | Type: "+(w["workType"]?:"-")))
        b.add(text("Description: "+(w["description"]?:"-")))
        b.add(text("Gold received: "+weight((w["goldReceived"] as Number).toDouble())+" g | Returned: "+weight((w["goldReturned"] as Number).toDouble())+" g | Difference: "+weight((w["wastage"] as Number).toDouble())+" g"))
        b.add(text("Labour: ₹"+money((w["labourAmount"] as Number).toDouble())+" | Other: ₹"+money((w["otherCharges"] as Number).toDouble())+" | Discount: ₹"+money((w["discount"] as Number).toDouble())))
        b.add(text("Total: ₹"+money((w["totalCharges"] as Number).toDouble())+" | Paid: ₹"+money(db.paid(id))+" | Due: ₹"+money(db.balance(id)),18f,true))
        b.add(button("Add Payment"){paymentDialog(id)})
        b.add(button("Edit Work"){workDialog(id)})
        b.add(button("Delete Work"){confirmDelete(id)})
        b.add(text("Payment history",18f,true))
        db.payments(id).forEach{p->b.add(text(p["date"].toString()+" • ₹"+money(p["amount"] as Double)+" • "+p["method"]+" • "+p["note"]))}
        finishPage(b)
    }

    private fun confirmDelete(id:Long){
        AlertDialog.Builder(this).setTitle("Delete work?")
            .setMessage("The work record and its payments will be deleted.")
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Delete"){_,_->db.deleteWork(id);showWorks()}.show()
    }

    private fun showParties(search:String=""){
        shell("Shops & Customers")
        val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        b.add(button("+ Add Shop / Customer"){partyDialog()})
        val q=edit("Search name or mobile",search);b.add(q,px(56))
        b.add(button("Search"){showParties(q.text.toString())})
        db.parties(search).forEach{p->
            val id=p["id"] as Long
            b.add(button(p["name"].toString()+" • "+p["type"]+" • "+p["mobile"]){partyHistory(id)})
        }
        finishPage(b)
    }

    private fun partyHistory(id:Long){
        shell("Party History")
        val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        b.add(text(db.partyName(id),22f,true))
        db.works().forEach{w->
            val ww=db.work(w["id"] as Long)
            if((ww?.get("partyId") as? Number)?.toLong()==id)
                b.add(button(w["workNo"].toString()+" • "+w["itemName"]+" • "+w["status"]+" • Due ₹"+money(w["balance"] as Double)){workDetails(w["id"] as Long)})
        }
        finishPage(b)
    }

    private fun partyDialog(){
        val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val name=edit("Shop / Customer name");val mobile=edit("Mobile number");val address=edit("Address");val notes=edit("Notes")
        b.add(name,px(56));b.add(mobile,px(56));b.add(address,px(56));b.add(notes,px(56))
        val type=Spinner(this);type.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("Jewellery Shop","Direct Customer"));b.add(type)
        AlertDialog.Builder(this).setTitle("Add Shop / Customer").setView(b)
            .setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->
                if(name.text.toString().trim().isNotEmpty())db.addParty(name.text.toString().trim(),type.selectedItem.toString(),mobile.text.toString(),address.text.toString(),notes.text.toString())
                showParties()
            }.show()
    }

    private fun workDialog(editId:Long?){
        val old=editId?.let{db.work(it)};val parties=db.parties()
        if(parties.isEmpty()){AlertDialog.Builder(this).setTitle("Add a party first").setMessage("Create a shop/customer before adding work.").setPositiveButton("Add"){_,_->partyDialog()}.setNegativeButton("Cancel",null).show();return}
        val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val party=Spinner(this);party.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,parties.map{it["name"].toString()})
        val oldParty=(old?.get("partyId") as? Number)?.toLong();val pi=parties.indexOfFirst{it["id"]==oldParty};if(pi>=0)party.setSelection(pi);b.add(party)
        val item=edit("Jewellery / item",old?.get("itemName")?.toString()?:"");val desc=edit("Work description",old?.get("description")?.toString()?:"")
        val date=edit("Received date",old?.get("receivedDate")?.toString()?:today);val expected=edit("Expected delivery",old?.get("expectedDate")?.toString()?:"");val delivery=edit("Delivery date",old?.get("deliveryDate")?.toString()?:"")
        val qty=edit("Quantity",old?.get("quantity")?.toString()?:"1");val purity=edit("Gold purity",old?.get("purity")?.toString()?:"")
        val received=edit("Gold received grams",num(old?.get("goldReceived")));val returned=edit("Gold returned grams",num(old?.get("goldReturned")));val wastage=edit("Difference / wastage grams",num(old?.get("wastage")))
        val stone=edit("Stone weight grams",num(old?.get("stoneWeight")));val labour=edit("Labour ₹",num(old?.get("labourAmount")));val other=edit("Other charges ₹",num(old?.get("otherCharges")));val discount=edit("Discount ₹",num(old?.get("discount")));val notes=edit("Notes",old?.get("notes")?.toString()?:"")
        val type=Spinner(this);type.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("New Jewellery Making","Repair","Polish","Resize","Stone Setting","Engraving","Cleaning","Melting","Other"))
        val oldType=old?.get("workType")?.toString()?:"";type.setSelection((0 until type.adapter.count).firstOrNull{type.adapter.getItem(it)==oldType}?:0)
        val status=Spinner(this);status.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("Received","In Progress","Ready","Delivered","Cancelled"))
        val oldStatus=old?.get("status")?.toString()?:"Received";status.setSelection((0 until status.adapter.count).firstOrNull{status.adapter.getItem(it)==oldStatus}?:0)
        listOf(item,desc,date,expected,delivery,qty,purity,received,returned,wastage,stone,labour,other,discount,notes).forEach{b.add(it,px(56))}
        b.add(type);b.add(status)
        val dlg=AlertDialog.Builder(this).setTitle(if(editId==null)"New Work" else "Edit Work").setView(ScrollView(this).apply{addView(b)}).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create()
        dlg.setOnShowListener{
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
                val g=received.text.toString().toDoubleOrNull()?:0.0;val r=returned.text.toString().toDoubleOrNull()?:0.0
                val l=labour.text.toString().toDoubleOrNull()?:0.0;val o=other.text.toString().toDoubleOrNull()?:0.0;val dis=discount.text.toString().toDoubleOrNull()?:0.0
                val v=ContentValues()
                v.put("partyId",parties[party.selectedItemPosition]["id"] as Long);v.put("receivedDate",date.text.toString());v.put("expectedDate",expected.text.toString());v.put("deliveryDate",delivery.text.toString())
                v.put("workType",type.selectedItem.toString());v.put("itemName",item.text.toString());v.put("description",desc.text.toString());v.put("quantity",qty.text.toString().toIntOrNull()?:1);v.put("purity",purity.text.toString())
                v.put("goldReceived",g);v.put("goldReturned",r);v.put("wastage",wastage.text.toString().toDoubleOrNull()?:g-r);v.put("stoneWeight",stone.text.toString().toDoubleOrNull()?:0.0)
                v.put("labourType","Manual");v.put("labourRate",0.0);v.put("labourAmount",l);v.put("otherCharges",o);v.put("discount",dis);v.put("totalCharges",(l+o-dis).coerceAtLeast(0.0));v.put("status",status.selectedItem.toString());v.put("notes",notes.text.toString());v.put("updatedAt",System.currentTimeMillis().toString())
                if(editId==null){v.put("workNo",db.nextWorkNo());v.put("createdAt",System.currentTimeMillis().toString());db.addWork(v)}else db.updateWork(editId,v)
                dlg.dismiss();showWorks()
            }
        };dlg.show()
    }

    private fun paymentDialog(workId:Long){
        val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};val date=edit("Date",today);val amount=edit("Amount ₹");val note=edit("Note")
        val method=Spinner(this);method.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("Cash","UPI","Bank","Other"))
        b.add(date,px(56));b.add(amount,px(56));b.add(method);b.add(note,px(56))
        AlertDialog.Builder(this).setTitle("Add Payment").setView(b).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->
            val a=amount.text.toString().toDoubleOrNull()?:0.0;if(a>0)db.addPayment(workId,date.text.toString(),a,method.selectedItem.toString(),note.text.toString());workDetails(workId)
        }.show()
    }

    private fun showReports(){
        shell("Reports");val d=db.dashboard();val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        b.add(text("Active jobs: "+(d["pending"]?:0.0).toInt(),18f,true));b.add(text("Gold currently recorded: "+weight(d["gold"]?:0.0)+" g"));b.add(text("Total labour: ₹"+money(d["labour"]?:0.0)));b.add(text("Total charges: ₹"+money(d["charges"]?:0.0)))
        b.add(text("Status filters",19f,true));listOf("Received","In Progress","Ready","Delivered","Cancelled").forEach{s->b.add(button(s){showWorks("",s)})};b.add(button("Backup / Restore"){backupDialog()});finishPage(b)
    }

    private fun searchDialog(){
        val e=edit("Search shop, customer, work no or item")
        AlertDialog.Builder(this).setTitle("Search").setView(e).setNegativeButton("Cancel",null).setPositiveButton("Search"){_,_->showWorks(e.text.toString())}.show()
    }

    private var pendingExport=""
    private fun backupDialog(){AlertDialog.Builder(this).setTitle("Backup / Restore").setItems(arrayOf("Export JSON backup","Import JSON backup")){_,i->if(i==0)exportBackup()else importBackup()}.show()}
    private fun exportBackup(){
        val o=JSONObject();val ps=JSONArray();val ws=JSONArray();val pays=JSONArray()
        db.readableDatabase.rawQuery("SELECT * FROM parties",null).use{c->while(c.moveToNext()){val x=JSONObject();for(i in 0 until c.columnCount)x.put(c.getColumnName(i),c.getString(i));ps.put(x)}}
        db.readableDatabase.rawQuery("SELECT * FROM works",null).use{c->while(c.moveToNext()){val x=JSONObject();for(i in 0 until c.columnCount)x.put(c.getColumnName(i),c.getString(i));ws.put(x)}}
        db.readableDatabase.rawQuery("SELECT * FROM payments",null).use{c->while(c.moveToNext()){val x=JSONObject();for(i in 0 until c.columnCount)x.put(c.getColumnName(i),c.getString(i));pays.put(x)}}
        o.put("parties",ps);o.put("works",ws);o.put("payments",pays);pendingExport=o.toString(2)
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{type="application/json";putExtra(Intent.EXTRA_TITLE,"smith-backup-"+today.replace('/','-')+".json")},900)
    }
    private fun importBackup(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="application/json";addCategory(Intent.CATEGORY_OPENABLE)},901)}

    override fun onActivityResult(req:Int,result:Int,data:Intent?){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data?.data==null)return
        try{
            if(req==900){contentResolver.openOutputStream(data.data!!)!!.use{it.write(pendingExport.toByteArray())};Toast.makeText(this,"Backup exported",Toast.LENGTH_LONG).show()}
            else if(req==901){
                val s=contentResolver.openInputStream(data.data!!)!!.use{BufferedReader(InputStreamReader(it)).readText()}
                val o=JSONObject(s);val ps=o.optJSONArray("parties")?:JSONArray();val ws=o.optJSONArray("works")?:JSONArray();val pays=o.optJSONArray("payments")?:JSONArray()
                val d=db.writableDatabase;val partyMap=HashMap<Long,Long>();val workMap=HashMap<Long,Long>()
                d.beginTransaction()
                try{
                    for(i in 0 until ps.length()){
                        val x=ps.getJSONObject(i);val v=ContentValues()
                        listOf("name","type","mobile","address","notes","createdAt").forEach{k->if(x.has(k))v.put(k,x.optString(k))}
                        val newId=d.insert("parties",null,v);if(newId>0)partyMap[x.optLong("id")]=newId
                    }
                    for(i in 0 until ws.length()){
                        val x=ws.getJSONObject(i);val v=ContentValues()
                        listOf("workNo","receivedDate","expectedDate","deliveryDate","workType","itemName","description","quantity","purity","goldReceived","goldReturned","wastage","stoneWeight","labourType","labourRate","labourAmount","otherCharges","discount","totalCharges","status","notes","createdAt","updatedAt").forEach{k->if(x.has(k))v.put(k,x.optString(k))}
                        val partyId=partyMap[x.optLong("partyId")]?:0L
                        if(partyId>0){v.put("partyId",partyId);var newId=d.insert("works",null,v)
                            if(newId<0){v.put("workNo",db.nextWorkNo());newId=d.insert("works",null,v)}
                            if(newId>0)workMap[x.optLong("id")]=newId}
                    }
                    for(i in 0 until pays.length()){
                        val x=pays.getJSONObject(i);val workId=workMap[x.optLong("workId")]?:0L
                        if(workId>0){val v=ContentValues();v.put("workId",workId);v.put("date",x.optString("date"));v.put("amount",x.optDouble("amount"));v.put("method",x.optString("method"));v.put("note",x.optString("note"));d.insert("payments",null,v)}
                    }
                    d.setTransactionSuccessful()
                }finally{d.endTransaction()}
                Toast.makeText(this,"Complete backup imported. Existing data kept.",Toast.LENGTH_LONG).show();showHome()
            }
        }catch(e:Exception){Toast.makeText(this,"Import failed: "+e.message,Toast.LENGTH_LONG).show()}
    }

    private fun money(v:Double)=String.format(Locale.US,"%,.2f",v)
    private fun weight(v:Double)=String.format(Locale.US,"%,.3f",v)
    private fun num(v:Any?)=(v as? Number)?.toString()?:""
}
