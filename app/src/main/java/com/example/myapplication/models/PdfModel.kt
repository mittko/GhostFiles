package com.example.myapplication.models

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap

class PdfModel(var _detectedType:String, var _bytes: ByteArray, var _id:
  String, var _name : String, var _pageBitmaps:List<ImageBitmap>
, val _createAt : Long) : FileModel {
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
        return null
    }
    fun getPageBitmaps() : List<ImageBitmap> {
        return _pageBitmaps
    }

    override fun createAt(): Long {
        return _createAt
    }
}