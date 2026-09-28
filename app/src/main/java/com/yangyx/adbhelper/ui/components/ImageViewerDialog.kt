package com.yangyx.adbhelper.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.MotionPhotosPaused
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.abs
import com.yangyx.adbhelper.adb.RemoteFileItem
import com.yangyx.adbhelper.ui.AdbViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Checks if a file name represents an image format readable by Android
 */
fun isImageFile(fileName: String, isDirectory: Boolean = false): Boolean {
    if (isDirectory) return false
    val lower = fileName.lowercase()
    val ext = lower.substringAfterLast('.', "")
    val imageExtensions = setOf(
        "png", "jpg", "jpeg", "webp", "bmp", "gif", "ico", "heic", "heif"
    )
    return ext in imageExtensions
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerDialog(
    initialFile: RemoteFileItem,
    imageList: List<RemoteFileItem> = emptyList(),
    viewModel: AdbViewModel,
    onDismiss: () -> Unit,
    onDownload: (RemoteFileItem) -> Unit
) {
    // Current index within image list
    val allImages = remember(imageList, initialFile) {
        if (imageList.isNotEmpty()) imageList else listOf(initialFile)
    }

    var currentIndex by remember(initialFile, allImages) {
        val idx = allImages.indexOfFirst { it.path == initialFile.path }
        mutableIntStateOf(if (idx >= 0) idx else 0)
    }

    val currentFile = allImages.getOrNull(currentIndex) ?: initialFile
    val context = LocalContext.current

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var exifData by remember { mutableStateOf<ExifMetadata?>(null) }
    var hdrInfo by remember { mutableStateOf<HdrInfo?>(null) }
    var livePhotoInfo by remember { mutableStateOf<LivePhotoInfo?>(null) }
    var isPlayingLivePhoto by remember { mutableStateOf(false) }
    var isMutedLivePhoto by remember { mutableStateOf(true) }
    var isDownloadingPairedVideo by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var transferredBytes by remember { mutableLongStateOf(0L) }
    var showExifSheet by remember { mutableStateOf(false) }
    var photoExifRotation by remember { mutableIntStateOf(-1) }

    // Transformation states for gesture zoom, pan, and horizontal swipe
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var swipeOffsetX by remember { mutableFloatStateOf(0f) }

    fun resetTransform() {
        scale = 1f
        offset = Offset.Zero
        rotationAngle = 0f
        swipeOffsetX = 0f
    }

    // Load remote image data and parse EXIF, HDR, Live Photo
    fun loadRemoteImage(fileToLoad: RemoteFileItem) {
        isLoading = true
        errorMessage = null
        transferredBytes = 0L
        bitmap = null
        exifData = null
        hdrInfo = null
        isPlayingLivePhoto = false
        livePhotoInfo?.videoFile?.delete()
        livePhotoInfo = null
        photoExifRotation = -1
        resetTransform()

        viewModel.readRemoteBinaryFile(
            path = fileToLoad.path,
            onProgress = { transferred ->
                transferredBytes = transferred
            },
            onResult = { result ->
                isLoading = false
                result.onSuccess { bytes ->
                    try {
                        val opts = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                        if (decoded != null) {
                            var detectedExifDegrees = -1
                            val orientedBmp = try {
                                val exif = androidx.exifinterface.media.ExifInterface(java.io.ByteArrayInputStream(bytes))
                                val orientation = exif.getAttributeInt(
                                    androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                                    androidx.exifinterface.media.ExifInterface.ORIENTATION_UNDEFINED
                                )
                                val degrees = when (orientation) {
                                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL -> 0f
                                    else -> 0f
                                }
                                detectedExifDegrees = when (orientation) {
                                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL -> 0
                                    else -> -1
                                }
                                if (degrees != 0f) {
                                    val mat = android.graphics.Matrix().apply { postRotate(degrees) }
                                    val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, mat, true)
                                    if (rotated != decoded) {
                                        decoded.recycle()
                                    }
                                    rotated
                                } else {
                                    decoded
                                }
                            } catch (_: Exception) {
                                decoded
                            }
                            photoExifRotation = detectedExifDegrees
                            bitmap = orientedBmp
                            // Asynchronously parse EXIF in background
                            exifData = ExifParser.parse(bytes)
                            // Detect Ultra HDR and Gainmap
                            hdrInfo = LivePhotoAndHdrHelper.detectHdr(bytes, orientedBmp)
                            // Extract Live Photo motion video if available
                            livePhotoInfo = LivePhotoAndHdrHelper.extractLivePhoto(context, bytes, fileToLoad, allImages)
                        } else {
                            errorMessage = "无法解码该图片数据，可能格式损坏或不受系统支持"
                        }
                    } catch (e: OutOfMemoryError) {
                        errorMessage = "图片分辨率过大，解码时内存溢出"
                    } catch (e: Throwable) {
                        errorMessage = "图片解码失败: ${e.message}"
                    }
                }.onFailure { err ->
                    errorMessage = err.message ?: "读取远程图片失败"
                }
            }
        )
    }

    fun loadPairedVideo(pairedFile: RemoteFileItem) {
        if (isDownloadingPairedVideo) return
        isDownloadingPairedVideo = true
        viewModel.readRemoteBinaryFile(
            path = pairedFile.path,
            onResult = { result ->
                isDownloadingPairedVideo = false
                result.onSuccess { videoBytes ->
                    try {
                        val tempFile = File(context.cacheDir, "live_apple_${System.currentTimeMillis()}.mov")
                        tempFile.writeBytes(videoBytes)
                        livePhotoInfo = livePhotoInfo?.copy(videoFile = tempFile)
                        isPlayingLivePhoto = true
                    } catch (_: Exception) {}
                }
            }
        )
    }

    LaunchedEffect(currentFile.path) {
        loadRemoteImage(currentFile)
    }

    DisposableEffect(Unit) {
        onDispose {
            bitmap = null
            livePhotoInfo?.videoFile?.delete()
        }
    }

    // Navigate to previous/next image
    fun goToPrevious() {
        if (currentIndex > 0) {
            isPlayingLivePhoto = false
            currentIndex--
        }
    }

    fun goToNext() {
        if (currentIndex < allImages.size - 1) {
            isPlayingLivePhoto = false
            currentIndex++
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        val view = LocalView.current
        val context = LocalContext.current

        // Enable hardware HDR color mode on the window when an HDR / Ultra HDR image is detected
        DisposableEffect(hdrInfo?.isHdr, view) {
            if (hdrInfo?.isHdr == true) {
                LivePhotoAndHdrHelper.setWindowHdrMode(view, true)
            }
            onDispose {
                LivePhotoAndHdrHelper.setWindowHdrMode(view, false)
            }
        }

        val bottomInsetDp = remember(view) {
            try {
                val rootInsets = ViewCompat.getRootWindowInsets(view)
                val navInsets = rootInsets?.getInsets(WindowInsetsCompat.Type.navigationBars())
                val density = context.resources.displayMetrics.density
                val h = (navInsets?.bottom ?: 0) / density
                if (h > 0) h.dp else 0.dp
            } catch (e: Exception) {
                0.dp
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            color = Color(0xFF0C0D10)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Middle Image Display Area with pinch-zoom, pan when zoomed, swipe to change image, and Live Photo layer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                val progressRatio = if (currentFile.size > 0) (transferredBytes.toFloat() / currentFile.size.toFloat()).coerceIn(0f, 1f) else null
                                if (progressRatio != null && currentFile.size > 100 * 1024) {
                                    LinearProgressIndicator(
                                        progress = { progressRatio },
                                        modifier = Modifier
                                            .width(180.dp)
                                            .height(4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${formatBytes(transferredBytes)} / ${formatBytes(currentFile.size)}",
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 12.sp
                                    )
                                } else {
                                    Text(
                                        text = "正在以无线调试通道读取图片...",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                        errorMessage != null -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = errorMessage ?: "加载失败",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedButton(onClick = onDismiss) {
                                        Text("返回")
                                    }
                                    FilledTonalButton(onClick = { loadRemoteImage(currentFile) }) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("重试加载")
                                    }
                                }
                            }
                        }
                        bitmap != null -> {
                            val bmp = bitmap!!

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(livePhotoInfo) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                if (scale > 1.2f) {
                                                    resetTransform()
                                                } else {
                                                    scale = 2.5f
                                                    offset = Offset.Zero
                                                    swipeOffsetX = 0f
                                                }
                                            },
                                            onLongPress = {
                                                if (livePhotoInfo?.isLivePhoto == true) {
                                                    if (livePhotoInfo?.videoFile != null) {
                                                        isPlayingLivePhoto = !isPlayingLivePhoto
                                                    } else if (livePhotoInfo?.pairedRemoteFile != null) {
                                                        loadPairedVideo(livePhotoInfo!!.pairedRemoteFile!!)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                    .pointerInput(currentFile.path, allImages.size, currentIndex) {
                                        awaitEachGesture {
                                            awaitFirstDown(requireUnconsumed = false)
                                            var totalDragX = 0f
                                            var totalDragY = 0f
                                            var isMultiTouch = false

                                            do {
                                                val event = awaitPointerEvent()
                                                val downPointers = event.changes.filter { it.pressed }

                                                if (downPointers.size >= 2) {
                                                    // Multi-touch pinch zoom & pan
                                                    isMultiTouch = true
                                                    swipeOffsetX = 0f
                                                    val zoom = event.calculateZoom()
                                                    val pan = event.calculatePan()

                                                    val newScale = (scale * zoom).coerceIn(0.5f, 8.0f)
                                                    scale = newScale

                                                    val maxOffsetX = (size.width * (newScale - 1f)).coerceAtLeast(0f) / 2f
                                                    val maxOffsetY = (size.height * (newScale - 1f)).coerceAtLeast(0f) / 2f
                                                    offset = Offset(
                                                        x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                                        y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                                    )

                                                    event.changes.forEach { it.consume() }
                                                } else if (downPointers.size == 1) {
                                                    val change = downPointers[0]
                                                    val dragAmount = change.positionChange()

                                                    if (scale > 1.05f) {
                                                        // Zoomed in: Pan image in any direction
                                                        val maxOffsetX = (size.width * (scale - 1f)).coerceAtLeast(0f) / 2f
                                                        val maxOffsetY = (size.height * (scale - 1f)).coerceAtLeast(0f) / 2f
                                                        offset = Offset(
                                                            x = (offset.x + dragAmount.x).coerceIn(-maxOffsetX, maxOffsetX),
                                                            y = (offset.y + dragAmount.y).coerceIn(-maxOffsetY, maxOffsetY)
                                                        )
                                                        change.consume()
                                                    } else if (!isMultiTouch) {
                                                        // 1x scale: Track horizontal swipe with live visual feedback
                                                        totalDragX += dragAmount.x
                                                        totalDragY += dragAmount.y
                                                        if (allImages.size > 1) {
                                                            swipeOffsetX = (swipeOffsetX + dragAmount.x).coerceIn(-400f, 400f)
                                                        }
                                                        change.consume()
                                                    }
                                                }
                                            } while (event.changes.any { it.pressed })

                                            // Gesture ended (all fingers lifted)
                                            if (scale <= 1.05f && !isMultiTouch && allImages.size > 1) {
                                                val swipeThreshold = 50.dp.toPx()
                                                if (abs(totalDragX) > swipeThreshold &&
                                                    abs(totalDragX) > abs(totalDragY) * 1.1f) {
                                                    if (totalDragX < 0 && currentIndex < allImages.size - 1) {
                                                        goToNext()
                                                    } else if (totalDragX > 0 && currentIndex > 0) {
                                                        goToPrevious()
                                                    }
                                                }
                                            }

                                            swipeOffsetX = 0f
                                            if (scale < 1.0f) {
                                                scale = 1.0f
                                                offset = Offset.Zero
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                BoxWithConstraints(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = if (scale > 1.05f) offset.x else swipeOffsetX,
                                            translationY = if (scale > 1.05f) offset.y else 0f,
                                            rotationZ = rotationAngle
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val imgW = bmp.width.toFloat().coerceAtLeast(1f)
                                    val imgH = bmp.height.toFloat().coerceAtLeast(1f)
                                    val imgAspect = imgW / imgH
                                    val containerAspect = maxWidth / maxHeight

                                    val (fittedWidth, fittedHeight) = if (containerAspect > imgAspect) {
                                        Pair(maxHeight * imgAspect, maxHeight)
                                    } else {
                                        Pair(maxWidth, maxWidth / imgAspect)
                                    }

                                    Box(
                                        modifier = Modifier.size(fittedWidth, fittedHeight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Default high-resolution static photo
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = currentFile.name,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Live Photo dynamic video layer (activated on play)
                                        if (livePhotoInfo?.videoFile != null) {
                                            LivePhotoVideoView(
                                                videoFile = livePhotoInfo!!.videoFile!!,
                                                isPlaying = isPlayingLivePhoto,
                                                isMuted = isMutedLivePhoto,
                                                targetPhotoWidth = bmp.width,
                                                targetPhotoHeight = bmp.height,
                                                photoExifRotation = photoExifRotation,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Top Action & Title Bar (Fixed at top)
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = Color.White
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentFile.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (allImages.size > 1) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "${currentIndex + 1}/${allImages.size}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                // HDR Badge
                                if (hdrInfo?.isHdr == true) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFE5A000).copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.8f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.HdrOn,
                                                contentDescription = "HDR 高动态范围",
                                                tint = Color(0xFFFFD54F),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "HDR",
                                                color = Color(0xFFFFD54F),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }

                                // Live Photo Badge with quick toggle
                                if (livePhotoInfo?.isLivePhoto == true) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isPlayingLivePhoto) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, if (isPlayingLivePhoto) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f)),
                                        modifier = Modifier.clickable {
                                            if (livePhotoInfo?.videoFile != null) {
                                                isPlayingLivePhoto = !isPlayingLivePhoto
                                            } else if (livePhotoInfo?.pairedRemoteFile != null) {
                                                loadPairedVideo(livePhotoInfo!!.pairedRemoteFile!!)
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isPlayingLivePhoto) Icons.Default.MotionPhotosOn else Icons.Default.MotionPhotosPaused,
                                                contentDescription = "实况动态照片",
                                                tint = if (isPlayingLivePhoto) MaterialTheme.colorScheme.primary else Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = if (isPlayingLivePhoto) "LIVE 播放中" else "LIVE",
                                                color = if (isPlayingLivePhoto) MaterialTheme.colorScheme.primary else Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                            val dims = if (bitmap != null) "${bitmap?.width} × ${bitmap?.height}  •  " else ""
                            Text(
                                text = "$dims${formatBytes(currentFile.size)}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        // EXIF & Details button
                        IconButton(onClick = { showExifSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "图片详情",
                                tint = if (exifData != null || hdrInfo?.isHdr == true || livePhotoInfo?.isLivePhoto == true) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        // Download Button
                        IconButton(onClick = { onDownload(currentFile) }) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "下载保存",
                                tint = Color.White
                            )
                        }
                    }
                }

                // 3. Floating Live Photo Play/Pause Controller (Default shows static; click to play motion effect)
                if (livePhotoInfo?.isLivePhoto == true) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xF0181B22),
                        border = BorderStroke(1.dp, if (isPlayingLivePhoto) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.35f)),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 114.dp + bottomInsetDp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    if (livePhotoInfo?.videoFile != null) {
                                        isPlayingLivePhoto = !isPlayingLivePhoto
                                    } else if (livePhotoInfo?.pairedRemoteFile != null) {
                                        loadPairedVideo(livePhotoInfo!!.pairedRemoteFile!!)
                                    }
                                },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (isPlayingLivePhoto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = if (isPlayingLivePhoto) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.size(34.dp)
                            ) {
                                if (isDownloadingPairedVideo) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = if (isPlayingLivePhoto) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlayingLivePhoto) "暂停动态效果" else "播放动态效果",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isDownloadingPairedVideo) "正在提取实况视频..."
                                       else if (isPlayingLivePhoto) "正在播放实况动效"
                                       else "播放动态效果",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (isPlayingLivePhoto) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { isMutedLivePhoto = !isMutedLivePhoto },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMutedLivePhoto) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                        contentDescription = if (isMutedLivePhoto) "取消静音" else "静音",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Bottom Controls Toolbar (Floating rounded dock, safely raised above system navigation area)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 50.dp + bottomInsetDp)
                ) {
                    Surface(
                        color = Color(0xF0181B22),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        tonalElevation = 8.dp,
                        shadowElevation = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous Image (if multiple images)
                            if (allImages.size > 1) {
                                IconButton(
                                    onClick = { goToPrevious() },
                                    enabled = currentIndex > 0,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = "上一张",
                                        tint = if (currentIndex > 0) Color.White else Color.White.copy(alpha = 0.25f)
                                    )
                                }
                            }

                            // Zoom Out
                            IconButton(
                                onClick = {
                                    scale = (scale / 1.35f).coerceAtLeast(0.5f)
                                    if (scale <= 1.0f) {
                                        offset = Offset.Zero
                                        swipeOffsetX = 0f
                                    }
                                },
                                enabled = bitmap != null,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "缩小", tint = Color.White)
                            }

                            // Fit Screen / Zoom ratio reset button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.16f),
                                modifier = Modifier.clickable(enabled = bitmap != null) { resetTransform() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.FitScreen,
                                        contentDescription = "适应屏幕",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${(scale * 100).toInt()}%",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Zoom In
                            IconButton(
                                onClick = {
                                    scale = (scale * 1.35f).coerceAtMost(8.0f)
                                },
                                enabled = bitmap != null,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "放大", tint = Color.White)
                            }

                            // Rotate 90°
                            IconButton(
                                onClick = { rotationAngle = (rotationAngle + 90f) % 360f },
                                enabled = bitmap != null,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.RotateRight, contentDescription = "旋转 90°", tint = Color.White)
                            }

                            // Next Image (if multiple images)
                            if (allImages.size > 1) {
                                IconButton(
                                    onClick = { goToNext() },
                                    enabled = currentIndex < allImages.size - 1,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "下一张",
                                        tint = if (currentIndex < allImages.size - 1) Color.White else Color.White.copy(alpha = 0.25f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Image Properties & EXIF Modal Bottom Sheet
        if (showExifSheet) {
            ImageDetailsBottomSheet(
                file = currentFile,
                bitmap = bitmap,
                exif = exifData,
                hdr = hdrInfo,
                livePhoto = livePhotoInfo,
                onDismiss = { showExifSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailsBottomSheet(
    file: RemoteFileItem,
    bitmap: Bitmap?,
    exif: ExifMetadata?,
    hdr: HdrInfo? = null,
    livePhoto: LivePhotoInfo? = null,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "图片详细信息与参数",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Basic File & Dimension Section
            InfoGroupCard(title = "基础属性", icon = Icons.Default.Image) {
                InfoItemRow(label = "文件名", value = file.name)
                InfoItemRow(label = "远程路径", value = file.path)
                InfoItemRow(label = "文件大小", value = "${formatBytes(file.size)} (${file.size} 字节)")
                if (bitmap != null) {
                    InfoItemRow(label = "物理分辨率", value = "${bitmap.width} × ${bitmap.height} 像素")
                    val mp = (bitmap.width.toLong() * bitmap.height.toLong()) / 1_000_000.0
                    if (mp > 0.1) {
                        InfoItemRow(label = "有效像素", value = "${String.format(java.util.Locale.US, "%.1f", mp)} MP (百万像素)")
                    }
                }
                if (exif?.orientation != null) {
                    InfoItemRow(label = "画面方向", value = exif.orientation)
                }
            }

            // HDR & Live Photo properties (if detected)
            if (hdr?.isHdr == true || livePhoto?.isLivePhoto == true) {
                Spacer(modifier = Modifier.height(12.dp))
                InfoGroupCard(title = "HDR 与动态照片特征", icon = Icons.Default.MotionPhotosOn) {
                    if (hdr?.isHdr == true) {
                        InfoItemRow(label = "HDR 高动态范围", value = if (hdr.description.isNotEmpty()) hdr.description else "已支持 HDR 广色域")
                        InfoItemRow(label = "HDR 增益图 Gainmap", value = if (hdr.hasGainmap) "包含增益图 (支持高亮高对比度渲染)" else "包含 HDR 元数据")
                    }
                    if (livePhoto?.isLivePhoto == true) {
                        InfoItemRow(label = "动态照片类型", value = if (livePhoto.formatName.isNotEmpty()) livePhoto.formatName else "实况动态照片")
                        if (livePhoto.videoSize > 0) {
                            InfoItemRow(label = "动效视频大小", value = formatBytes(livePhoto.videoSize))
                        } else if (livePhoto.videoFile != null) {
                            InfoItemRow(label = "动效视频大小", value = formatBytes(livePhoto.videoFile.length()))
                        } else if (livePhoto.pairedRemoteFile != null) {
                            InfoItemRow(label = "关联动效文件", value = livePhoto.pairedRemoteFile.name)
                        }
                        InfoItemRow(label = "播放方式", value = "长按图片或点击底部播放按钮即可循环动态播放")
                    }
                }
            }

            // 2. Camera Information (Only if available)
            if (exif != null && exif.hasCameraInfo()) {
                Spacer(modifier = Modifier.height(12.dp))
                InfoGroupCard(title = "照相机设备信息", icon = Icons.Default.CameraAlt) {
                    if (exif.make != null) InfoItemRow(label = "制造厂商", value = exif.make)
                    if (exif.model != null) InfoItemRow(label = "相机型号", value = exif.model)
                    if (exif.lensModel != null) InfoItemRow(label = "镜头型号", value = exif.lensModel)
                    if (exif.software != null) InfoItemRow(label = "处理软件", value = exif.software)
                }
            }

            // 3. Advanced Photography Information (Only if available)
            if (exif != null && exif.hasPhotoParams()) {
                Spacer(modifier = Modifier.height(12.dp))
                InfoGroupCard(title = "高级照片拍摄参数", icon = Icons.Default.Tune) {
                    if (exif.dateTimeOriginal != null) InfoItemRow(label = "拍摄时间", value = exif.dateTimeOriginal)
                    if (exif.fNumber != null) InfoItemRow(label = "光圈值", value = exif.fNumber)
                    if (exif.exposureTime != null) InfoItemRow(label = "快门速度", value = exif.exposureTime)
                    if (exif.iso != null) InfoItemRow(label = "感光度", value = exif.iso)
                    if (exif.focalLength != null) InfoItemRow(label = "焦距", value = exif.focalLength)
                    if (exif.focalLength35mm != null) InfoItemRow(label = "等效焦距", value = exif.focalLength35mm)
                    if (exif.exposureProgram != null) InfoItemRow(label = "曝光程序", value = exif.exposureProgram)
                    if (exif.meteringMode != null) InfoItemRow(label = "测光模式", value = exif.meteringMode)
                    if (exif.whiteBalance != null) InfoItemRow(label = "白平衡", value = exif.whiteBalance)
                    if (exif.flash != null) InfoItemRow(label = "闪光灯", value = exif.flash)
                    if (exif.colorSpace != null) InfoItemRow(label = "色彩空间", value = exif.colorSpace)
                }
            }

            // 4. GPS Information (Only if available)
            if (exif != null && exif.hasGpsInfo()) {
                Spacer(modifier = Modifier.height(12.dp))
                InfoGroupCard(title = "GPS 定位信息", icon = Icons.Default.LocationOn) {
                    if (exif.gpsCoordinates != null) InfoItemRow(label = "经纬度坐标", value = exif.gpsCoordinates)
                    if (exif.gpsAltitude != null) InfoItemRow(label = "海拔高度", value = exif.gpsAltitude)
                    if (exif.gpsSpeed != null) InfoItemRow(label = "移动速度", value = exif.gpsSpeed)
                    if (exif.gpsDateStamp != null) InfoItemRow(label = "GPS时间戳", value = exif.gpsDateStamp)
                }
            }
        }
    }
}

@Composable
private fun InfoGroupCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun InfoItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(100.dp)
                .alignByBaseline()
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .alignByBaseline()
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    return when {
        bytes >= 1024L * 1024L * 1024L -> String.format(java.util.Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
