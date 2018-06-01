package com.example.kostowski.legolist

import android.graphics.Bitmap

class Brick {
    var maxNumber: Int
    var collectedNumber: Int
    val blockIdCode: String
    val colorCode: Int
    val colorName: String
    val name: String
    val typeName: String
    val brickTypeCode: String
    val imgCode: String

    var stateString: String
    var done: Boolean
    var bitmap: Bitmap? = null


    constructor(name: String, typeName: String, colorCode: Int, blockIdCode: String, blockTypeCode: String, actualNumber: Int, maxNumber: Int, colorName: String, imgCode: String) {
        this.blockIdCode = blockIdCode
        this.collectedNumber = actualNumber
        this.maxNumber = maxNumber
        this.colorCode = colorCode
        this.colorName = colorName
        this.name = name
        this.typeName = typeName
        this.brickTypeCode = blockTypeCode
        this.stateString = collectedNumber.toString() +" / "+ maxNumber.toString()
        this.done = actualNumber==maxNumber
        this.imgCode = imgCode
    }

    fun addToCurrent(){
        if(collectedNumber<maxNumber) {
            this.collectedNumber++
            this.stateString = collectedNumber.toString() +" / "+ maxNumber.toString()
            this.done = collectedNumber==maxNumber
        }
    }

    fun subFromCurrent(){
        if(collectedNumber>0) {
            this.collectedNumber--
            this.stateString = collectedNumber.toString() +" / "+ maxNumber.toString()
            this.done = collectedNumber==maxNumber
        }
    }
}