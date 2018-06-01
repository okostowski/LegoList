package com.example.kostowski.legolist

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import android.databinding.ObservableArrayList
import android.os.StrictMode
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.project.*

class MainActivity : AppCompatActivity(), NewProjectDialog.OnDialogInputListener {

    override fun onInput(nameInput: String, urlInput: String) {
        var newProjectId = db.addProjectFromUrl(nameInput,urlInput,this)
        projectsList.clear()
        projectsList.addAll(db.fetchProjects())
        projectsAdapter.notifyDataSetChanged()
    }

    val db: DatabaseHelper = DatabaseHelper(this)
    private var projectsList = ObservableArrayList<Project>()
    var projectsAdapter = ProjectsAdapter(this, projectsList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (android.os.Build.VERSION.SDK_INT > 9) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }

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


        projectsList.addAll(db.fetchProjects())

        lvProjects.adapter = projectsAdapter
        lvProjects.setOnItemClickListener { parent, view, i, l ->
            var item = lvProjects.getItemAtPosition(i) as Project
            var intent = Intent(this, ProjectView::class.java)
            intent.putExtra("projectId", item.projectId)
            startActivity(intent)
        }
    }

    fun addProject(v:View){
        var dialog = NewProjectDialog()
        dialog.show(fragmentManager, "NewProjectDialog")
    }


    inner class ProjectsAdapter : BaseAdapter {

        private var projectsList = ObservableArrayList<Project>()
        private var context: Context? = null

        constructor(context: Context, projectsList: ObservableArrayList<Project>) : super() {
            this.projectsList = projectsList
            this.context = context
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {

            val view: View?
            val vh: ViewHolder

            if (convertView == null) {
                view = layoutInflater.inflate(R.layout.project, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            vh.tvTitle.text = projectsList[position].name
            vh.tvContent.text = projectsList[position].getState()

            return view
        }

        override fun getItem(position: Int): Any {
            return projectsList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return projectsList.size
        }
    }

    private class ViewHolder(view: View?) {
        val tvTitle: TextView
        val tvContent: TextView

        init {
            this.tvTitle = view?.findViewById<TextView>(R.id.tvTitle) as TextView
            this.tvContent = view?.findViewById<TextView>(R.id.tvContent) as TextView
        }
    }
}
