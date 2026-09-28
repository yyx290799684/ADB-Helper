package com.yangyx.adbhelper.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.view.TextureView
import android.view.View
import android.view.Window
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogWindowProvider
import com.yangyx.adbhelper.adb.RemoteFileItem
import java.io.File
import java.nio.charset.StandardCharsets

data class LivePhotoInfo(
    val isLivePhoto: Boolean,
    val videoFile: File? = null,
    val videoSize: Long = 0L,
    val formatName: String = "",
    val pairedRemoteFile: RemoteFileItem? = null
)

data class HdrInfo(
    val isHdr: Boolean,
    val hasGainmap: Boolean = false,
    val description: String = ""
)

data class VideoMetadata(
    val rawWidth: Int = 0,
    val rawHeight: Int = 0,
    val rotation: Int = 0
) {
    val displayWidth: Int = if (rotation == 90 || rotation == 270) rawHeight else rawWidth
    val displayHeight: Int = if (rotation == 90 || rotation == 270) rawWidth else rawHeight
    val hasValidSize: Boolean = displayWidth > 0 && displayHeight > 0
}

object LivePhotoAndHdrHelper {

    /**
     * Detects if the image is an HDR image (e.g. Ultra HDR with Gainmap, Wide Color Gamut).
     */
    fun detectHdr(bytes: ByteArray, bitmap: Bitmap?): HdrInfo {
        var hasGainmap = false
        if (Build.VERSION.SDK_INT >= 34 && bitmap != null) {
            try {
                hasGainmap = bitmap.hasGainmap()
            } catch (_: Throwable) {}
        }

        // Search for Ultra HDR Gainmap metadata signatures in raw bytes
        val hasHdrMarker = containsGainmapMetadata(bytes)

        val isHdr = hasGainmap || hasHdrMarker
        val desc = when {
            hasGainmap -> "Ultra HDR (系统硬件亮度增益图 Gainmap)"
            hasHdrMarker -> "Ultra HDR (Adobe / ISO HDR Gainmap 高动态范围)"
            else -> "标准动态范围 (SDR)"
        }

        return HdrInfo(
            isHdr = isHdr,
            hasGainmap = hasGainmap || hasHdrMarker,
            description = desc
        )
    }

