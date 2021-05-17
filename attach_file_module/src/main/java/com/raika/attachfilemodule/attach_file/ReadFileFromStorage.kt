package com.raika.attachfilemodule.attach_file

import android.app.Activity
import androidx.lifecycle.LifecycleOwner
import java.io.File

class ReadFileFromStorage(val context: Activity) {
    companion object {
        var imageFormats = arrayListOf("jpg", "JPG", "png", "PNG", "jpeg", "JPEG")
        var videoFormats = arrayListOf("mp4", "3gp", "vob")
        var musicFormats = arrayListOf("mp3", "wav")
        var documentFormats = arrayListOf("pdf", "doc", "ppt", "txt")
        var compressFormats = arrayListOf("zip", "rar", "7zip")
    }

    fun getFiles(
        formatList: ArrayList<String>,
        limitSize: Int = -1,
        listener: (files: MutableList<File>) -> Unit,
    ) {
        loadFiles(formatList).getFileList().observe(context as LifecycleOwner) {
            listener(it.filter { file -> isSmallerThanFileSize(file, limitSize) }.toMutableList())
        }
    }

    private fun loadFiles(formatList: ArrayList<String>): ReadFileViewModel {
        val files = ReadFileViewModel(context, formatList)
        files.getAllFiles()
        return files
    }

    private fun isSmallerThanFileSize(file: File, limitSize: Int): Boolean {
        if (limitSize <= 0) return true

        val fileSizeInByte = file.length().toFloat()
        val sizeInKB = fileSizeInByte / 1024
        return sizeInKB <= limitSize
    }
}