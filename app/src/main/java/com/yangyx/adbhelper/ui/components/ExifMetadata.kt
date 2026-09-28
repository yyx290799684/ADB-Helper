package com.yangyx.adbhelper.ui.components

import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Locale

data class ExifMetadata(
    // Basic Camera Information
    val make: String? = null,
    val model: String? = null,
    val lensModel: String? = null,
    val software: String? = null,

    // Advanced Photo Information
    val dateTimeOriginal: String? = null,
    val exposureTime: String? = null,
    val fNumber: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val focalLength35mm: String? = null,
    val exposureProgram: String? = null,
    val meteringMode: String? = null,
    val whiteBalance: String? = null,
    val flash: String? = null,
    val colorSpace: String? = null,

    // Resolution & Dimension
    val pixelXDimension: Int? = null,
    val pixelYDimension: Int? = null,
    val orientation: String? = null,

    // GPS Information
    val gpsCoordinates: String? = null,
    val gpsAltitude: String? = null,
    val gpsSpeed: String? = null,
    val gpsDateStamp: String? = null
) {
    fun hasAnyExifData(): Boolean {
        return make != null || model != null || lensModel != null || software != null ||
                dateTimeOriginal != null || exposureTime != null || fNumber != null ||
                iso != null || focalLength != null || focalLength35mm != null ||
                exposureProgram != null || meteringMode != null || whiteBalance != null ||
                flash != null || colorSpace != null || gpsCoordinates != null ||
                gpsAltitude != null || gpsSpeed != null || gpsDateStamp != null
    }

    fun hasCameraInfo(): Boolean {
        return make != null || model != null || lensModel != null || software != null
    }

    fun hasPhotoParams(): Boolean {
        return dateTimeOriginal != null || exposureTime != null || fNumber != null ||
                iso != null || focalLength != null || focalLength35mm != null ||
                exposureProgram != null || meteringMode != null || whiteBalance != null ||
                flash != null || colorSpace != null
    }

    fun hasGpsInfo(): Boolean {
        return gpsCoordinates != null || gpsAltitude != null || gpsSpeed != null || gpsDateStamp != null
    }
}

object ExifParser {
    fun parse(bytes: ByteArray): ExifMetadata? {
        return try {
            val inputStream = ByteArrayInputStream(bytes)
            val exif = ExifInterface(inputStream)

            // Camera info
            val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()?.takeIf { it.isNotEmpty() }
            val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()?.takeIf { it.isNotEmpty() }
            val lensModel = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)?.trim()?.takeIf { it.isNotEmpty() }
            val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)?.trim()?.takeIf { it.isNotEmpty() }

            // Photo params
            val rawDateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            val dateTimeOriginal = formatExifDate(rawDateTime)

            val rawExposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
            val exposureTime = formatExposureTime(rawExposureTime)

            val rawFNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
            val fNumber = if (rawFNumber > 0.0) "f/${String.format(Locale.US, "%.1f", rawFNumber)}" else null

            val isoVal = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                ?: exif.getAttribute("ISO")
            val iso = if (!isoVal.isNullOrEmpty()) "ISO $isoVal" else null

            val focalVal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
            val focalLength = if (focalVal > 0.0) "${String.format(Locale.US, "%.1f", focalVal)} mm" else null

