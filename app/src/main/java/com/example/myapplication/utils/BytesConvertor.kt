package com.example.myapplication.utils

class BytesConvertor {

    companion object {
        public fun formatFileSize(bytes:Long) :String {
            val kb = bytes / 1024.0
            return when {
                kb < 1024 -> "%.1f KB".format(kb)
                else ->
                {
                    val mb = kb / 1024.0
                    "%.1f MB".format(mb)
                }
            }
        }
    }

}