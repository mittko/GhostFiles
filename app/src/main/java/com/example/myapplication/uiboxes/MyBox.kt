package com.example.myapplication.uiboxes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.myapplication.R
import com.example.myapplication.models.FileModel
import com.example.myapplication.models.PdfModel
import com.example.myapplication.userDirectoryAsUri
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLConverter
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
fun ImageBox(file : FileModel) {
    var selectedBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    val imageBitmap = remember(file) {
        file.getBitmap()?.asImageBitmap()
    }
    imageBitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
                .padding(4.dp)
                .clickable {
                    selectedBitmap = file.getBitmap()

                }
            , contentScale = ContentScale.Crop
        )
    } ?: CircularProgressIndicator()

    // endregion Lazy Grid
    selectedBitmap?.let { bitmap ->

        FullScreenDialog(file = file, onDismiss = {
            selectedBitmap = null
        }) {
            OpenFullScreenImage(bitmap)
        }
    }
}
@Composable
fun OpenFullScreenImage(bitmap: Bitmap) {
    // 1. Manage state for scale and position
    var scale by remember {
        mutableFloatStateOf(1f)
    }
    var offset by remember {
        mutableStateOf(Offset.Zero)
    }
    // 2. Define gesture behavior (limit scale between 1x and 5x)
    val state = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f,5f)
        offset += pan
    }
    Box(modifier = Modifier.fillMaxSize().transformable(state = state)) {
        Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Full Screen Image",
            modifier = Modifier.fillMaxSize().graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            ),
            contentScale = ContentScale.Fit)
    }

}


@Composable
fun TextBox(file : FileModel) {
    var selectedText by remember {
        mutableStateOf<String?>(null)
    }

    file.getBytes().let {
        val lines = String(
            file.getBytes(),
            Charsets.UTF_8
        ).split("\n")
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clickable {
                    selectedText = lines.joinToString("\n")
                }
        ) {
            items(lines) {
                Text(text = it)
            }
        }
    }
    selectedText?.let { text ->
        FullScreenDialog(file = file,onDismiss = {
            selectedText = null
        })  {
            Text(text = text)
        }
    }
}

@Composable
fun PdfBox(file : FileModel) {
    var selectedPdfBitmap by remember {
        mutableStateOf<ImageBitmap?>(null)
    }
    val pdfModel: PdfModel = file as PdfModel

    if (pdfModel.getPageBitmaps().isEmpty()) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(pdfModel.getPageBitmaps()) { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = "PDF Page $0",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clip(
                            RoundedCornerShape(
                                8.dp
                            )
                        )
                        .background(Color.White)
                        .clickable {
                            selectedPdfBitmap = bitmap
                        }
                )
            }
        }
    }
    selectedPdfBitmap?.let { bitmap ->
        FullScreenDialog(file = file, onDismiss = {
            selectedPdfBitmap = null
        }) {
            OpenFullPdf(bitmap)
        }
    }
}
@Composable
fun OpenFullPdf(bitmap : ImageBitmap) {
// 1. Manage state for scale and position
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

// 2. Define gesture behavior (limit scale between 1x and 5x)
    val state = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 5f)
        offset += pan
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .transformable(state = state) // 3. Detect gestures
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = "PDF Page",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer( // 4. Apply transformations
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }

}


