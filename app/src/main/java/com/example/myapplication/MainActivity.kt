package com.example.myapplication


import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fileencrpyptoraes256.ui.theme.FileEncrpyptorAes256Theme
import com.example.fileencrpyptoraes256.utils.BytesConvertor
import com.example.myapplication.models.PasswordViewModel
import com.example.myapplication.prefs.getEncryptionKeyFromPrefs
import com.example.myapplication.prefs.getIvFromPrefs
import com.example.myapplication.transform.decryptBiometricKey
import com.example.myapplication.transform.deleteOriginalFile
import com.example.myapplication.transform.encryptFile
import com.example.myapplication.uiboxes.AudioBox
import com.example.myapplication.uiboxes.DocBox
import com.example.myapplication.uiboxes.ExcelBox
import com.example.myapplication.uiboxes.ImageBox
import com.example.myapplication.uiboxes.MyAlertDialog
import com.example.myapplication.uiboxes.PdfBox
import com.example.myapplication.uiboxes.TextBox
import com.example.myapplication.uiboxes.VideoBox
import com.example.myapplication.utils.BiometricHelper.getBiometricPrompt
import com.example.myapplication.utils.BiometricHelper.getPromptInfo
import com.example.myapplication.utils.BiometricHelper.getSecretKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

var userDirectoryAsUri:Uri? = null


class MainActivity : FragmentActivity() {
    
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    @RequiresApi(Build.VERSION_CODES.R)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()
        var keepSplashScreen = true

        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        enableEdgeToEdge()

        // LOAD PREFERENCES
        val userThemePreferences = intPreferencesKey("USER_THEME")
        val userThemeFlow : Flow<Int> = dataStore.data.map {
            preferences ->
            preferences[userThemePreferences] ?: 0
        }
        var userThemeAsInt : Int = 0

        val userDirectorySaved = stringPreferencesKey("DIR_URI")
        val userDirectoryFlow : Flow<String?> = dataStore.data.map {
            preferences ->
            preferences[userDirectorySaved]
        }

        // must read data SYNCHRONOUSLY
        runBlocking {
           userThemeAsInt = userThemeFlow.first()
           userDirectoryAsUri = userDirectoryFlow.first()?.toUri()

        }

        keepSplashScreen = false// hide splash and show UI

