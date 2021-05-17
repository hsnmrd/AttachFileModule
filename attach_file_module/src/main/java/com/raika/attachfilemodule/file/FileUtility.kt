package com.raika.attachfilemodule.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.StrictMode
import android.util.Base64
import android.util.Log
import com.raika.alertmodule.progress.ModuleProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.net.URL
import java.nio.channels.FileChannel
import java.text.DecimalFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

fun calculateFileSize(targetFile: Uri, limitSize: Int): Boolean {
    if (limitSize <= 0) return true
    val file = File(targetFile.path.toString())
    val fileSizeInByte = file.length().toFloat()
    val sizeInKB = fileSizeInByte / 1024
    return sizeInKB <= limitSize
}

fun File.fileSize(): String {
    val size = this.length()
    
    val hrSize: String?
    
    val b = size.toDouble()
    val k = size / 1024.0
    val m = size / 1024.0 / 1024.0
    val g = size / 1024.0 / 1024.0 / 1024.0
    val t = size / 1024.0 / 1024.0 / 1024.0 / 1024.0
    
    val dec = DecimalFormat("0.00")
    
    hrSize = when {
        t > 1 -> dec.format(t) + " TB"
        g > 1 -> dec.format(g) + " GB"
        m > 1 -> dec.format(m) + " MB"
        k > 1 -> dec.format(k) + " KB"
        else -> dec.format(b) + " B"
    }
    
    return hrSize
}

private fun resizeBitMapImage(filePath: String?, targetWidth: Int, targetHeight: Int): Bitmap? {
    var bitMapImage: Bitmap? = null
    try {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, options)
        var sampleSize = 0.0
        val scaleByHeight = abs(options.outHeight - targetHeight) >= Math.abs(options.outWidth - targetWidth)
        if (options.outHeight * options.outWidth * 2 >= 1638) {
            sampleSize = if (scaleByHeight) (options.outHeight / targetHeight).toDouble() else options.outWidth / targetWidth.toDouble()
            sampleSize = 2.0.pow(floor(ln(sampleSize) / ln(2.0))) as Double
        }
        options.inJustDecodeBounds = false
        options.inTempStorage = ByteArray(128)
        while (true) {
            try {
                options.inSampleSize = sampleSize.toInt()
                bitMapImage = BitmapFactory.decodeFile(filePath, options)
                break
            } catch (ex: Exception) {
                try {
                    sampleSize *= 2
                } catch (ex1: Exception) {
                }
            }
        }
    } catch (ex: Exception) {
    }
    return bitMapImage
}

private fun sizeOf(data: Bitmap): Int {
    return data.byteCount / 1024
}

fun toBase64WithReduceSize(path: File?, targetSize: Int = 1000, quality: Int = 100, progress: ModuleProgress? = null, listener: (base64: String) -> Unit) {
    progress?.show()
    GlobalScope.launch {
        var isCompress = false
        var bitmap: Bitmap? = resizeBitMapImage(path?.absolutePath, 1000, 1000)
        for (i in 1000 downTo 50 step 10) {
            bitmap?.let { bitmapObj ->
                if (sizeOf(bitmapObj) < targetSize) {
                    isCompress = true
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmapObj.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
                    val byteArray = byteArrayOutputStream.toByteArray()
                    withContext(Dispatchers.Main) {
                        progress?.hide()
                        listener(Base64.encodeToString(byteArray, Base64.DEFAULT))
                    }
                    return@launch
                } else {
                    bitmap = resizeBitMapImage(path?.absolutePath, i, i)
                }
            }
        }
        
        if (!isCompress) {
            withContext(Dispatchers.IO) {
                progress?.hide()
                listener("null")
            }
        }
    }
}

fun toBase64(file: File?, listener: (base64: String) -> Unit) {
    var bytes: ByteArray? = ByteArray(0)
    try {
        bytes = file?.let { loadFile(it) }
    } catch (e: IOException) {
        Log.e("base64file : error", e.toString())
    }
    listener(Base64.encodeToString(bytes, Base64.DEFAULT))
}


private fun loadFile(file: File): ByteArray? {
    val inputStream: InputStream = FileInputStream(file)
    val length: Long = file.length()
    if (length > Int.MAX_VALUE) {
        Log.e("base64file : file is ..", "")
    }
    val bytes = ByteArray(length.toInt())
    var offset = 0
    var numRead: Int? = null
    while (offset < bytes.size && inputStream.read(bytes, offset, bytes.size - offset).also { numRead = it } >= 0) {
        numRead?.let { offset += it }
    }
    if (offset < bytes.size) {
        throw IOException("base64file : Could not completely read file " + file.name)
    }
    inputStream.close()
    return bytes
}


fun bitmapToFile(context: Context, bitmap: Bitmap, filename: String): File {
    val file = File(context.cacheDir, filename)
    file.createNewFile()
    
    val bos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos)
    val byteArray: ByteArray = bos.toByteArray()
    
    val fileOutputStream = FileOutputStream(file)
    fileOutputStream.write(byteArray)
    fileOutputStream.flush()
    fileOutputStream.close()
    
    return file
}

fun fileToBitmap(file: File): Bitmap? {
    val filePath = file.path
    return BitmapFactory.decodeFile(filePath)
}

fun urlToBitmap(url: String): String? {
    try {
        val imageUrl = URL(url)
        val urlConnection = imageUrl.openConnection()
        val inputStream = urlConnection.getInputStream()
        val outputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var read: Int
        while (inputStream.read(buffer, 0, buffer.size).also { read = it } != -1) {
            outputStream.write(buffer, 0, read)
        }
        outputStream.flush()
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    } catch (e: Exception) {
        Log.d("Error__", e.toString())
    }
    return null
}

fun urlToBase64(url: String): String? {
    val newUrl: URL
    val bitmap: Bitmap
    var base64: String? = ""
    
    try {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        newUrl = URL(url)
        bitmap = BitmapFactory.decodeStream(newUrl.openConnection().getInputStream())
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
    return base64
}

fun String.toBlob(formatOfFile: String): String {
    return "data:image/${formatOfFile};base64,$this"
}


fun copyFileOrDirectory(srcDir: String?, dstDir: String?, newFileName: String, deleteOldFile: Boolean = false) {
    try {
        val src = File(srcDir.toString())
        val dst = File(dstDir, newFileName)
        if (src.isDirectory) {
            val files = src.list()
            files?.let {
                val filesLength = files.size
                for (i in 0 until filesLength) {
                    val src1 = File(src, files[i]).path
                    val dst1 = dst.path
                    copyFileOrDirectory(src1, dst1, newFileName)
                }
            }
        } else {
            copyFile(src, dst)
        }
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
}

@Throws(IOException::class)
private fun copyFile(sourceFile: File?, destFile: File) {
    destFile.parentFile?.let {
        if (!it.exists()) {
            it.mkdirs()
        } else {
            deleteFile(it)
            it.mkdirs()
        }
        if (!destFile.exists()) {
            destFile.createNewFile()
        }
        var source: FileChannel? = null
        var destination: FileChannel? = null
        try {
            source = FileInputStream(sourceFile).channel
            destination = FileOutputStream(destFile).channel
            destination.transferFrom(source, 0, source.size())
        } finally {
            source?.close()
            destination?.close()
        }
    }
}

fun deleteFile(file: File?) {
    file?.delete()
}