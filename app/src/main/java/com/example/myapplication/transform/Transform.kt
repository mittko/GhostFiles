package com.example.myapplication.transform

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import com.example.myapplication.prefs.saveEncryptionKeyToPrefs
import com.example.myapplication.prefs.saveIvToPrefs
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

fun encryptBiometricKey(context: Activity, cipher: Cipher) : String {
    val rnd = SecureRandom()
    val bytes = ByteArray(16)
    rnd.nextBytes(bytes)

    val encryptedBytes = cipher.doFinal(bytes)


    saveEncryptionKeyToPrefs(context,encryptedBytes)
    saveIvToPrefs(context,cipher.iv)

    // 4. Return the RAW key as a Base64 string to use as your "password"
    return Base64.encodeToString(bytes,Base64.NO_WRAP)
}

@RequiresApi(Build.VERSION_CODES.R)
fun decryptBiometricKey(byteArray: ByteArray,cipher: Cipher) : String {

    val decryptedBytes = cipher.doFinal(byteArray)
    // 4. Return the RAW key as a Base64 string to use as your "password"
    return Base64.encodeToString(decryptedBytes,Base64.NO_WRAP)
}
fun encryptFile(
    fis: InputStream,
    fileOutputStream: OutputStream,
    fileType: String,
    password: String,
    context: Context,
    fileUri: Uri
) {
    // 1. Setup cryptography
    val pass = Base64.decode(password, Base64.NO_WRAP)
    val secretKeySpec = SecretKeySpec(pass, "AES")
    val cipher = Cipher.getInstance("AES/CTR/NoPadding")

    val secureRandom = SecureRandom()
    val iv = ByteArray(16)
    secureRandom.nextBytes(iv)
    val ivParameterSpec = IvParameterSpec(iv)

    // 2. Write IV to the output stream first
    fileOutputStream.write(iv)

    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)

    // 3. Process streams using Kotlin's .use to guarantee automatic, safe closure
    try {
        fis.use { input ->
            fileOutputStream.use { output ->
                CipherOutputStream(output, cipher).use { cipherOutputStream ->

                    // Stream data block-by-block
                    val byteArray = ByteArray(1024)
                    var readBytes: Int
                    while (input.read(byteArray).also { readBytes = it } != -1) {
                        cipherOutputStream.write(byteArray, 0, readBytes)
                    }

                    // Append the file type metadata
                    cipherOutputStream.write(fileType.toByteArray(Charsets.UTF_8))
                    cipherOutputStream.flush()
                }
            }
        }

        // 4. Delete the original file ONLY after streams are safely closed and flushed
        val deleted = DocumentsContract.deleteDocument(context.contentResolver, fileUri)
        if (!deleted) {
            Log.w("Encryption", "File deletion returned false")
        }

    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("Encryption", "Encryption or deletion failed: ${e.message}")
    }
}




fun decryptFileToByteArray(password:String, inputStream_: InputStream) : ByteArray {
    // Decode the Base64 password back to exactly 16 bytes
    val keyBytes = Base64.decode(password,Base64.NO_WRAP)
    val secretKeySpec = SecretKeySpec(keyBytes/*password.toByteArray(Charsets.UTF_8)*/,"AES")
    val cipher = Cipher.getInstance("AES/CTR/NoPadding")

    val byteArray2 = ByteArray(16)
    inputStream_.read(byteArray2)
    val ivParameterSpec = IvParameterSpec(byteArray2)

    cipher.init(Cipher.DECRYPT_MODE,secretKeySpec,ivParameterSpec)

    val baos = ByteArrayOutputStream()
    val byteArray = ByteArray(1024)
    var readBytes:Int
    CipherInputStream(inputStream_,cipher).use {
                cipherInputStream ->
            while(cipherInputStream.read(byteArray).also {
                    readBytes = it } != -1) {
                baos.write(byteArray,0,readBytes)

            }

    }
    return baos.toByteArray()
}

fun createTempFile(context: Context, pdfBytes: ByteArray) : ParcelFileDescriptor {
   val tempFile = File.createTempFile("decrypted_",".pdf",context.cacheDir)
   tempFile.outputStream().use {
         it.write(pdfBytes)
   }
    return ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
}


fun extractMimeAndBytes(decryptedBytes: ByteArray): Pair<ByteArray, String> {
    // Scan backwards from end - MIME type is always printable ASCII
    var mimeStart = decryptedBytes.size
    for (i in decryptedBytes.indices.reversed()) {
        val b = decryptedBytes[i].toInt().and(0xFF)
        if (b < 32 || b > 126) { // hit non-printable byte = end of file content
            mimeStart = i + 1
            break
        }
        if (decryptedBytes.size - i > 100) { // safety limit
            mimeStart = i + 1
            break
        }
    }
    val mimeType = String(decryptedBytes.copyOfRange(mimeStart, decryptedBytes.size), Charsets.UTF_8)
        .dropWhile { !it.isLetter() && it != '.' }
    val fileBytes = decryptedBytes.copyOfRange(0, mimeStart)
    return Pair(fileBytes, mimeType)
}