        setContent {

            val viewModel : PasswordViewModel = viewModel()

            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            var userTheme by remember {
                mutableIntStateOf(userThemeAsInt)
            }

            var isDirectoryPickerDialogLaunched by remember {
                mutableStateOf(false)
            }

            var createdFiles by remember {
                mutableIntStateOf(0)
            }

            var decryptedKey by remember {
                mutableStateOf("")
            }

            var encryption by remember {
                mutableStateOf(false)
            }
            var decryption by remember {
                mutableStateOf(false)
            }



            // region ENCRYPT FILES OPEN BIOMETRIC PROMPT

            val multipleFileickerLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenMultipleDocuments()
            ) { uris ->

               if(uris.isEmpty()) return@rememberLauncherForActivityResult

                val iv = getIvFromPrefs(context as Activity)
                val decryptCipher = Cipher.getInstance("AES/CTR/NoPadding")
                decryptCipher.init(
                    Cipher.DECRYPT_MODE, getSecretKey(),
                    IvParameterSpec(Base64.decode(iv, Base64.NO_WRAP))
                )

                val biometricPrompt =
                    getBiometricPrompt(context as FragmentActivity) { authenticationResult ->

                        encryption = true
                        decryption = false

                        var decryptedKey = ""
                        val iv = getIvFromPrefs(context as Activity)
                        val decryptCipher = Cipher.getInstance("AES/CTR/NoPadding")
                        decryptCipher.init(
                            Cipher.DECRYPT_MODE, getSecretKey(),
                            IvParameterSpec(Base64.decode(iv, Base64.NO_WRAP))
                        )



                        val bytes = getEncryptionKeyFromPrefs(context)
                        decryptedKey = decryptBiometricKey(
                            Base64.decode(bytes, Base64.NO_WRAP),
                            authenticationResult.cryptoObject?.cipher!!
                        )
                            uris.forEach { uri ->
                                uri.let {


                                    viewModel.viewModelScope.launch() {
                                        withContext(Dispatchers.IO) {

                                            var mimeType:String? = null
                                            var filename:String? = null
                                            // --- Get MIME type ---
                                            mimeType = context.contentResolver.getType(it)

//                        // --- Get File Name ---
                                            context.contentResolver.query(
                                                it, null, null,
                                                null, null
                                            )?.use { cursor ->
                                                val nameIndex =
                                                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                                if (cursor.moveToFirst() && nameIndex >= 0) {
                                                    filename = cursor.getString(nameIndex)
                                                }
                                            }

                                            val treeUri =
                                                userDirectoryAsUri!!  // The folder selected by the user

                                            // Use DocumentFile.createFile(), it's the official recommended approach for SAF operations.
                                            val docDir = DocumentFile.fromTreeUri(context, treeUri)
                                            if (docDir?.canWrite() != true) {
                                                return@withContext
                                            }
                                            val indexOf = filename?.lastIndexOf(".")
                                            val newFileName = filename?.substring(0, indexOf!!)
                                            val fileUri = docDir.createFile(
                                                "application/octet-stream",
                                                "${newFileName}.enc"
                                            )

                                            if (fileUri != null) {


                                                context.contentResolver.openInputStream(it)
                                                    ?.use { inputStream ->
                                                        context.contentResolver.openOutputStream(
                                                            fileUri.uri
                                                        )
                                                            ?.use { fileOutputStream ->


                                                                encryptFile(
                                                                    fis = inputStream,
                                                                    fileOutputStream = fileOutputStream,
                                                                    mimeType!!,
                                                                    decryptedKey,
                                                                    context = context,
                                                                    fileUri = uri
                                                                )

                                                                createdFiles++
                                                            }
                                                    }


                                            }

                                        }

                                    }



                                }
                            }



                    }
                biometricPrompt.authenticate(
                    getPromptInfo(context),
                    BiometricPrompt.CryptoObject(decryptCipher)
                )
            }
            // endregion ENCRYPT FILES


            // region SELECT DIRECTORY TO SAVE FILES
            val dirPickerLauncher = rememberLauncherForActivityResult(
                contract = PermissibleOpenDocumentTreeDocument(false),
                onResult = {
                        maybeUri ->
                    maybeUri?.let {
                            uri ->
                        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        if(!checkUriPersisted(contentResolver = context.contentResolver, uri)) {
                           // context.contentResolver.releasePersistableUriPermission(uri, flags)
                            context.contentResolver.takePersistableUriPermission(uri, flags)
                        }
                        context.contentResolver.takePersistableUriPermission(uri, flags)
                       // isDirectoryPicked = true
                        userDirectoryAsUri = uri
                        scope.launch { saveDirectoryPicked(userDirectoryAsUri!!) }

                       // filePickerLauncher.launch("*/*")// opens all file types. You can filter (e.g. "image/*", "application/pdf").

                        multipleFileickerLauncher.launch(arrayOf("*/*"))

                    }
                }
            )
            // endregion SELECT DIRECTORY TO SAVE FILES




