package com.example.myapplication.models

import android.graphics.Bitmap

class AudioFileModel(var _detectedType:String, var _bytes: ByteArray, var _id:String,
                   var _name : String, var _bitmap: Bitmap, val _createdAt : Long) : FileModel {
    override fun getId(): String {
        return _id
    }

    override fun getName(): String {
        return _name
    }

    override fun getDetectedType(): String {
        return _detectedType
    }

    override fun getBytes(): ByteArray {
        return _bytes
    }

    override fun getBitmap(): Bitmap? {
        return _bitmap
    }

    override fun createAt(): Long {
        return _createdAt
    }
}