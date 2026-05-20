package com.example.myapplication.models

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.transform.createTempFile
import com.example.myapplication.transform.decryptFileToByteArray
import com.example.myapplication.transform.extractMimeAndBytes
import com.example.myapplication.uiboxes.getDefaultIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.use

class PasswordViewModel() : ViewModel() {

    var password by mutableStateOf("")

    val fileMap = mutableStateMapOf<Uri?, FileModel>()

    var files = mutableStateOf<List<DocumentFile>>(emptyList())


    // ViewModel
    fun decryptFiles(context: Context, files: List<DocumentFile>) {
        viewModelScope.launch(Dispatchers.IO) {
            files.forEach { file ->


                if (fileMap.containsKey(file.uri)) return@forEach // вече декриптиран

                val inputStream = context.contentResolver.openInputStream(file.uri) ?: run {
                    return@forEach
                }
                val bytes = decryptFileToByteArray(password, inputStream)
                val (cleanBytes, detectedType) = extractMimeAndBytes(bytes)

                val decFile = when {
                    detectedType.contains("jpeg")  ->  {
                        val bitmap =
                            BitmapFactory.decodeByteArray(
                                cleanBytes, 0,
                                cleanBytes.size
                            )
                        ImageModel(
                            detectedType,
                            cleanBytes,
                            UUID.randomUUID().toString(),
                            file.name!!,
                            bitmap, file.lastModified())
                    }

                    detectedType.contains("pdf")   -> {
                            val descriptor =
                                createTempFile(
                                    context,
                                    cleanBytes
                                )
                            val pdfRenderer =
                                PdfRenderer(descriptor)
                            val bitmaps =
                                mutableListOf<ImageBitmap>()
                            for (i in 0 until pdfRenderer.pageCount) {
                                pdfRenderer.openPage(i)
                                    .use { page ->
                                        val bitmap =
                                            Bitmap.createBitmap(
                                                page.width,
                                                page.height,
                                                Bitmap.Config.ARGB_8888
                                            )
                                        page.render(
                                            bitmap,
                                            null,
                                            null,
                                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                        )
                                        bitmaps.add(bitmap.asImageBitmap())
                                    }
                            }
                            pdfRenderer.close()
                            descriptor.close()

                            PdfModel(detectedType,
                                cleanBytes,
                                UUID.randomUUID().toString(),
                                file.name!!,
                                bitmaps, file.lastModified())
                        }
                       detectedType.contains("plain") -> {

                        TextModel(
                            detectedType,
                            cleanBytes,
                            UUID.randomUUID().toString(),
                            file.name!!,
                            getDefaultIcon(context,
                                R.drawable.outline_contract_24)
                            , file.lastModified())
                        }

                        detectedType.contains("docx")  -> {

                            TextModel(
                                detectedType,
                                cleanBytes,
                                UUID.randomUUID().toString(),
                                file.name!!,
                                getDefaultIcon(
                                    context,
                                    R.drawable.word_doc_thumb
                                ), file.lastModified())
                        }

                        detectedType.contains("excel") -> {
                            TextModel(
                                detectedType,
                                cleanBytes,
                                UUID.randomUUID().toString(),
                                file.name!!,
                                getDefaultIcon(
                                    context,
                                    R.drawable.excell_thumb
                                ), file.lastModified())
                        }

                        detectedType.contains("mpeg")  -> {
                            AudioFileModel(
                                detectedType,
                                cleanBytes,
                                UUID.randomUUID().toString(),
                                file.name!!,
                                getDefaultIcon(
                                    context,
                                    R.drawable.audio_thumb
                                ),file.lastModified())
                        }

                        detectedType.contains("mp4")   -> {
                            VideoFileModel(
                                detectedType,
                                cleanBytes,
                                UUID.randomUUID().toString(),
                                file.name!!,
                                getDefaultIcon(
                                    context,
                                    R.drawable.baseline_videocam_24
                                ),file.lastModified())
                        }
                    else -> null
                } ?: return@forEach

                fileMap[file.uri] = decFile // mutableStateMapOf е thread-safe
            }
        }
    }
}