            FileEncrpyptorAes256Theme(darkTheme = true, userTheme) {

                Surface(tonalElevation = 5.dp) {

                    Scaffold(modifier = Modifier.fillMaxSize(),
                        bottomBar = {

                            Column {
                                val diff = viewModel.files.value.size - viewModel.fileMap.size

                                if(diff > 0) {
                                    Text(text = "You have $diff hidden files , unlock them to see it",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp, 0.dp, 0.dp, 0.dp))
                                }

                                BottomAppBar(

                                    actions = {


                                        // region DECRYPT FILES
                                        IconButton(onClick = {


                                            val iv = getIvFromPrefs(context as Activity)
                                            val decryptCipher =
                                                Cipher.getInstance("AES/CTR/NoPadding")
                                            decryptCipher.init(
                                                Cipher.DECRYPT_MODE, getSecretKey(),
                                                IvParameterSpec(Base64.decode(iv, Base64.NO_WRAP))
                                            )

                                            val biometricPrompt =
                                                getBiometricPrompt(context as FragmentActivity) { authenticationResult ->
                                                    val bytes = getEncryptionKeyFromPrefs(context)
                                                    decryptedKey = decryptBiometricKey(
                                                        Base64.decode(bytes, Base64.NO_WRAP),
                                                        authenticationResult.cryptoObject?.cipher!!
                                                    )

                                                    viewModel.password = decryptedKey

                                                    decryption = true
                                                    encryption = false

                                                }


                                            biometricPrompt.authenticate(
                                                getPromptInfo(context),
                                                BiometricPrompt.CryptoObject(decryptCipher)
                                            )


                                        }, Modifier.weight(1f),
                                            enabled = diff > 0) {


                                            Icon(
                                                painter = painterResource(R.drawable.outline_fingerprint_24),
                                                contentDescription = "Localized description"
                                            )
                                            if (viewModel.files.value.size > viewModel.fileMap.size) {
                                                Icon(
                                                    painter = painterResource(R.drawable.red_dot),
                                                    contentDescription = "red dot",
                                                    modifier = Modifier
                                                        .zIndex(1f)
                                                        .offset(x = 9.dp, y = (-9).dp),
                                                    tint = Color.Red
                                                )
                                            }
                                        }

                                        // endregion DECRYPT FILES

                                        // region CHANGE THEME
                                        IconButton(onClick = {
                                            userTheme = if (userTheme == 1) 0 else 1
                                            Log.e("Click", userTheme.toString())

                                            scope.launch {
                                                saveUserTheme(userTheme)
                                            }

                                        }, Modifier.weight(1f)) {

                                            Icon(
                                                painter = painterResource(R.drawable.ic_theme),
                                                contentDescription = "Localized description"
                                            )
                                        }
                                        // endregion CHANGE THEME


                                        // region SELECT DIRECTORY
                                        IconButton(onClick = {
                                            if (userDirectoryAsUri == null) {
                                                dirPickerLauncher.launch(Uri.EMPTY)
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "User directory already selected",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }, Modifier.weight(1f)) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_directory),
                                                contentDescription = "Localized description"
                                            )
                                        }
                                        // endregion SELECT DIRECTORY


                                    }, floatingActionButton = {

                                        // region OPEN FILE BROWSER TO ENCRYPT FILES
                                        FloatingActionButton(onClick = {

                                            if (userDirectoryAsUri == null) {

                                                isDirectoryPickerDialogLaunched = true
                                            } else {
                                                //    filePickerLauncher.launch("*/*")// opens all file types. You can filter (e.g. "image/*", "application/pdf").

                                                multipleFileickerLauncher.launch(arrayOf("*/*"))
                                            }
                                        }
                                            // endregion OPEN FILE BROWSER TO ENCRYPT FILES
                                            ,
                                            //   shape = MaterialTheme.shapes.medium,
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()) {
                                            Icon(
                                                Icons.Filled.Add,
                                                contentDescription = "Localized description"
                                            )
                                        }

                                        if (isDirectoryPickerDialogLaunched) {
                                            MyAlertDialog(onDismissRequest = {
                                                isDirectoryPickerDialogLaunched = false
                                            }, onConfirmation = {
                                                // тук се отваря файловия бразър за избор
                                                dirPickerLauncher.launch(Uri.EMPTY)
                                            }, dialogText = "")
                                        }

                                    })
                            }
                        }) { innerPadding ->

                        if(viewModel.password != "") {

                                if(decryption) {
                                    Box(modifier = Modifier.padding(innerPadding)) {
                                    StartDecryption("My Vault", viewModel.files.value,viewModel)
                                    }
                                } else { // encryption
                                    DrawLazyGrid(modifier = Modifier.padding(innerPadding),viewModel,"My Vault")
                                }

                        }

                        LaunchedEffect(createdFiles) {
                                userDirectoryAsUri?.let {
                                    val documentTree = DocumentFile.fromTreeUri(context,it)
                                    viewModel.files.value = documentTree?.listFiles()?.filter {
                                            doc -> doc.isFile
                                    }?.sortedByDescending { doc -> doc.lastModified() }  ?.toList() ?: emptyList()
                                }
                        }
                    }
                }
            }
        }



    }
    // region user actions
    suspend fun saveUserTheme(newUserTheme : Int) {
        val currentTheme : Preferences.Key<Int> = intPreferencesKey("USER_THEME")
        dataStore.edit {
            settings ->
            settings[currentTheme] = newUserTheme
        }
    }

    suspend fun saveDirectoryPicked(uri:Uri) {
        val currentDirectory : Preferences.Key<String> =
            stringPreferencesKey("DIR_URI")
        dataStore.edit {
            settings ->
            settings[currentDirectory] = uri.toString()
        }


    }

    fun checkUriPersisted(contentResolver: ContentResolver, uri:Uri) : Boolean {
        return contentResolver.persistedUriPermissions.any {
            permission -> permission.uri == uri
        }
    }
    // endregion user actions
}