@Composable
fun ExcelBox(file : FileModel) {
    var openExcel by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {


        val imageBitmap = file.getBitmap()!!.asImageBitmap()
        Image(
            bitmap = imageBitmap,
            contentDescription = "excel file",
            Modifier.size(50.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Button(onClick = {

            openExcel = true


        }) {
            Text(
                text = "Open File",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
    if(openExcel) {
        FullScreenDialog(file, onDismiss = {
           openExcel = false
        }) {
            ExcelDialogContent(file.getBytes())
        }
    }

}
fun convertExcelToHtmlSafe(bytes: ByteArray): String {
    val html = StringBuilder()
    // Inject custom CSS to make it look like an accurate web spreadsheet
    html.append("<html><head><style>")
    html.append("table { border-collapse: collapse; width: 100%; font-family: sans-serif; }")
    html.append("td { border: 1px solid #ccc; padding: 8px; font-size: 14px; min-width: 80px; }")
    html.append("tr:nth-child(even) { background-color: #f9f9f9; }")
    html.append("</style></head><body><table>")

    try {
        ByteArrayInputStream(bytes).use { inputStream ->
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0) // Grabs sheet 1

            for (row in sheet) {
                html.append("<tr>")
                for (cn in 0 until row.lastCellNum) {
                    val cell = row.getCell(cn, org.apache.poi.ss.usermodel.Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)

                    val cellValue = when (cell.cellType) {
                        CellType.STRING -> cell.stringCellValue
                        CellType.NUMERIC -> cell.numericCellValue.toString()
                        CellType.BOOLEAN -> cell.booleanCellValue.toString()
                        else -> ""
                    }

                    html.append("<td>").append(cellValue).append("</td>")
                }
                html.append("</tr>")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return "<html><body>Error loading Excel sheet</body></html>"
    }

    html.append("</table></body></html>")
    return html.toString()
}
@Composable
fun ExcelDialogContent(decryptedBytes: ByteArray) {
    // Generate the HTML from the Excel bytes
    val htmlContent = remember(decryptedBytes) { convertExcelToHtmlSafe(decryptedBytes) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .padding(8.dp)
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.javaScriptEnabled = false
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@Composable
fun DocBox(file : FileModel) {
    val context = LocalContext.current
    var openWordFile by remember {
        mutableStateOf(false)
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val imageBitmap = file.getBitmap()
        imageBitmap?.let {
            Image(
                bitmap = imageBitmap
                    .asImageBitmap(),
                contentDescription = "word file",
                modifier = Modifier.size(50.dp)
            )
        } ?: CircularProgressIndicator()

        Spacer(modifier = Modifier.height(6.dp))

        Button(onClick = {

            openWordFile = true

        }) {
            Text(
                text = "Open Document",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
    if(openWordFile) {
        FullScreenDialog(file = file, onDismiss = {
            openWordFile = false
        }) {
            WordDialogContent(file.getBytes())
        }
    }
}
@Composable
fun WordDialogContent(decryptedBytes: ByteArray) {
    // Generate the styled HTML string from document bytes
    val htmlContent = remember(decryptedBytes) { convertWordToHtml(decryptedBytes) }


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp) // Bound the dialog content
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    // 🔍 Enable pinch-to-zoom
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true

                    // 🎛️ Hide the ugly +/- zoom buttons on screen
                    settings.displayZoomControls = false
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    // Block execution of scripts for extra security
                    settings.javaScriptEnabled = false
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
fun convertWordToHtml(bytes: ByteArray): String {
    return try {
        ByteArrayInputStream(bytes).use { inputStream ->
            val document = XWPFDocument(inputStream)
            ByteArrayOutputStream().use { outputStream ->
                // This preserves tables, alignment, and spacing as HTML
                XHTMLConverter.getInstance().convert(document, outputStream, XHTMLOptions.create())
                outputStream.toString("UTF-8")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        "<html><body>Error loading document</body></html>"
    }
}


@Composable
fun AudioBox(file : FileModel, decryptedBytes: ByteArray, bitmap:Bitmap, extension:String) {
    val context = LocalContext.current
   var openAudio by remember {
       mutableStateOf(false)
   }
    val audioFile = remember {
        File(context.cacheDir,"audio_${System.currentTimeMillis()}.$extension").also {
            it.writeBytes(decryptedBytes)
        }

    }



    val audioPlayer = remember {
        MediaPlayer().apply {
            setDataSource(audioFile.absolutePath)
            prepare()
            setOnCompletionListener {
               it.seekTo(0)
           }
        }

    }
    DisposableEffect(Unit) {
        onDispose {
            if(audioPlayer.isPlaying) {
                audioPlayer.pause()
            }
            audioPlayer.seekTo(0)
            audioFile.delete()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.DarkGray)
        .clickable {
            openAudio = true
        }
        .padding(16.dp)) {

        Column {
            Image(remember {
                bitmap.asImageBitmap()
            }, contentDescription = "audio file")
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = file.getName(), color = Color.White)
        }
    }
    if(openAudio) {
        FullScreenDialog(file, onDismiss = {
           openAudio = false
           if(audioPlayer.isPlaying) {
               audioPlayer.pause()
           }
            audioPlayer.seekTo(0)
        }) {

            Column (modifier = Modifier
                .fillMaxSize()) {
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "audio file icon",
                    modifier = Modifier.fillMaxSize())
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = file.getName(), color = Color.White, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
                Image(painter = painterResource(id = R.drawable.baseline_play_arrow_24),
                    contentDescription = "play audio", modifier = Modifier.fillMaxSize()
                        .clickable {
                            audioPlayer.start()
                        })
            }

        }
    }
}

fun createVideoThumbnail(context: Context, videoBytes: ByteArray, extension: String = "mp4"): Pair<File,Bitmap?> {
    val videoFile = File(context.cacheDir, "video_${System.currentTimeMillis()}.$extension")

    var bitmap: Bitmap? = null

    // 1️⃣ Ensure the file is fully closed before proceeding
    videoFile.outputStream().use { it.write(videoBytes) }

    val retriever = MediaMetadataRetriever()
    try {
        // 2️⃣ Use absolute path for reliability with internal cache files
        retriever.setDataSource(videoFile.absolutePath)

        // 3️⃣ Use 0 instead of 1,000,000 to ensure you get a frame even in short videos
        // Use OPTION_CLOSEST_SYNC for better reliability
        bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

        if (bitmap == null) {
            Log.e("Thumbnail", "Video frame returned NULL. The file might be corrupt or incompatible.")
        }
    } catch (e: Exception) {
        Log.e("Thumbnail", "Retriever failed: ${e.message}")
    } finally {
        retriever.release()
    }

    return videoFile to bitmap
}
@Composable
fun VideoBox(file : FileModel) {
    val context = LocalContext.current

    var thumbnail by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var videoFile by remember {
        mutableStateOf<File?>(null)
    }
    var openVideo by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        val result =  createVideoThumbnail(context,file.getBytes())

        thumbnail = result.second

        videoFile = result.first
    }

    Box(modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(Color.Black)
        .clickable {
            openVideo = true

        }) {
        if(thumbnail != null) {
            Image(bitmap = thumbnail!!.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop
            , modifier = Modifier.fillMaxSize())

            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null,
                tint = Color.White, modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center))
        } else {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
        }
        if(openVideo) {
            videoFile?.let {
                FullScreenDialog(file = file, onDismiss = {
                    openVideo = false
                }) {
                    //   playVideo(context, it)
                    VideoStreamPlayer(it)
                }

            }
        }
    }
}
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoStreamPlayer(videoFile : File) {
    val context = LocalContext.current
    val videoUri = FileProvider.getUriForFile(context,"${context.packageName}.provider",videoFile)

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true // Start automatically

        }
    }


    // Release player when the composable leaves the screen
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Bridge the PlayerView into Compose
    AndroidView(
        factory = { ctx ->
            // Requires Media3 Compose UI dependency


            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxWidth().fillMaxHeight().aspectRatio(9/16f)
    )
}

@Composable
fun FullScreenDialog(file : FileModel, onDismiss: () -> Unit, content : @Composable () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = onDismiss
        , properties = DialogProperties(usePlatformDefaultWidth = false)) { // This allows the dialog to be full screen

        Surface(Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {

                Row {
                    IconButton(onClick = onDismiss, modifier =
                        Modifier.padding(50.dp,0.dp,100.dp,0.dp)) {
                        Icon(Icons.Default.Close,"Close Dialog")
                    }
                    IconButton(onClick = {

                        scope.launch {
                            val origiName = file.getName()
                            val lastIndex = origiName.lastIndexOf(".")
                            val fileName = origiName.substring(0,lastIndex)
                            val mimeType = file.getDetectedType()
                            val fileExtension = when {
                                mimeType.contains("jpeg") -> "jpg"
                                mimeType.contains("png") -> "png"
                                mimeType.contains("plain") -> "txt"
                                mimeType.contains("pdf") -> "pdf"
                                mimeType.contains(".docx") || mimeType.contains("wordprocessingml") -> "docx" // ← first!
                                mimeType.contains("doc") -> "doc"
                                mimeType.contains("excel") -> "xls"
                                mimeType.contains("mpeg") -> "mp3"
                                mimeType.contains("mp4") -> "mp4"
                                else -> ""
                            }
                            val allBytes = file.getBytes()
                            //    val actualBytes = allBytes.copyOfRange(0, allBytes.size - 5) // strip ".docx"
                            //   val last20 = allBytes.takeLast(20)
                            //   Log.d("SAVE_DEBUG", "last20_hex=${last20.map { it.toInt().and(0xFF).toString(16) }}")
                            //    Log.d("SAVE_DEBUG", "last20_str='${String(last20.toByteArray(), Charsets.UTF_8)}'")
                            //    Log.d("SAVE_DEBUG", "total_size=${allBytes.size}")

                            val result = saveBytesToFileAsync(context, allBytes, "$fileName.$fileExtension")
                            result.onSuccess {
                                Toast.makeText(context,"download completed!",Toast.LENGTH_SHORT).show()
                            }
                            result.onFailure {
                                Toast.makeText(context,"download failed!",Toast.LENGTH_SHORT).show()
                            }
                        }

                    }) {
                        Icon(painter = painterResource(R.drawable.outline_download_24), contentDescription = "Save file on disk")
                    }
                }

                Box(modifier = Modifier
                    .fillMaxSize()// Standard "Gallery" look
                    .clickable {
                        onDismiss() // Close when clicking background
                    }
                    .verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center
                ) {
                    content()
                }
            }
        }

    }
}

suspend fun saveBytesToFileAsync(
    context: Context,
    bytes: ByteArray,
    fileName: String
): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        val parentUri = userDirectoryAsUri ?: error("No folder selected")

        val parentDoc = DocumentFile.fromTreeUri(context, parentUri)
            ?: error("Invalid URI")

        val subFolder = parentDoc.findFile("DecryptedFiles")
            ?: parentDoc.createDirectory("DecryptedFiles")
            ?: error("Cannot create folder")

        val newFile = subFolder.createFile("application/octet-stream", fileName)
            ?: error("Cannot create file")

        context.contentResolver.openOutputStream(newFile.uri)?.use {
            it.write(bytes)
        } ?: error("Cannot open stream")
    }
}

//
@Composable
fun MyAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String = "Directory permission required",
    dialogText: String,
    icon: ImageVector = Icons.Default.Info
) {
    AlertDialog(icon = {
        Icon(icon, contentDescription = "alert dialog icon")
    }, onDismissRequest = {
        onDismissRequest()
    }, confirmButton = {
        TextButton(onClick = {
            onConfirmation()
        }) {
            Text(text = "Ok")
        }
    }, dismissButton = {
        TextButton(onClick = {
            onDismissRequest()
        }) {
            Text(text = "Cancel")
        }
    }, title = {
        Text(text = dialogTitle)
    }, text = {
        Text(text = dialogText)
    })
}
// endregion get default icon


// region get default icon
fun getDefaultIcon(context: Context, icon: Int): Bitmap {
    val drawable = ContextCompat.getDrawable(context, icon)


    val bitmap = createBitmap(drawable!!.intrinsicWidth, drawable.intrinsicHeight)

    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}




