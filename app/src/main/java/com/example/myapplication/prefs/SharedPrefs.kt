package com.example.myapplication.prefs

import android.app.Activity
import android.content.Context
import android.util.Base64

private const val SAVE_ENCRYPTION_KEY : String = "SAVE_ENCRYPTION_KEY"
private const val SAVE_ENCRYPTION_IV : String = "SAVE_ENCRYPTION_IV"

fun saveEncryptionKeyToPrefs(context: Activity, byteArray: ByteArray) {
    val sharedPreferences = context.getSharedPreferences(SAVE_ENCRYPTION_KEY,Context.MODE_PRIVATE) ?: return
    with(sharedPreferences.edit()) {
        putString(SAVE_ENCRYPTION_KEY, Base64.encodeToString(byteArray, Base64.NO_WRAP))
        apply()
    }
}

fun getEncryptionKeyFromPrefs(context: Activity) : String {
    val sharedPreferences = context.getSharedPreferences(SAVE_ENCRYPTION_KEY,Context.MODE_PRIVATE) ?: return ""
    val defaultValue = ""
    return sharedPreferences.getString(SAVE_ENCRYPTION_KEY,defaultValue)!!
}

fun saveIvToPrefs(context: Activity, byteArray: ByteArray) {
    val sharedPreferences = context.getSharedPreferences(SAVE_ENCRYPTION_IV,Context.MODE_PRIVATE) ?: return
    with(sharedPreferences.edit()) {
        putString(SAVE_ENCRYPTION_IV, Base64.encodeToString(byteArray, Base64.NO_WRAP))
        apply()
    }
}

fun getIvFromPrefs(context: Activity) : String {
    val sharedPreferences = context.getSharedPreferences(SAVE_ENCRYPTION_IV,Context.MODE_PRIVATE) ?: return ""
    val defaultValue = ""
    return sharedPreferences.getString(SAVE_ENCRYPTION_IV,defaultValue)!!
}