    private fun containsGainmapMetadata(bytes: ByteArray): Boolean {
        // Quick scan for HDR Gainmap markers in the header/XMP (usually in first 256KB or last 64KB)
        val scanLimit = minOf(bytes.size, 256 * 1024)
        if (scanLimit < 32) return false
        val prefixString = String(bytes, 0, scanLimit, StandardCharsets.ISO_8859_1)
        if (prefixString.contains("hdr-gain-map") ||
            prefixString.contains("hdrgm:Version") ||
            prefixString.contains("Item:Semantic=\"GainMap\"") ||
            prefixString.contains("Item:Semantic=\"Gainmap\"") ||
            prefixString.contains("xmlns:hdrgm") ||
            prefixString.contains("xmlns:Container") && prefixString.contains("GainMap")
        ) {
            return true
        }

        // Also check tail if file is large
        if (bytes.size > scanLimit + 64 * 1024) {
            val tailStart = bytes.size - 64 * 1024
            val tailString = String(bytes, tailStart, 64 * 1024, StandardCharsets.ISO_8859_1)
            if (tailString.contains("hdr-gain-map") ||
                tailString.contains("hdrgm:Version") ||
                tailString.contains("Item:Semantic=\"GainMap\"")
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Extracts an embedded motion photo MP4 video from standard Android JPEG/HEIC,
     * or detects paired Apple Live Photo video file.
     */
    fun extractLivePhoto(
        context: Context,
        bytes: ByteArray,
        currentFile: RemoteFileItem,
        siblingFiles: List<RemoteFileItem> = emptyList()
    ): LivePhotoInfo? {
        if (bytes.size < 4096) return null

        // 1. Try parsing Google/Samsung Motion Photo via XMP metadata offset
        val xmpOffset = findMicroVideoOffsetFromXmp(bytes)
        if (xmpOffset != null && xmpOffset > 0 && xmpOffset < bytes.size) {
            val videoStart = bytes.size - xmpOffset
            if (videoStart >= 0 && isValidMp4Header(bytes, videoStart)) {
                val tempFile = writeTempVideo(context, currentFile.name, bytes, videoStart)
                if (tempFile != null) {
                    return LivePhotoInfo(
                        isLivePhoto = true,
                        videoFile = tempFile,
                        videoSize = (bytes.size - videoStart).toLong(),
                        formatName = "Google/Android 动态照片"
                    )
                }
            }
        }

        // 2. Scan backwards for embedded MP4 'ftyp' box signature
        // MP4 format header: [4 bytes size][f][t][y][p][4 bytes brand]
        val maxScanBack = minOf(bytes.size - 16, 50 * 1024 * 1024) // up to 50MB backwards
        val minIndex = bytes.size - maxScanBack
        var i = bytes.size - 16
        while (i >= minIndex) {
            // Check 'f' 't' 'y' 'p' (0x66, 0x74, 0x79, 0x70)
            if (bytes[i] == 0x66.toByte() &&
                bytes[i + 1] == 0x74.toByte() &&
                bytes[i + 2] == 0x79.toByte() &&
                bytes[i + 3] == 0x70.toByte()
            ) {
                val boxStart = i - 4
                if (boxStart >= 0) {
                    val boxSize = readBigEndianInt(bytes, boxStart)
                    // Valid ftyp box size is usually 16..256 bytes
                    if (boxSize in 16..1024) {
                        val brand = String(bytes, i + 4, 4, StandardCharsets.ISO_8859_1)
                        if (isRecognizedMp4Brand(brand)) {
                            val tempFile = writeTempVideo(context, currentFile.name, bytes, boxStart)
                            if (tempFile != null) {
                                return LivePhotoInfo(
                                    isLivePhoto = true,
                                    videoFile = tempFile,
                                    videoSize = (bytes.size - boxStart).toLong(),
                                    formatName = "动态照片 (内嵌微视频)"
                                )
                            }
                        }
                    }
                }
            }
            i--
        }

        // 3. Check for paired Apple Live Photo MOV/MP4 file in siblings
        val baseName = currentFile.name.substringBeforeLast('.')
        val ext = currentFile.name.substringAfterLast('.', "").lowercase()
        if (ext == "jpg" || ext == "jpeg" || ext == "heic") {
            val pairedVideo = siblingFiles.firstOrNull { sib ->
                val sibBase = sib.name.substringBeforeLast('.')
                val sibExt = sib.name.substringAfterLast('.', "").lowercase()
                sibBase.equals(baseName, ignoreCase = true) && (sibExt == "mov" || sibExt == "mp4")
            }
            if (pairedVideo != null) {
                return LivePhotoInfo(
                    isLivePhoto = true,
                    videoFile = null,
                    videoSize = pairedVideo.size,
                    formatName = "Apple 实况照片 (配对视频)",
                    pairedRemoteFile = pairedVideo
                )
            }
        }

        return null
    }

    private fun findMicroVideoOffsetFromXmp(bytes: ByteArray): Int? {
        val scanLen = minOf(bytes.size, 128 * 1024)
        val text = String(bytes, 0, scanLen, StandardCharsets.ISO_8859_1)
        val regex1 = Regex("""MicroVideoOffset=["'](\d+)["']""")
        val match1 = regex1.find(text)
        if (match1 != null) {
            return match1.groupValues[1].toIntOrNull()
        }
        val regex2 = Regex("""Item:Length=["'](\d+)["'][^>]*Item:Mime=["']video/mp4["']""")
        val match2 = regex2.find(text)
        if (match2 != null) {
            return match2.groupValues[1].toIntOrNull()
        }
        val regex3 = Regex("""Item:Mime=["']video/mp4["'][^>]*Item:Length=["'](\d+)["']""")
        val match3 = regex3.find(text)
        if (match3 != null) {
            return match3.groupValues[1].toIntOrNull()
        }
        return null
    }

    private fun isValidMp4Header(bytes: ByteArray, offset: Int): Boolean {
        if (offset + 8 > bytes.size) return false
        val boxSize = readBigEndianInt(bytes, offset)
        if (boxSize < 8 || boxSize > 1024) return false
        return bytes[offset + 4] == 0x66.toByte() &&
                bytes[offset + 5] == 0x74.toByte() &&
                bytes[offset + 6] == 0x79.toByte() &&
                bytes[offset + 7] == 0x70.toByte()
    }

    private fun isRecognizedMp4Brand(brand: String): Boolean {
        val b = brand.lowercase()
        return b.startsWith("mp4") || b.startsWith("iso") || b.startsWith("qt") ||
                b.startsWith("3gp") || b.startsWith("avc") || b.startsWith("msn") ||
                b.startsWith("m4v") || b.startsWith("dash")
    }

    private fun readBigEndianInt(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
    }

    private fun writeTempVideo(context: Context, originalName: String, bytes: ByteArray, startOffset: Int): File? {
        return try {
            val cleanBase = originalName.substringBeforeLast('.').replace(Regex("""[^a-zA-Z0-9_-]"""), "_")
            val tempFile = File(context.cacheDir, "live_${cleanBase}_${System.currentTimeMillis()}.mp4")
            tempFile.outputStream().use { fos ->
                fos.write(bytes, startOffset, bytes.size - startOffset)
            }
            tempFile
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Enables or disables HDR color mode on the Window if supported (API 26+ / API 34+).
     */
    fun setWindowHdrMode(view: View, enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val window = findWindow(view) ?: return
                if (enable) {
                    window.colorMode = ActivityInfo.COLOR_MODE_HDR
                } else {
                    window.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
                }
            } catch (_: Throwable) {}
        }
    }

    fun findWindow(view: View): Window? {
        var parent = view.parent
        while (parent != null) {
            if (parent is DialogWindowProvider) {
                return parent.window
            }
            parent = parent.parent
        }
        var ctx = view.context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx.window
            ctx = ctx.baseContext
        }
        return null
    }

    /**
     * Extracts raw dimensions and rotation metadata from video file.
     */
    fun extractVideoMetadata(file: File): VideoMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val r = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            VideoMetadata(rawWidth = w, rawHeight = h, rotation = r)
        } catch (_: Exception) {
            VideoMetadata()
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Resolves the correct video rotation to ensure that Live Photo video playback
     * orientation strictly aligns with the still photo orientation.
     * Prevents cases where horizontal photos have vertical video playback or upside down playback
     * due to camera preview orientation tags remaining fixed at 90° in Motion Photos.
     */
    fun resolveVideoRotation(
        rawWidth: Int,
        rawHeight: Int,
        metaRotation: Int,
        photoWidth: Int,
        photoHeight: Int,
        photoExifRotation: Int = -1
    ): Int {
        if (rawWidth <= 0 || rawHeight <= 0 || photoWidth <= 0 || photoHeight <= 0) {
            return metaRotation
        }

        val photoIsLandscape = photoWidth >= photoHeight

        // Check whether metaRotation yields the same landscape/portrait orientation as the photo
        val isMetaRotated90 = (metaRotation == 90 || metaRotation == 270)
        val metaVideoWidth = if (isMetaRotated90) rawHeight else rawWidth
        val metaVideoHeight = if (isMetaRotated90) rawWidth else rawHeight
        val metaVideoIsLandscape = metaVideoWidth >= metaVideoHeight

        // If orientation matches the photo and no orientation mismatch was detected:
        if (photoIsLandscape == metaVideoIsLandscape) {
            return metaRotation
        }

        // Mismatch detected:
        // For instance, photo is horizontal/landscape (photoWidth >= photoHeight), but metaRotation=90
        // turns a 1920x1080 landscape video into a 1080x1920 vertical video!
        // We find candidate rotations among {0, 90, 180, 270} that match the photo's orientation.
        val candidateRotations = listOf(0, 90, 180, 270)
        val validCandidates = candidateRotations.filter { r ->
            val isR90 = (r == 90 || r == 270)
            val w = if (isR90) rawHeight else rawWidth
            val h = if (isR90) rawWidth else rawHeight
            (w >= h) == photoIsLandscape
        }

        // For landscape photos where Android camera preview records at metaRotation=90:
        // The raw sensor frame at 0° is 180° upside-down relative to the upright landscape photo.
        // Therefore, 180° rotation is required to be right-side up.
        val preferred = if (photoIsLandscape) {
            if (photoExifRotation == 180) {
                180
            } else if (photoExifRotation == 0 && metaRotation != 90) {
                0
            } else if (metaRotation == 90) {
                180
            } else if (metaRotation == 270) {
                0
            } else {
                180
            }
        } else {
            if (photoExifRotation == 270) {
                270
            } else if (photoExifRotation == 90) {
                90
            } else if (metaRotation == 180) {
                270
            } else {
                90
            }
        }

        return if (validCandidates.contains(preferred)) preferred else (validCandidates.firstOrNull() ?: metaRotation)
    }

    /**
     * Calculates and applies an aspect-fit and rotation transformation matrix to TextureView.
     * Prevents stretching or distortion during Live Photo playback.
     */
    fun applyVideoTransform(
        textureView: TextureView,
        rawWidth: Int,
        rawHeight: Int,
        rotation: Int
    ) {
        val viewWidth = textureView.width.toFloat()
        val viewHeight = textureView.height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f || rawWidth <= 0 || rawHeight <= 0) return

        val cx = viewWidth / 2f
        val cy = viewHeight / 2f

        val effVideoWidth = if (rotation == 90 || rotation == 270) rawHeight else rawWidth
        val effVideoHeight = if (rotation == 90 || rotation == 270) rawWidth else rawHeight

        val viewAspect = viewWidth / viewHeight
        val effAspect = effVideoWidth.toFloat() / effVideoHeight.toFloat()

        val (dispW, dispH) = if (viewAspect > effAspect) {
            Pair(viewHeight * effAspect, viewHeight)
        } else {
            Pair(viewWidth, viewWidth / effAspect)
        }

        val matrix = Matrix()
        if (rotation == 90 || rotation == 270) {
            val sx = dispH / viewWidth
            val sy = dispW / viewHeight
            matrix.setScale(sx, sy, cx, cy)
            matrix.postRotate(rotation.toFloat(), cx, cy)
        } else {
            val sx = dispW / viewWidth
            val sy = dispH / viewHeight
            matrix.setScale(sx, sy, cx, cy)
            if (rotation == 180) {
                matrix.postRotate(180f, cx, cy)
            }
        }
        textureView.setTransform(matrix)
    }
}

/**
 * Lightweight hardware-accelerated video view for Motion / Live Photos.
 * Seamlessly integrates into Jetpack Compose layer with looping support and aspect ratio preservation.
 */
@Composable
fun LivePhotoVideoView(
    videoFile: File,
    isPlaying: Boolean,
    isMuted: Boolean,
    targetPhotoWidth: Int = 0,
    targetPhotoHeight: Int = 0,
    photoExifRotation: Int = -1,
    modifier: Modifier = Modifier
) {
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var surfaceTexture by remember { mutableStateOf<SurfaceTexture?>(null) }
    var currentTextureView by remember { mutableStateOf<TextureView?>(null) }
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentIsMuted by rememberUpdatedState(isMuted)

    // Pre-extract metadata for video rotation and dimensions
    val videoMeta = remember(videoFile.absolutePath) {
        LivePhotoAndHdrHelper.extractVideoMetadata(videoFile)
    }
    var currentRawWidth by remember(videoFile.absolutePath) { mutableIntStateOf(videoMeta.rawWidth) }
    var currentRawHeight by remember(videoFile.absolutePath) { mutableIntStateOf(videoMeta.rawHeight) }

    fun updateTransform(tv: TextureView? = currentTextureView) {
        tv?.let { view ->
            if (currentRawWidth > 0 && currentRawHeight > 0) {
                val effRotation = LivePhotoAndHdrHelper.resolveVideoRotation(
                    rawWidth = currentRawWidth,
                    rawHeight = currentRawHeight,
                    metaRotation = videoMeta.rotation,
                    photoWidth = targetPhotoWidth,
                    photoHeight = targetPhotoHeight,
                    photoExifRotation = photoExifRotation
                )
                LivePhotoAndHdrHelper.applyVideoTransform(
                    textureView = view,
                    rawWidth = currentRawWidth,
                    rawHeight = currentRawHeight,
                    rotation = effRotation
                )
            }
        }
    }

    // Manage MediaPlayer instance
    DisposableEffect(videoFile.absolutePath) {
        val mp = MediaPlayer().apply {
            try {
                setDataSource(videoFile.absolutePath)
                isLooping = true
                val vol = if (currentIsMuted) 0f else 1f
                setVolume(vol, vol)
                setOnVideoSizeChangedListener { _, width, height ->
                    if (width > 0 && height > 0) {
                        currentRawWidth = width
                        currentRawHeight = height
                        updateTransform()
                    }
                }
                setOnPreparedListener { player ->
                    val w = player.videoWidth
                    val h = player.videoHeight
                    if (w > 0 && h > 0) {
                        currentRawWidth = w
                        currentRawHeight = h
                    }
                    updateTransform()
                    if (currentIsPlaying) {
                        player.start()
                    }
                }
                prepareAsync()
            } catch (_: Exception) {}
        }
        mediaPlayer = mp

        onDispose {
            try {
                mp.stop()
            } catch (_: Exception) {}
            mp.release()
            mediaPlayer = null
        }
    }

    // React to isPlaying changes
    LaunchedEffect(isPlaying, mediaPlayer) {
        mediaPlayer?.let { mp ->
            try {
                if (isPlaying) {
                    if (!mp.isPlaying) {
                        updateTransform()
                        mp.start()
                    }
                } else {
                    if (mp.isPlaying) {
                        mp.pause()
                        mp.seekTo(0)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // React to isMuted changes
    LaunchedEffect(isMuted, mediaPlayer) {
        mediaPlayer?.let { mp ->
            try {
                val vol = if (isMuted) 0f else 1f
                mp.setVolume(vol, vol)
            } catch (_: Exception) {}
        }
    }

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = isPlaying,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        currentTextureView = this
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                                surfaceTexture = st
                                val s = android.view.Surface(st)
                                mediaPlayer?.setSurface(s)
                                updateTransform(this@apply)
                            }

                            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                                updateTransform(this@apply)
                            }

                            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                mediaPlayer?.setSurface(null)
                                surfaceTexture = null
                                return true
                            }

                            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                        }
                    }
                },
                update = { view ->
                    currentTextureView = view
                    if (surfaceTexture != null && mediaPlayer != null) {
                        try {
                            mediaPlayer?.setSurface(android.view.Surface(surfaceTexture))
                        } catch (_: Exception) {}
                    }
                    updateTransform(view)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
