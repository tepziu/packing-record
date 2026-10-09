package com.example.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.domain.model.PackingSession
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    videoFilePath: String,
    session: PackingSession? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showWatermarkOverlay by remember { mutableStateOf(true) }
    var currentPlaybackPositionMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }

    val exoPlayer = remember(videoFilePath) {
        ExoPlayer.Builder(context).build().apply {
            val file = File(videoFilePath)
            if (file.exists()) {
                val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Update real-time playback position for synchronized timestamp HUD
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPlaybackPositionMs = exoPlayer.currentPosition
            delay(200)
        }
    }

    // Calculate real-time timestamp during playback
    val baseStartTime = session?.startTimeDevice ?: System.currentTimeMillis()
    val currentFrameTimeMillis = baseStartTime + currentPlaybackPositionMs
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val liveVideoTimestamp = sdf.format(Date(currentFrameTimeMillis))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            }
        )

        // Minimal Date & Time Watermark Badge in Corner (không che nội dung video)
        if (showWatermarkOverlay) {
            Surface(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xB3000000) // Semi-transparent dark background
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFFEF4444), CircleShape)
                    )
                    Text(
                        text = liveVideoTimestamp,
                        color = Color(0xFFFDE047), // High visibility Yellow
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Toggle Watermark Button on top right of video
        IconButton(
            onClick = { showWatermarkOverlay = !showWatermarkOverlay },
            modifier = Modifier
                .padding(6.dp)
                .size(28.dp)
                .align(Alignment.TopEnd)
                .background(Color(0x66000000), CircleShape)
        ) {
            Icon(
                imageVector = if (showWatermarkOverlay) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = "Toggle Watermark",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
