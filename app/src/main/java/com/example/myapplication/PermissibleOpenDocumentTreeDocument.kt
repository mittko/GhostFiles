package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.unit.IntRect

class PermissibleOpenDocumentTreeDocument(private val write: Boolean = false)
    : ActivityResultContracts.OpenDocumentTree() {
    override fun createIntent(context: Context, input: Uri?): Intent {

        val intent =  super.createIntent(context, input)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if(write) {
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        return intent
    }

}