@RequiresApi(Build.VERSION_CODES.R)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartDecryption(name:String, files:List<DocumentFile>
                    , viewModel: PasswordViewModel) {

    val context = LocalContext.current

    LaunchedEffect(files) {
        viewModel.decryptFiles(context, files)
    }


    DrawLazyGrid(modifier = Modifier,viewModel,name)
}


// region draw lazy grid
@Composable
private fun DrawLazyGrid(modifier : Modifier, viewModel : PasswordViewModel, name: String) {



        ConstraintLayout(
            modifier = modifier
                .fillMaxWidth()
                .padding(5.dp, 1.dp, 1.dp, 1.dp)

                .border(
                    width = 5.dp, color = Color(244, 244, 244), shape = RoundedCornerShape(12.dp)
                )
        ) {

            val (infoText, lazyColumn) = createRefs()


            val list = viewModel.fileMap.toList().sortedByDescending { it.second.createAt() }

            // region LazyGrid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .constrainAs(lazyColumn) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }) {


                items(list)
                { (fileUri, value) ->

                    val file = value
                    Card(
                        modifier = Modifier
                            .padding(2.dp)
                            //  .fillParentMaxWidth(.315f)
                            .fillMaxWidth(.315f)
                            .height(150.dp)
                            .border(
                                width = 1.dp, color = Color.Gray,
                                shape = RoundedCornerShape(size = 2.dp)
                            )
                    )
                    {


                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {

                            if (file.getDetectedType().contains("jpeg")) {

                                ImageBox(file)

                            } else if (file.getDetectedType().contains("plain")) {

                                TextBox(file)


                            } else if (file.getDetectedType().contains("pdf")) {

                                PdfBox(file)

                            } else if (file.getDetectedType().contains("docx")) {
                                DocBox(file)
                            } else if (file.getDetectedType().contains("excel")) {
                                ExcelBox(file)
                            } else if (file.getDetectedType().contains("mpeg")) {

                                AudioBox(
                                    file, decryptedBytes = file.getBytes(),
                                    file.getBitmap()!!, extension = "mp3"
                                )
                            } else if (file.getDetectedType().contains("mp4")) {
                                VideoBox(file)
                            }
                        }


                        Text(
                            text = "file name",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(5.dp, 5.dp, 0.dp, 5.dp)
                        )
                        Text(
                            text = file.getDetectedType(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = 5.dp,
                                top = 5.dp,
                                end = 0.dp,
                                bottom = 0.dp
                            )
                        )

                        Text(
                            text = BytesConvertor.formatFileSize(file.getBytes().size.toLong()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(5.dp, 5.dp, 0.dp, 5.dp)
                        )

                    }

                }

            }
        }




        // endregion Lazy Grid



}
// endregion draw lazy grid






// region Greeting Preview
@RequiresApi(Build.VERSION_CODES.R)
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FileEncrpyptorAes256Theme(true,
        themeSelected = 0) {
        Surface(tonalElevation = 5.dp) {
          //  DecryptFiles("My Vault",null!!, viewModel())
        }

    }
}
// endregion Greeting Preview








