package com.example.kostowski.legolist

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.databinding.ObservableArrayList
import android.graphics.Bitmap
import android.util.Log
import java.io.FileOutputStream
import java.io.IOException
import java.sql.Blob
import android.database.sqlite.SQLiteStatement
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory




class DatabaseHelper: SQLiteOpenHelper {

    private val DB_PATH = "/data/data/com.example.kostowski.legolist/databases/"

    private val DB_NAME = "BrickListDB.db"

    private var myDataBase: SQLiteDatabase? = null

    private val myContext: Context


    constructor(context: Context):super(context, "BrickListDB.db", null, 4){
        this.myContext = context
    }

    @Throws(IOException::class)
    fun createDataBase() {

        val dbExist = checkDataBase()

        if (!dbExist) {

            this.readableDatabase

            try {

                copyDataBase()

            } catch (e: IOException) {

                throw Error("Error copying database")

            }

        }

    }

    private fun checkDataBase(): Boolean {

        var checkDB: SQLiteDatabase? = null

        try {
            val myPath = DB_PATH + DB_NAME
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY)

        } catch (e: SQLiteException) {

        }

        if (checkDB != null) {

            checkDB.close()

        }

        return if (checkDB != null) true else false
    }

    @Throws(IOException::class)
    private fun copyDataBase() {

        val myInput = myContext.assets.open(DB_NAME)
        val outFileName = DB_PATH + DB_NAME
        val myOutput = FileOutputStream(outFileName)

        val buffer = ByteArray(1024)
        var length = myInput.read(buffer)
        while (length > 0) {
            myOutput.write(buffer, 0, length)
            length = myInput.read(buffer)
        }

        myOutput.flush()
        myOutput.close()
        myInput.close()

    }

    @Throws(SQLException::class)
    fun openDataBase() {

        val myPath = DB_PATH + DB_NAME
        myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE)

    }

    @Synchronized override fun close() {

        if (myDataBase != null)
            myDataBase!!.close()

        super.close()

    }

    override fun onCreate(db: SQLiteDatabase) {

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if(newVersion>oldVersion){
            copyDataBase()
        }
    }


    fun fetchName(code: String): String{
        val query = "SELECT NAME FROM Parts WHERE CODE LIKE \"$code\""
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        var name: String = ""
        if(cursor.moveToFirst()){
            name = cursor.getString(0)
        }
        cursor.close()
        return name
    }

    fun fetchColorName(colorId: Int): String{
        val query = "SELECT NAME FROM Colors WHERE CODE=$colorId"
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        var colorName: String = ""
        if(cursor.moveToFirst()){
            colorName = cursor.getString(0)
        }
        cursor.close()
        return colorName
    }

    fun fetchTypeName(code: String): String{
        val query = "SELECT NAME FROM ItemTypes WHERE CODE LIKE \"$code\""
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        var typeName: String = ""
        if(cursor.moveToFirst()){
            typeName = cursor.getString(0)
        }
        cursor.close()
        return typeName
    }

    fun fetchProjects(): ObservableArrayList<Project> {
        val query = "SELECT _id, Name, Active FROM Inventories"
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        var tempList = ObservableArrayList<Project>()
        if(cursor.moveToFirst()) {
            tempList.add(Project(cursor.getString(1), cursor.getInt(0), cursor.getInt(2)>0))
        }
        while(cursor.moveToNext()){
            tempList.add(Project(cursor.getString(1), cursor.getInt(0), cursor.getInt(2)>0))
        }
        cursor.close()
        return tempList
    }

    fun fetchCode(itemId: Int, colorId: Int): String{
        val query = "SELECT Code FROM Codes WHERE ItemID=$itemId AND ColorID=$colorId"
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        var code: String = ""
        if(cursor.moveToFirst()){
            code = cursor.getString(0)
        }
        cursor.close()
        return code
    }

    fun fetchImage(itemId: Int, colorId: Int): Bitmap?{
        val query = "SELECT Image FROM Codes WHERE ItemID=$itemId AND ColorID=$colorId"
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        var image: ByteArray? = null
        if(cursor.moveToFirst()){
            image = cursor.getBlob(0)
        }
        cursor.close()
        var bitmap:Bitmap? = null
        if(image!=null)
            bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)

        return bitmap
    }

    fun saveImage(itemId: Int, colorId: Int, bitmap: Bitmap){
        val db = this.writableDatabase
        var values = ContentValues()
        db.beginTransaction()
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

        values.put("Image", stream.toByteArray())
        db.update("Codes", values, "ItemID=$itemId AND ColorID=$colorId", null)
        db.setTransactionSuccessful()
        db.endTransaction()
    }

    fun saveProject(project: Project){
        val db = this.writableDatabase
        var values = ContentValues()
        db.beginTransaction()
        values.put("_id", project.projectId)
        values.put("Name", project.name)
        if(project.active)
            values.put("Active", 1)
        else
            values.put("Active", 0)
        values.put("LastAccessed", 0)

        db.delete("Inventories", "_id=${project.projectId}", null)

        db.insert("Inventories", null, values)

        db.delete("InventoriesParts", "InventoryID=${project.projectId}", null)

        for(brick in project.listOfNeededBricks){
            values = ContentValues()
            values.put("_id", getNextId("InventoriesParts") )
            values.put("InventoryID", project.projectId)
            values.put("TypeID", brick.brickTypeCode)
            values.put("ItemID", brick.blockIdCode)
            values.put("QuantityInSet", brick.maxNumber)
            values.put("QuantityInStore", brick.collectedNumber)
            values.put("ColorID", brick.colorCode)
            values.put("Extra", 0)
            db.insert("InventoriesParts", null, values)
        }
        db.setTransactionSuccessful()
        db.endTransaction()
    }

    fun getBrickId(code: String): Int{
        val query = "SELECT _id FROM Parts WHERE Code LIKE \"$code\""
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        return if(cursor.moveToFirst()) cursor.getInt(0) else -1
    }

    fun getNextId(table: String): Int{
        val query = "SELECT max(_id) FROM $table;"
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        var lastId = 0
        if (cursor.moveToFirst()) {
            lastId = cursor.getInt(0)
        }
        cursor.close()
        return lastId+1
    }

    fun addProjectFromUrl(name: String, url: String, c: Context): Int{
        val newProject = Project(name, getNextId("Inventories"), true)
        var newUrl: String = "http://fcds.cs.put.poznan.pl/MyWeb/BL/"+url+".xml"
        newProject.makeProjectFromLink(newUrl, c)
        saveProject(newProject)
        return newProject.projectId
    }

    fun loadProject(id: Int): Project{
        val db = this.readableDatabase
        val queryItems = "SELECT * FROM InventoriesParts WHERE InventoryID=$id"
        var cursor = db.rawQuery(queryItems, null)
        var blocks = ObservableArrayList<Brick>()

        cursor.moveToFirst()
        var blockId = getBrickId(cursor.getString(3))
        blocks.add(Brick(fetchName(cursor.getString(3)), fetchTypeName(cursor.getString(2)), cursor.getInt(6), cursor.getString(3), cursor.getString(2), cursor.getInt(5), cursor.getInt(4), fetchColorName(cursor.getInt(6)),fetchCode(blockId, cursor.getInt(6))))
        while(cursor.moveToNext()){
            blockId = getBrickId(cursor.getString(3))
            blocks.add(Brick(fetchName(cursor.getString(3)), fetchTypeName(cursor.getString(2)), cursor.getInt(6), cursor.getString(3), cursor.getString(2), cursor.getInt(5), cursor.getInt(4), fetchColorName(cursor.getInt(6)),fetchCode(blockId, cursor.getInt(6))))
        }
        cursor.close()

        val queryProject = "SELECT * FROM Inventories WHERE _id=$id"
        cursor = db.rawQuery(queryProject, null)
        cursor.moveToFirst()
        var loadedProject = Project(cursor.getString(1), cursor.getInt(0), cursor.getInt(2)>0)
        loadedProject.listOfNeededBricks = blocks

        cursor.close()
        return loadedProject
    }
}