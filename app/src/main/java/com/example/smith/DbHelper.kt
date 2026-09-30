package com.example.smith

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper(context: Context) : SQLiteOpenHelper(context, "smith.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE parties(
            id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, type TEXT NOT NULL,
            mobile TEXT, address TEXT, notes TEXT, createdAt TEXT NOT NULL
        )""")
        db.execSQL("""CREATE TABLE works(
            id INTEGER PRIMARY KEY AUTOINCREMENT, workNo TEXT UNIQUE NOT NULL, partyId INTEGER NOT NULL,
            receivedDate TEXT NOT NULL, expectedDate TEXT, deliveryDate TEXT, workType TEXT,
            itemName TEXT, description TEXT, quantity INTEGER DEFAULT 1, purity TEXT,
            goldReceived REAL DEFAULT 0, goldReturned REAL DEFAULT 0, wastage REAL DEFAULT 0,
            stoneWeight REAL DEFAULT 0, labourType TEXT, labourRate REAL DEFAULT 0,
            labourAmount REAL DEFAULT 0, otherCharges REAL DEFAULT 0, discount REAL DEFAULT 0,
            totalCharges REAL DEFAULT 0, status TEXT NOT NULL, notes TEXT,
            createdAt TEXT NOT NULL, updatedAt TEXT NOT NULL,
            FOREIGN KEY(partyId) REFERENCES parties(id)
        )""")
        db.execSQL("""CREATE TABLE payments(
            id INTEGER PRIMARY KEY AUTOINCREMENT, workId INTEGER NOT NULL, date TEXT NOT NULL,
            amount REAL NOT NULL, method TEXT, note TEXT,
            FOREIGN KEY(workId) REFERENCES works(id) ON DELETE CASCADE
        )""")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun nextWorkNo(): String {
        val c=readableDatabase.rawQuery("SELECT COUNT(*) FROM works",null);c.moveToFirst()
        val n=c.getInt(0)+1;c.close();return "GS-"+n.toString().padStart(4,'0')
    }
    fun addParty(name:String,type:String,mobile:String,address:String,notes:String):Long {
        val v=ContentValues().apply{put("name",name);put("type",type);put("mobile",mobile);put("address",address);put("notes",notes);put("createdAt",System.currentTimeMillis().toString())}
        return writableDatabase.insert("parties",null,v)
    }
    fun parties(search:String=""):List<Map<String,Any>> {
        val out=mutableListOf<Map<String,Any>>()
        val q=if(search.isBlank())"SELECT * FROM parties ORDER BY name" else "SELECT * FROM parties WHERE name LIKE ? OR mobile LIKE ? ORDER BY name"
        val args=if(search.isBlank())null else arrayOf("%$search%","%$search%")
        val c=readableDatabase.rawQuery(q,args)
        while(c.moveToNext())out.add(mapOf("id" to c.getLong(c.getColumnIndexOrThrow("id")),"name" to c.getString(c.getColumnIndexOrThrow("name")),"type" to c.getString(c.getColumnIndexOrThrow("type")),"mobile" to (c.getString(c.getColumnIndexOrThrow("mobile"))?:"")))
        c.close();return out
    }
    fun partyName(id:Long):String {
        val c=readableDatabase.rawQuery("SELECT name FROM parties WHERE id=?",arrayOf(id.toString()))
        val s=if(c.moveToFirst())c.getString(0) else "Unknown";c.close();return s
    }
    fun addWork(v:ContentValues):Long=writableDatabase.insert("works",null,v)
    fun updateWork(id:Long,v:ContentValues):Int=writableDatabase.update("works",v,"id=?",arrayOf(id.toString()))
    fun deleteWork(id:Long){writableDatabase.delete("payments","workId=?",arrayOf(id.toString()));writableDatabase.delete("works","id=?",arrayOf(id.toString()))}

    fun works(search:String="",status:String=""):List<Map<String,Any>> {
        val out=mutableListOf<Map<String,Any>>();val clauses=mutableListOf<String>();val args=mutableListOf<String>()
        if(search.isNotBlank()){clauses.add("(w.workNo LIKE ? OR p.name LIKE ? OR w.itemName LIKE ? OR w.description LIKE ?)");repeat(4){args.add("%$search%")}}
        if(status.isNotBlank()){clauses.add("w.status=?");args.add(status)}
        val where=if(clauses.isEmpty())"" else " WHERE "+clauses.joinToString(" AND ")
        val c=readableDatabase.rawQuery("SELECT w.*,p.name partyName FROM works w JOIN parties p ON p.id=w.partyId$where ORDER BY w.id DESC",args.toTypedArray())
        while(c.moveToNext()){
            val id=c.getLong(c.getColumnIndexOrThrow("id"))
            out.add(mapOf("id" to id,"workNo" to c.getString(c.getColumnIndexOrThrow("workNo")),"partyName" to c.getString(c.getColumnIndexOrThrow("partyName")),
                "receivedDate" to c.getString(c.getColumnIndexOrThrow("receivedDate")),"itemName" to (c.getString(c.getColumnIndexOrThrow("itemName"))?:""),
                "workType" to (c.getString(c.getColumnIndexOrThrow("workType"))?:""),"goldReceived" to c.getDouble(c.getColumnIndexOrThrow("goldReceived")),
                "goldReturned" to c.getDouble(c.getColumnIndexOrThrow("goldReturned")),"totalCharges" to c.getDouble(c.getColumnIndexOrThrow("totalCharges")),
                "status" to c.getString(c.getColumnIndexOrThrow("status")),"balance" to balance(id)))
        }
        c.close();return out
    }

    fun work(id:Long):Map<String,Any?>? {
        val c=readableDatabase.rawQuery("SELECT * FROM works WHERE id=?",arrayOf(id.toString()))
        if(!c.moveToFirst()){c.close();return null}
        val m=mutableMapOf<String,Any?>()
        for(i in 0 until c.columnCount)m[c.getColumnName(i)]=when(c.getType(i)){
            Cursor.FIELD_TYPE_NULL->null;Cursor.FIELD_TYPE_INTEGER->c.getLong(i);Cursor.FIELD_TYPE_FLOAT->c.getDouble(i);else->c.getString(i)
        }
        c.close();return m
    }
    fun addPayment(workId:Long,date:String,amount:Double,method:String,note:String):Long{
        val v=ContentValues().apply{put("workId",workId);put("date",date);put("amount",amount);put("method",method);put("note",note)}
        return writableDatabase.insert("payments",null,v)
    }
    fun payments(workId:Long):List<Map<String,Any>>{
        val out=mutableListOf<Map<String,Any>>();val c=readableDatabase.rawQuery("SELECT * FROM payments WHERE workId=? ORDER BY id DESC",arrayOf(workId.toString()))
        while(c.moveToNext())out.add(mapOf("date" to c.getString(c.getColumnIndexOrThrow("date")),"amount" to c.getDouble(c.getColumnIndexOrThrow("amount")),"method" to (c.getString(c.getColumnIndexOrThrow("method"))?:""),"note" to (c.getString(c.getColumnIndexOrThrow("note"))?:"")))
        c.close();return out
    }
    fun paid(workId:Long):Double{val c=readableDatabase.rawQuery("SELECT COALESCE(SUM(amount),0) FROM payments WHERE workId=?",arrayOf(workId.toString()));c.moveToFirst();val x=c.getDouble(0);c.close();return x}
    fun balance(workId:Long):Double{val w=work(workId)?:return 0.0;return ((w["totalCharges"] as? Number)?.toDouble()?:0.0)-paid(workId)}
    fun dashboard():Map<String,Double>{
        val c=readableDatabase.rawQuery("""SELECT COUNT(*) jobs,
          COALESCE(SUM(CASE WHEN status!='Delivered' AND status!='Cancelled' THEN 1 ELSE 0 END),0) pending,
          COALESCE(SUM(CASE WHEN status!='Delivered' AND status!='Cancelled' THEN goldReceived-goldReturned ELSE 0 END),0) gold,
          COALESCE(SUM(labourAmount),0) labour, COALESCE(SUM(totalCharges),0) charges FROM works""",null)
        c.moveToFirst();val m=mapOf("jobs" to c.getDouble(0),"pending" to c.getDouble(1),"gold" to c.getDouble(2),"labour" to c.getDouble(3),"charges" to c.getDouble(4));c.close();return m
    }
}
