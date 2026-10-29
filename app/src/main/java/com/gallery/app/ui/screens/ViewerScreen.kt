package com.gallery.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.gallery.app.data.model.MediaItem
import com.gallery.app.ui.viewmodel.GalleryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun ViewerScreen(
    viewModel: GalleryViewModel,
    initialIndex: Int,
    source: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val context = LocalContext.current

    val mediaList = remember(uiState.allMedia, source) {
        when {
            source == "home" -> uiState.allMedia
            source == "favorites" -> viewModel.getFavoriteMedia()
            source.startsWith("album_") -> {
                val bucketId = source.removePrefix("album_").toLongOrNull() ?: 0L
                viewModel.getAlbumMedia(bucketId)
            }
            else -> uiState.allMedia
        }
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (mediaList.size - 1).coerceAtLeast(0))
    ) { mediaList.size }

    var showControls by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }

    val currentItem = mediaList.getOrNull(pagerState.currentPage)
    val isFavorite = favoriteIds.contains(currentItem?.id?.toString())

    // Delete with system dialog on Android 11+
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.loadMedia()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        if (mediaList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No media to display", color = Color.White)
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = mediaList[page]
                if (item.isVideo) {
                    VideoPlayer(
                        item = item,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ZoomableImage(
                        item = item,
                        onClick = { showControls = !showControls },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Overlay controls
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }

                        currentItem?.let { item ->
                            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.dateTaken > 0) {
                                    val date = SimpleDateFormat(
                                        "dd MMM yyyy, HH:mm",
                                        Locale.getDefault()
                                    ).format(Date(item.dateTaken))
                                    Text(
                                        text = date,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { showInfoSheet = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Default.Info, "Info", tint = Color.White)
                        }
                    }

                    // Bottom action bar
                    currentItem?.let { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Share
                            ViewerActionButton(
                                icon = Icons.Default.Share,
                                label = "Share",
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = item.mimeType
                                        putExtra(Intent.EXTRA_STREAM, Uri.parse(item.uri))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "Share via")
                                    )
                                }
                            )

                            // Favorite
                            ViewerActionButton(
                                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                label = "Favorite",
                                tint = if (isFavorite) Color(0xFFFF4D6D) else Color.White,
                                onClick = {
                                    viewModel.toggleFavorite(item.id.toString(), isFavorite)
                                }
                            )

                            // Delete
                            ViewerActionButton(
                                icon = Icons.Default.Delete,
                                label = "Delete",
                                tint = Color(0xFFFF6B6B),
                                onClick = { showDeleteDialog = true }
                            )
                        }
                    }

                    // Page indicator
                    if (mediaList.size > 1) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 76.dp),
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${mediaList.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog && currentItem != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Media") },
                text = { Text("Are you sure you want to delete \"${currentItem.name}\"? This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteMedia(currentItem) { uri ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    val pendingIntent = android.provider.MediaStore.createDeleteRequest(
                                        context.contentResolver,
                                        listOf(uri)
                                    )
                                    deleteLauncher.launch(
                                        androidx.activity.result.IntentSenderRequest
                                            .Builder(pendingIntent.intentSender).build()
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Info bottom sheet
        if (showInfoSheet && currentItem != null) {
            MediaInfoSheet(
                item = currentItem,
                onDismiss = { showInfoSheet = false }
            )
        }
    }
}

@Composable
fun ViewerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
fun ZoomableImage(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = {
                        coroutineScope.launch {
                            scale = if (scale > 1f) 1f else 2.5f
                            if (scale == 1f) offset = Offset.Zero
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.8f, 5f)
                    offset = if (scale > 1f) offset + pan else Offset.Zero
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}

@Composable
fun VideoPlayer(item: MediaItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().also { player ->
            player.setMediaItem(ExoMediaItem.fromUri(Uri.parse(item.uri)))
            player.prepare()
            player.playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaInfoSheet(item: MediaItem, onDismiss: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
    val date = if (item.dateTaken > 0) dateFormat.format(Date(item.dateTaken)) else "Unknown"
    val sizeKb = item.size / 1024
    val sizeMb = sizeKb / 1024.0

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Media Info", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            InfoRow("Name", item.name)
            InfoRow("Date", date)
            InfoRow("Resolution", if (item.width > 0) "${item.width} × ${item.height}" else "Unknown")
            InfoRow("Size", if (sizeMb >= 1.0) "%.1f MB".format(sizeMb) else "$sizeKb KB")
            InfoRow("Type", item.mimeType)
            InfoRow("Album", item.bucketName)
            if (item.isVideo && item.duration > 0) {
                val min = (item.duration / 1000) / 60
                val sec = (item.duration / 1000) % 60
                InfoRow("Duration", "%d:%02d".format(min, sec))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
