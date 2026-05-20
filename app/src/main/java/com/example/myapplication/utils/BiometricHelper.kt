package com.example.myapplication.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.myapplication.prefs.getIvFromPrefs
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

@RequiresApi(Build.VERSION_CODES.R)
object BiometricHelper {
    private const val ENCRYPTION_TRANSFORMATION = "AES/CTR/NoPadding"
    // Android KeyStore provider
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    // Key alias for the secret key
    private const val KEY_ALIAS = "MyKeyAlias"
    // KeyStore instance
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE)


    init {
        // Load the KeyStore
        keyStore.load(null)
        // this is important , uncomment this if needed to refresh key if throw key user not aunthenticated
//        if (keyStore.containsAlias(KEY_ALIAS)) {
//            keyStore.deleteEntry(KEY_ALIAS)
//        }
//        // If key alias doesn't exist, create a new secret key
        if(!keyStore.containsAlias(KEY_ALIAS)) {
            createSecretKey()
        }
    }


    fun getBiometricPrompt(context: FragmentActivity, onAuthSucceed: (BiometricPrompt.AuthenticationResult) -> Unit) : BiometricPrompt {

        val biometricPrompt = BiometricPrompt(
            context, ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onAuthSucceed(result)
                }

                override fun onAuthenticationFailed() {
                    Log.e("err", "Authentication failed")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Log.e("err", "Authentication error")
                }
            })
        return biometricPrompt
    }
    // Register user biometrics

    // Register user biometrics
    fun registerUserBiometric(context: FragmentActivity,
                              onSuccess: (authResult: BiometricPrompt.AuthenticationResult) -> Unit = {}) {

        val biometricPrompt = getBiometricPrompt(context) {
            authenticationResult ->
            authenticationResult.cryptoObject?.cipher.let {
                    onSuccess(authenticationResult)

            }
        }
        val cipher = initEncryptionCipher()



        biometricPrompt.authenticate(getPromptInfo(context),
            BiometricPrompt.CryptoObject(cipher)
        )
    }

   // Create BiometricPrompt.PromptInfo with customized display text
    public fun getPromptInfo(context: FragmentActivity) : BiometricPrompt.PromptInfo  {
         return BiometricPrompt.PromptInfo.Builder()
             .setTitle("Biometric Prompt")
             .setSubtitle("just prompt")
             .setDescription("This is biometric test prompt")
             .setConfirmationRequired(false)
            // .setNegativeButtonText("close")
             .setAllowedAuthenticators(
                 BiometricManager.Authenticators.BIOMETRIC_STRONG or
                     BiometricManager.Authenticators.DEVICE_CREDENTIAL)
             .build()
    }
    // Initialize encryption cipher
    fun initEncryptionCipher(): Cipher {
        val cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        return cipher
    }
    fun getDecryptionCipher(context: Context) : Cipher {
        val iv = getIvFromPrefs(context as Activity)
        val decryptCipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION)
        decryptCipher.init(
            Cipher.DECRYPT_MODE,getSecretKey(),
            IvParameterSpec(Base64.decode(iv, Base64.NO_WRAP))
        )
        return decryptCipher
    }


    // Initialize encryption cipher
    @RequiresApi(Build.VERSION_CODES.R)
    fun createSecretKey() {

        val keyGenParams = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setBlockModes(KeyProperties.BLOCK_MODE_CTR)

            // ???? WHAT IS THIS //
            //WesetEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            (
                setUserAuthenticationRequired(true)
                    // By setting .setInvalidatedByBiometricEnrollment(false),
                    // you are telling Android: "I don't care if the user adds a new finger or a new face; keep this key alive."
                    .setInvalidatedByBiometricEnrollment(false)

                    // Required on Android 11+
                    .setUserAuthenticationParameters(
                        0,
                        KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
            )
                )
                // Invalidate the keys if the user has registered a new biometric
                // credential, such as a new fingerprint. Can call this method only
                // on Android 7.0 (API level 24) or higher. The variable
                // "invalidatedByBiometricEnrollment" is true by default.
           //     .setInvalidatedByBiometricEnrollment(true)
        }.build()

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(keyGenParams)
        keyGenerator.generateKey()
    }
    fun getSecretKey() : SecretKey {

        return try {
          keyStore.getKey(KEY_ALIAS,null) as SecretKey
        }catch (e : Exception) {
            // If the key is gone because the user disabled security:
            createSecretKey()
            keyStore.getKey(KEY_ALIAS,null) as SecretKey
        }
    }

    // Check if biometric authentication is available on device
    fun isBiometricAvailable(context: Context) : Boolean {
        val biometricManager = BiometricManager.from(context)
        return when(biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Toast.makeText(context, "Biometric authentication is available on this device", Toast.LENGTH_LONG).show()
                true
            }
            else -> {
                Toast.makeText(context, "Biometric authentication is not available on this device", Toast.LENGTH_LONG).show()
                false
            }
        }
    }

}