package com.example.myapplication.models

import android.graphics.Bitmap

interface FileModel {


    fun getId() : String
    fun getName() : String
    fun getDetectedType() : String
    fun getBytes() : ByteArray
    fun getBitmap() : Bitmap?
    fun createAt() : Long
}
