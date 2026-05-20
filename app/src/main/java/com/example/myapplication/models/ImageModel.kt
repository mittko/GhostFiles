package com.example.myapplication.models

import android.graphics.Bitmap

data class ImageModel(var _detectedType:String, var _bytes: ByteArray, var _id:String
, var _name : String, var _bitmap: Bitmap?, val _createdAt : Long
) : FileModel {
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageModel

        if (_detectedType != other._detectedType) return false
        if (!_bytes.contentEquals(other._bytes)) return false
        if (_id != other._id) return false
        if (_bitmap != other._bitmap) return false

        return true
    }

    override fun hashCode(): Int {
        var result = _detectedType.hashCode()
        result = 31 * result + _bytes.contentHashCode()
        result = 31 * result + _id.hashCode()
        result = 31 * result + _bitmap.hashCode()
        return result
    }

}