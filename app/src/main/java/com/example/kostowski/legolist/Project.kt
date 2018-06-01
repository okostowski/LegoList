package com.example.kostowski.legolist

import android.content.Context
import android.databinding.ObservableArrayList

class Project(n: String, id: Int, active: Boolean) {

    val name: String = n
    val projectId: Int = id
    var active: Boolean = active
    var listOfNeededBricks: ObservableArrayList<Brick> = ObservableArrayList()

    fun makeProjectFromLink(url: String, c: Context){
        listOfNeededBricks= XMLParser(c).parseFromLink(url) as ObservableArrayList<Brick>
    }

    fun refreshProjectStatus(){
        var howSIt = false
        for(block in listOfNeededBricks){
            if(block.collectedNumber<block.maxNumber)
                howSIt = true
        }
        active = howSIt
    }

    fun endProject(){
        active = false
    }

    fun restartProject(){
        active = true
    }

    fun getState(): String{
        return if (active) "Aktywny" else "Zarchiwizowany"
    }
}