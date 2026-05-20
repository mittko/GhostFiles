package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.FragmentActivity
import com.example.myapplication.utils.BiometricHelper.registerUserBiometric
import com.example.myapplication.prefs.getEncryptionKeyFromPrefs
import com.example.myapplication.transform.encryptBiometricKey
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import kotlin.jvm.java

class EncryptKeyActivity : FragmentActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val encryptedKey = getEncryptionKeyFromPrefs(this)
        if (encryptedKey == "") {
            enableEdgeToEdge()
            setContent {
                MyApplicationTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        WelcomeScreen(
                            modifier =
                                Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        } else {
          val intent = Intent(this, MainActivity::class.java)
          startActivity(intent)
        }
    }
}
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun WelcomeScreen(modifier : Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    ConstraintLayout(modifier = modifier.fillMaxHeight()) {
        val (title, description, button) = createRefs()
        Text(text = "Secure Your Files", modifier = Modifier.constrainAs(title) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            verticalBias = 0.3f
        }.fillMaxHeight(0.1f).fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 23.sp)
        Text(text = "Confirm your identity to create your private encryption key. This ensures only you can access your files."
        , modifier = Modifier.constrainAs(description) {
             top.linkTo(title.bottom)
             start.linkTo(parent.start)
             end.linkTo(parent.end)
        }, textAlign = TextAlign.Center, fontSize = 18.sp)
        Button(onClick = {
           coroutineScope.launch {
               registerUserBiometric(context as FragmentActivity) {
                       authenticationResult ->
                   val plainKey = encryptBiometricKey(context as Activity,authenticationResult.cryptoObject?.cipher!!)

                   val intent = Intent(context, MainActivity::class.java)
                   context.startActivity(intent)
                   context.finish()
               }
           }
        }, modifier = modifier.constrainAs(button) {
            top.linkTo(description.bottom)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        }.padding(0.dp,30.dp)) {
            Text(text = "Enable Security")
        }
    }

}



@RequiresApi(Build.VERSION_CODES.R)
@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
    MyApplicationTheme {
        WelcomeScreen(modifier = Modifier)
    }
}