package com.example.featherandroidtasks

import android.Manifest
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.provider.MediaStore
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.featherandroidtasks.ui.theme.FeatherAndroidTasksTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            var isPermissionDenied by remember { mutableStateOf(false) }
            val permissionState =
                rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE,) { permissionResult ->
                    if (!permissionResult) {
                        isPermissionDenied = true
                    }
                }

            LaunchedEffect(permissionState) {
                permissionState.launchPermissionRequest()
            }

            FeatherAndroidTasksTheme {
                if (permissionState.status.isGranted) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val mediaList = getMediaList()
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(mediaList) { mediaItem ->
                                MediaItemCard(mediaItem = mediaItem)
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.background(Color.Blue))
                }
            }
        }
    }

    private fun getMediaList(): List<MediaItem> {
        val mediaList = mutableListOf<MediaItem>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATA
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        // Use the content resolver to query the MediaStore for images
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            // Get the column indices
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            // Iterate through the cursor and add each image to the list
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val data = cursor.getString(dataColumn)

                // Create a MediaItem object and add it to the list
                val mediaItem = MediaItem(id, displayName, dateAdded, data, MediaType.IMAGE)
                mediaList.add(mediaItem)
            }
        }

        // Use the content resolver to query the MediaStore for videos
        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            // Get the column indices
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            // Iterate through the cursor and add each video to the list
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val data = cursor.getString(dataColumn)

                // Create a MediaItem object and add it to the list
                val mediaItem = MediaItem(id, displayName, dateAdded, data, MediaType.VIDEO)
                mediaList.add(mediaItem)
            }
        }

        // Return the combined list of images and videos
        return mediaList
    }

    // Composable function to display each media item in a card
    @Composable
    private fun MediaItemCard(mediaItem: MediaItem) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Display the media item's type icon (image or video)
                Icon(
                    imageVector = if (mediaItem.type == MediaType.IMAGE) Icons.Default.Person else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Display the media item's name
                Text(
                    text = mediaItem.displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Display additional details based on the media type
                when (mediaItem.type) {
                    MediaType.IMAGE -> {
                        AsyncImage(
                            model = mediaItem.data,
                            contentDescription = null,
                        )
                    }

                    MediaType.VIDEO -> {
                        Column {
                            Text(
                                text = "Video duration: ${getVideoDuration(mediaItem.data)}",
                            )
                            VideoPlayer(videoUri = mediaItem.data)
                        }
                    }
                }
            }
        }
    }

    // Function to get the duration of a video using its URI
    private fun getVideoDuration(videoUri: String): String {
        // Use the MediaMetadataRetriever to get video details
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoUri)
        val duration =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
        retriever.release()

        // Convert the duration from milliseconds to seconds
        val seconds = duration / 1000

        // Format the duration as HH:mm:ss
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }
}

@Composable
private fun VideoPlayer(videoUri: String) {
    val uri = videoUri.toUri()

    // Use VideoView to play the video
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(uri)
                start()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(8.dp)
    )
}

// Data class to represent a media item
data class MediaItem(
    val id: Long,
    val displayName: String,
    val dateAdded: Long,
    val data: String,
    val type: MediaType
)

// Enum class to represent the type of media (image or video)
enum class MediaType {
    IMAGE,
    VIDEO
}