package com.example.assitwo.sharedfast

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.assitwo.sharedfast.model.NoteFolder
import com.example.assitwo.sharedfast.ui.theme.SharedFastAssi2Theme
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class FolderDetailScreen : ComponentActivity() {
    private var folder: NoteFolder? = null
    private lateinit var targetDir: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        folder = intent.getParcelableExtra("folder")
        targetDir = File(getExternalFilesDir(null), "SharedFastNotes/${folder?.title}")
        if (!targetDir.exists()) targetDir.mkdirs()

        setContent {
            SharedFastAssi2Theme {
                folder?.let {
                    FolderContentScreen(it, this, targetDir)
                } ?: Text("Folder not found")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || folder == null) return

        if (!targetDir.exists()) targetDir.mkdirs()

        when (requestCode) {
            REQUEST_IMPORT_FILE, REQUEST_IMPORT_IMAGE -> {
                val uri = data?.data ?: return
                val name = getFileNameFromUri(uri, contentResolver) ?: UUID.randomUUID().toString()
                val targetFile = File(targetDir, name)
                copyFileFromUri(uri, targetFile)
                Toast.makeText(this, "Imported: $name", Toast.LENGTH_SHORT).show()
                recreate()
            }
            REQUEST_CAPTURE_IMAGE -> {
                Toast.makeText(this, "Image captured", Toast.LENGTH_SHORT).show()
                recreate()
            }
        }
    }

    private fun copyFileFromUri(uri: Uri, targetFile: File) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(targetFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getFileNameFromUri(uri: Uri, resolver: ContentResolver): String? {
        val returnCursor = resolver.query(uri, null, null, null, null)
        returnCursor?.use {
            val nameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            it.moveToFirst()
            return it.getString(nameIndex)
        }
        return null
    }

    companion object {
        const val REQUEST_IMPORT_FILE = 101
        const val REQUEST_IMPORT_IMAGE = 102
        const val REQUEST_CAPTURE_IMAGE = 103
    }
}

@Composable
fun FolderContentScreen(folder: NoteFolder, activity: Activity, folderDir: File) {
    val context = LocalContext.current
    var fileList by remember { mutableStateOf(listOf<File>()) }

    LaunchedEffect(Unit) {
        fileList = folderDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "*/*"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                activity.startActivityForResult(intent, FolderDetailScreen.REQUEST_IMPORT_FILE)
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90CAF9))) {
                Text("import files")
            }

            Button(onClick = {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                activity.startActivityForResult(intent, FolderDetailScreen.REQUEST_IMPORT_IMAGE)
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA5D6A7))) {
                Text("import images")
            }

            Button(onClick = {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val imageFileName = "IMG_$timeStamp.jpg"
                val imagePath = File(activity.getExternalFilesDir(null), "SharedFastNotes/${folder.title}/$imageFileName")
                imagePath.parentFile?.mkdirs()

                val uri = FileProvider.getUriForFile(
                    activity,
                    "${activity.packageName}.provider",
                    imagePath
                )

                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                activity.startActivityForResult(intent, FolderDetailScreen.REQUEST_CAPTURE_IMAGE)
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF59D))) {
                Text("capture image")
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(fileList) { file ->
                if (file.extension in listOf("jpg", "jpeg", "png")) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(file),
                            contentDescription = file.name,
                            modifier = Modifier
                                .size(100.dp)
                                .background(Color(0xFFFFF9C4)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = file.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            val date = SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.getDefault())
                                .format(Date(file.lastModified()))
                            Text(text = "Date: $date", fontSize = 14.sp)
                        }
                    }
                } else {
                    Text(text = file.name, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
