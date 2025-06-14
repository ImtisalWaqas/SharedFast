package com.example.assitwo.sharedfast

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.assitwo.sharedfast.model.NoteFolder
import com.example.assitwo.sharedfast.ui.theme.SharedFastAssi2Theme
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SharedFastAssi2Theme {
                HomeScreen()
            }
        }
    }
}

@Composable
fun HomeScreen() {
    val folders = remember { mutableStateListOf<NoteFolder>() }
    val context = LocalContext.current
    var selectedFolder by remember { mutableStateOf<NoteFolder?>(null) }

    LaunchedEffect(Unit) {
        val rootDir = File(context.getExternalFilesDir(null), "SharedFastNotes")
        if (rootDir.exists()) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            rootDir.listFiles()?.forEach { folder ->
                if (folder.isDirectory) {
                    val date = sdf.format(folder.lastModified())
                    folders.add(NoteFolder(folder.name, date))
                }
            }
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add New Folder") },
            text = {
                TextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val folderName = newFolderName.trim()
                    Log.d("AddFolder", "Trying to create folder: $folderName")
                    if (folderName.isNotBlank()) {
                        val currentDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                        val rootDir = File(context.getExternalFilesDir(null), "SharedFastNotes")
                        if (!rootDir.exists()) rootDir.mkdirs()
                        val newFolder = File(rootDir, folderName)
                        if (!newFolder.exists()) {
                            val created = newFolder.mkdirs()
                            Log.d("AddFolder", "Created: $created at ${newFolder.absolutePath}")
                            if (created) {
                                folders.add(NoteFolder(folderName, currentDate))
                            }
                        } else {
                            Log.d("AddFolder", "Folder already exists.")
                        }
                        newFolderName = ""
                        showDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(folders) { folder ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(context, FolderDetailScreen::class.java)
                                intent.putExtra("folder", folder)
                                context.startActivity(intent)
                            }
                            .padding(20.dp)
                    ) {
                        IconButton(onClick = {
                            selectedFolder = if (selectedFolder == folder) null else folder
                        }) {
                            Icon(
                                imageVector = if (selectedFolder == folder) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,                                contentDescription = "Select Folder"
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = folder.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Created: ${folder.createdTime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            fun shareSelectedFolder(appPackage: String?) {
                val folder = selectedFolder ?: return
                val folderFile = File(context.getExternalFilesDir(null), "SharedFastNotes/${folder.title}")
                val zipFile = File(context.cacheDir, "${folder.title}.zip")
                zipFolder(folderFile, zipFile)
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    context.packageName + ".provider",
                    zipFile
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    appPackage?.let { setPackage(it) }
                }
                try {
                    context.startActivity(Intent.createChooser(intent, "Share Folder"))
                } catch (e: Exception) {
                    Toast.makeText(context, "App not installed", Toast.LENGTH_SHORT).show()
                }
            }

            IconButton(onClick = { shareSelectedFolder("com.facebook.katana") }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_facebook),
                    contentDescription = "Facebook",
                    modifier = Modifier.size(36.dp),
                    tint = Color.Unspecified
                )
            }
            IconButton(onClick = { shareSelectedFolder("com.whatsapp") }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_whatsapp),
                    contentDescription = "WhatsApp",
                    modifier = Modifier.size(36.dp),
                    tint = Color.Unspecified
                )
            }
            IconButton(onClick = { shareSelectedFolder("com.google.android.gm") }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_gmail),
                    contentDescription = "Gmail",
                    modifier = Modifier.size(36.dp),
                    tint = Color.Unspecified
                )
            }
            IconButton(onClick = { shareSelectedFolder(null) }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bluetooth),
                    contentDescription = "Bluetooth",
                    modifier = Modifier.size(36.dp),
                    tint = Color.Unspecified
                )
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.End)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Folder")
        }
    }
}

fun zipFolder(sourceFolder: File, outputFile: File) {
    ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zos ->
        sourceFolder.walkTopDown().forEach { file ->
            val entryName = file.relativeTo(sourceFolder.parentFile!!).path
            if (file.isFile) {
                zos.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SharedFastAssi2Theme {
        Greeting("Android")
    }
}