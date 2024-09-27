package com.example.mediastore

import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.mediastore.ui.theme.MediastoreTheme

typealias Path = String

class MainActivity : ComponentActivity() {

    private var list: MutableList<Path> = mutableListOf()

    private var showList by mutableStateOf(false)
    private var showImageDialog by mutableStateOf(false)
    private var imageUrl: Path = ""
    private var selectedFolder: Folder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkForPermission()
        setContent {
            MediastoreTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showList.not()) {
                        GalleryLayout(list = Folder.entries) {
                            selectedFolder = it
                            retrieveAllImages()
                            showList = true
                        }
                    } else {
                        ImagesListComposable(
                            list = list,
                            columnName = selectedFolder?.name.orEmpty()
                        ) {
                            imageUrl = it
                            showImageDialog = true
                        }
                        if (showImageDialog)
                            ImageDialog {
                                showImageDialog = false
                            }
                    }
                }
            }
        }
    }

    @Composable
    fun GalleryLayout(
        modifier: Modifier = Modifier,
        list: List<Folder>,
        onFolderItemClick: (Folder) -> Unit
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier.padding(horizontal = 8.dp, vertical = 40.dp)
        ) {
            items(list.size) { index ->
                FolderItem(folder = list[index], onFolderItemClick = onFolderItemClick)
            }
        }
    }


    @Composable
    fun FolderItem(
        modifier: Modifier = Modifier,
        folder: Folder,
        onFolderItemClick: (Folder) -> Unit
    ) {
        Surface(
            modifier = modifier.padding(8.dp),
            onClick = { onFolderItemClick(folder) },
            color = Color.White,
            border = BorderStroke(1.dp, Color.Blue)
        ) {
            Text(
                text = folder.name.uppercase(),
                modifier = modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    private fun ImagesListComposable(
        list: List<Path>,
        columnName: String,
        onClick: (Path) -> Unit
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 40.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    fontWeight = FontWeight.Bold,
                    text = columnName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, bottom = 32.dp, top = 8.dp),
                    textAlign = TextAlign.Start,
                    style = TextStyle(fontSize = 26.sp)
                )
            }
            items(list.size) { index ->
                ImageItemComposable(string = list[index], onClick = onClick)
            }
        }
    }

    override fun onBackPressed() {
        showList = false
    }

    @Composable
    fun ImageDialog(
        modifier: Modifier = Modifier,
        onDismiss: () -> Unit
    ) {
        Dialog(onDismissRequest = onDismiss) {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val painter = rememberAsyncImagePainter(
                    placeholder = painterResource(id = R.drawable.ic_launcher_background),
                    model = imageUrl,
                    contentScale = ContentScale.Crop
                )
                Crossfade(targetState = painter.state, label = "crossAnim") {
                    when (it) {
                        is AsyncImagePainter.State.Success -> {
                            Image(
                                modifier = modifier.size(300.dp),
                                contentScale = ContentScale.Crop,
                                painter = painter,
                                contentDescription = null
                            )
                        }

                        is AsyncImagePainter.State.Loading -> {
                            Image(
                                modifier = modifier.size(300.dp),
                                contentScale = ContentScale.Crop,
                                painter = painterResource(id = R.drawable.ic_launcher_background),
                                contentDescription = null
                            )
                        }

                        else -> {}
                    }
                }

            }
        }
    }

    @Composable
    fun ImageItemComposable(
        modifier: Modifier = Modifier,
        string: String,
        onClick: (String) -> Unit
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .padding(vertical = 8.dp)
                .clickable {
                    onClick(string)
                },
        ) {
            Image(
                modifier = modifier.size(160.dp),
                contentScale = ContentScale.Crop,
                painter = rememberAsyncImagePainter(
                    model = string,
                    contentScale = ContentScale.Crop
                ),
                contentDescription = null
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun retrieveAllImages() {
//        /storage/emulated/0/
        list.clear()
        val folderPath =
            Environment.getExternalStorageDirectory()
                .toString() + "/" + selectedFolder?.path
        Toast.makeText(this, folderPath, Toast.LENGTH_SHORT).show()
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(MediaStore.Images.Media.DATA)

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        val selection = "${MediaStore.Images.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("%$folderPath%")

        val cursor: Cursor? =
            contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)


        cursor?.use {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (cursor.moveToNext()) {
                val imagePath = cursor.getString(columnIndex)
                Log.d("MainActivity", "Image Path: $imagePath")
                list.add(imagePath)
            }
        }
    }

    private fun checkForPermission() {
        val isAndroid13 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val permissions = if (isAndroid13) {
            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                permissions[0]
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
                    1
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    1
                )
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
    MediastoreTheme {
        Greeting("Android")
    }
}

enum class Folder(
    val path: Path,
    val label: String
) {
    TELEGRAM(
        "Pictures/Telegram", label = "Telegram"
    ),
    TWITTER(
        "Pictures/Twitter", label = "Twitter"
    ),
    SNAPSEED(
        "Snapseed", label = "Snapseed"
    );
}

