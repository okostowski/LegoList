package com.example.kostowski.legolist

import android.app.DialogFragment
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import kotlinx.android.synthetic.main.newproject_layout.*

class NewProjectDialog: DialogFragment() {

    interface OnDialogInputListener{
        fun onInput(nameInput: String, url: String)
    }
    var listener: OnDialogInputListener? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        var view = inflater!!.inflate(R.layout.newproject_layout, container, false)

        view.findViewById<Button>(R.id.addProjectBtn).setOnClickListener {
            var inputUrl = urlText.text.toString()
            var name = projectNameText.text.toString()
            listener?.onInput(name, inputUrl)

            dialog.dismiss()
        }

        return view
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try{
            listener = activity as OnDialogInputListener
        }catch (e: ClassCastException){
            e.printStackTrace()
        }
    }
}