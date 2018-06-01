package com.example.kostowski.legolist

import android.content.Context
import android.databinding.ObservableArrayList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.activity_project_view.*

class ProjectView : AppCompatActivity() {

    var currentProject: Project? = null
    val db: DatabaseHelper = DatabaseHelper(this)
    var bricksList = ObservableArrayList<Brick>()
    var bricksAdapter = BricksAdapter(this, bricksList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_view)
        val extras = intent.extras ?: return
        var projectId = extras.getInt("projectId")

        try{
            db.createDataBase()
        }catch (e: Exception){
            e.printStackTrace()
        }
        try{
            db.openDataBase()
        }catch (e: Exception){
            e.printStackTrace()
        }

        currentProject = DatabaseHelper(this).loadProject(projectId)

        //bricksList.addAll(currentProject!!.listOfNeededBricks)

        bricksAdapter = BricksAdapter(this, currentProject!!.listOfNeededBricks )

        lvBricks.adapter=bricksAdapter
    }

    fun exportProject(v:View){
        XMLParser(this).exportToXMLFile(currentProject!!, currentProject!!.name)
        Toast.makeText(this, "Zapisano na karcie pamięci jako " + currentProject!!.name + ".xml", Toast.LENGTH_SHORT).show()

    }

    override fun finish() {
        currentProject!!.refreshProjectStatus()
        db.saveProject(currentProject!!)
        super.finish()
    }

    override fun onPause() {
        currentProject!!.refreshProjectStatus()
        db.saveProject(currentProject!!)
        super.onPause()
    }

    override fun onDestroy() {
        currentProject!!.refreshProjectStatus()
        db.saveProject(currentProject!!)
        super.onDestroy()
    }

    inner class BricksAdapter : BaseAdapter {

        private var bricksList = ObservableArrayList<Brick>()
        private var context: Context? = null

        constructor(context: Context, bricksList: ObservableArrayList<Brick>) : super() {
            this.bricksList = bricksList
            this.context = context
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {

            val view: View?
            val vh: ViewHolder
            var currentBrick: Brick = getItem(position) as Brick

            if (convertView == null) {
                view = layoutInflater.inflate(R.layout.brick, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            vh.tvName.text = bricksList[position].name
            vh.tvState.text = bricksList[position].stateString

            vh.addButton.setOnClickListener{
                currentBrick.addToCurrent()
                notifyDataSetChanged()
            }

            vh.subButton.setOnClickListener{
                currentBrick.subFromCurrent()
                notifyDataSetChanged()
            }

            var imageBitmap = db.fetchImage(db.getBrickId(currentBrick.blockIdCode), currentBrick.colorCode)
            if (imageBitmap!=null){
                vh.imageView.setImageBitmap(imageBitmap)
            } else {
                var urlString = "https://www.lego.com/service/bricks/5/2/${currentBrick!!.imgCode}"
                var urlString2 = "https://www.bricklink.com/PL/${currentBrick!!.blockIdCode}.jpg"
                var urlString3 = "https://www.bricklink.com/P/${currentBrick!!.colorCode}/${currentBrick!!.blockIdCode}.gif"
                if (currentBrick!!.bitmap == null) {
                    var imageLoader = ImageAsyncDownloader(vh.imageView!!, currentBrick!!)
                    imageLoader.execute(urlString, urlString3, urlString2)
                } else {
                    vh.imageView!!.setImageBitmap(currentBrick!!.bitmap)
                }

            }

            return view
        }

        override fun getItem(position: Int): Any {
            return bricksList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return bricksList.size
        }

        inner class ImageAsyncDownloader: AsyncTask<String, Void, Bitmap> {
            override fun doInBackground(vararg p0: String?): Bitmap? {
                var urldisplay = p0[0]
                var bitmap: Bitmap? = null
                try {
                    val inS = java.net.URL(urldisplay).openStream()
                    bitmap = BitmapFactory.decodeStream(inS)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if(bitmap!=null)
                    db.saveImage(db.getBrickId(brick.blockIdCode), brick.colorCode, bitmap)
                else{
                    urldisplay = p0[1]
                    bitmap= null
                    try {
                        val inS = java.net.URL(urldisplay).openStream()
                        bitmap = BitmapFactory.decodeStream(inS)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    if(bitmap!=null)
                        db.saveImage(db.getBrickId(brick.blockIdCode), brick.colorCode, bitmap)
                    else{
                        urldisplay = p0[2]
                        bitmap= null
                        try {
                            val inS = java.net.URL(urldisplay).openStream()
                            bitmap = BitmapFactory.decodeStream(inS)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        if(bitmap!=null)
                            db.saveImage(db.getBrickId(brick.blockIdCode), brick.colorCode, bitmap)
                    }
                }
                return bitmap
            }

            var brickImage: ImageView
            var brick: Brick

            constructor(brickImage: ImageView, block: Brick) {
                this.brickImage = brickImage
                this.brick = block
            }

            override fun onPostExecute(result: Bitmap?) {
                brickImage.setImageBitmap(result)
                brick.bitmap = result
            }
        }
    }

    private class ViewHolder(view: View?) {
        val tvName: TextView
        val tvState: TextView
        val addButton: Button
        val subButton: Button
        val imageView: ImageView

        init {
            this.tvName = view?.findViewById<TextView>(R.id.tvName) as TextView
            this.tvState = view?.findViewById<TextView>(R.id.tvStateString) as TextView
            this.addButton = view?.findViewById<Button>(R.id.btnAddBrick) as Button
            this.subButton = view?.findViewById<Button>(R.id.btnSubBrick) as Button
            this.imageView = view?.findViewById<ImageView>(R.id.ivBrickImage) as ImageView
        }
    }
}