            val focal35Val = exif.getAttributeInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, 0)
            val focalLength35mm = if (focal35Val > 0) "${focal35Val} mm (等效 35mm)" else null

            val exposureProgram = when (exif.getAttributeInt(ExifInterface.TAG_EXPOSURE_PROGRAM, -1)) {
                1 -> "手动 (Manual)"
                2 -> "普通程序 (Normal)"
                3 -> "光圈优先 (Aperture priority)"
                4 -> "快门优先 (Shutter priority)"
                5 -> "创意模式 (偏景深)"
                6 -> "动作模式 (高速快门)"
                7 -> "肖像模式"
                8 -> "风景模式"
                else -> null
            }

            val meteringMode = when (exif.getAttributeInt(ExifInterface.TAG_METERING_MODE, -1)) {
                1 -> "平均测光 (Average)"
                2 -> "中央重点平均测光 (CenterWeightedAverage)"
                3 -> "点测光 (Spot)"
                4 -> "多点测光 (MultiSpot)"
                5 -> "多区域测光 (Pattern)"
                6 -> "局部测光 (Partial)"
                else -> null
            }

            val whiteBalance = when (exif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, -1)) {
                0 -> "自动白平衡 (Auto)"
                1 -> "手动白平衡 (Manual)"
                else -> null
            }

            val flashCode = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1)
            val flash = when {
                flashCode == -1 -> null
                flashCode and 1 != 0 -> "开启"
                else -> "关闭"
            }

            val colorSpace = when (exif.getAttributeInt(ExifInterface.TAG_COLOR_SPACE, -1)) {
                1 -> "sRGB"
                2 -> "Adobe RGB"
                65535 -> "未校准 (Uncalibrated)"
                else -> null
            }

            // Dimensions
            val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                .takeIf { it > 0 } ?: exif.getAttributeInt(ExifInterface.TAG_PIXEL_X_DIMENSION, 0).takeIf { it > 0 }
            val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                .takeIf { it > 0 } ?: exif.getAttributeInt(ExifInterface.TAG_PIXEL_Y_DIMENSION, 0).takeIf { it > 0 }

            val orientationCode = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
            val orientation = when (orientationCode) {
                ExifInterface.ORIENTATION_NORMAL -> "正常 (0°)"
                ExifInterface.ORIENTATION_ROTATE_90 -> "顺时针 90°"
                ExifInterface.ORIENTATION_ROTATE_180 -> "180°"
                ExifInterface.ORIENTATION_ROTATE_270 -> "逆时针 90°"
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> "水平翻转"
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> "垂直翻转"
                else -> null
            }

            // GPS info
            val latLong = exif.latLong
            val gpsCoordinates = if (latLong != null && latLong.size >= 2) {
                val lat = latLong[0]
                val lon = latLong[1]
                val latDir = if (lat >= 0) "N" else "S"
                val lonDir = if (lon >= 0) "E" else "W"
                String.format(
                    Locale.US,
                    "%.5f° %s, %.5f° %s",
                    Math.abs(lat), latDir,
                    Math.abs(lon), lonDir
                )
            } else null

            val altitudeVal = exif.getAltitude(Double.NaN)
            val gpsAltitude = if (!altitudeVal.isNaN()) {
                "${String.format(Locale.US, "%.1f", altitudeVal)} m"
            } else null

            val rawSpeed = exif.getAttribute(ExifInterface.TAG_GPS_SPEED)
            val gpsSpeed = if (!rawSpeed.isNullOrEmpty()) {
                val speedVal = rawSpeed.toDoubleOrNull()
                if (speedVal != null) "${String.format(Locale.US, "%.1f", speedVal)} km/h" else rawSpeed
            } else null

            val gpsDateStamp = exif.getAttribute("GPSDateStamp")

            val meta = ExifMetadata(
                make = make,
                model = model,
                lensModel = lensModel,
                software = software,
                dateTimeOriginal = dateTimeOriginal,
                exposureTime = exposureTime,
                fNumber = fNumber,
                iso = iso,
                focalLength = focalLength,
                focalLength35mm = focalLength35mm,
                exposureProgram = exposureProgram,
                meteringMode = meteringMode,
                whiteBalance = whiteBalance,
                flash = flash,
                colorSpace = colorSpace,
                pixelXDimension = width,
                pixelYDimension = height,
                orientation = orientation,
                gpsCoordinates = gpsCoordinates,
                gpsAltitude = gpsAltitude,
                gpsSpeed = gpsSpeed,
                gpsDateStamp = gpsDateStamp
            )

            if (meta.hasAnyExifData()) meta else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun formatExifDate(rawDate: String?): String? {
        if (rawDate.isNullOrEmpty()) return null
        return try {
            // EXIF standard: "YYYY:MM:DD HH:MM:SS"
            val inputFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            val date = inputFormat.parse(rawDate)
            if (date != null) {
                val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                outputFormat.format(date)
            } else rawDate
        } catch (_: Exception) {
            rawDate
        }
    }

    private fun formatExposureTime(rawSec: String?): String? {
        if (rawSec.isNullOrEmpty()) return null
        return try {
            val sec = rawSec.toDoubleOrNull() ?: return rawSec
            if (sec >= 1.0) {
                "${String.format(Locale.US, "%.1f", sec)}s"
            } else if (sec > 0.0) {
                val denom = Math.round(1.0 / sec)
                "1/${denom}s"
            } else rawSec
        } catch (_: Exception) {
            rawSec
        }
    }
}
