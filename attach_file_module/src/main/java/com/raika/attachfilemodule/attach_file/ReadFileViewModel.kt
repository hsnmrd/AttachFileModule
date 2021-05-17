package com.raika.attachfilemodule.attach_file

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.CoroutineContext

class ReadFileViewModel(var context: Context, var extensions: ArrayList<String>) : ViewModel(),
    CoroutineScope {
    
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main
    
    private var fileLiveData: MutableLiveData<MutableList<File>> = MutableLiveData()
    fun getFileList(): MutableLiveData<MutableList<File>> {
        return fileLiveData
    }
    
    private var files: MutableList<MyCustomFile> = mutableListOf()
    
    private fun loadFilesFromSDCard(): MutableList<File> {
        
        val uri = MediaStore.Files.getContentUri("external")
        val orderBy = MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
        
        val data = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Files.FileColumns.DATA
        } else {
            MediaStore.Files.FileColumns.DATA
        }
        val projection = arrayOf(
            data,
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATE_ADDED, MediaStore.Files.FileColumns.TITLE,
            MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        
        
        val mimes: MutableList<String?> = ArrayList()
        for (ext in extensions) {
            mimes.add(MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext))
        }
        
        val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, orderBy)
        if (cursor != null) {
            val mimeColumnIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
            val pathColumnIndex =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                } else {
                    cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                }
            
            while (cursor.moveToNext()) {
                
                Log.e("hame_11donya", "loadFilesFromSDCard: $pathColumnIndex")
                val mimeType = cursor.getString(mimeColumnIndex)
                val filePath = cursor.getString(pathColumnIndex)
                if (mimeType != null && mimes.contains(mimeType)) {
                    Log.e("hame_donya", "loadFilesFromSDCard: $mimeType")
                    // handle cursor
                    files.add(makeFile(cursor))
                } else {
                    // need to check extension, because the Mime Type is null
                    val extension: String = getExtensionByPath(filePath).toString()
                    Log.e("hame_donya__", extension)
                    if (extensions.contains(extension)) {
                        // handle cursor
                        files.add(makeFile(cursor))
                        
                    }
                }
            }
            cursor.close()
        }
        
        
        return files.map { File(it.filePath.toString()) }.toMutableList()
    }
    
    fun getAllFiles() {
        launch(Dispatchers.Main) {
            fileLiveData.postValue(withContext(Dispatchers.IO) {
                loadFilesFromSDCard()
            })
        }
    }
}

data class MyCustomFile(
    var fileId: Int = 0,
    var filePath: String? = null,
    var fileSize: String? = null,
    var fileTitle: String? = null,
    var fileDisplayName: String? = null,
    var mimeType: String? = null,
    var type: String? = null,
)


fun getExtensionByPath(path: String): String? {
    var result: String? = "%20"
    val i = path.lastIndexOf('.')
    if (i > 0) {
        result = path.substring(i + 1)
    }
    return result
}

private fun makeFile(cursor: Cursor): MyCustomFile {
    val mimeColumnIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
    val pathColumnIndex =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
        } else {
            cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
        }
    val sizeColumnIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
    val titleColumnIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.TITLE)
    val nameColumnIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
    val fileId = cursor.getInt(pathColumnIndex)
    val fileSize = cursor.getString(sizeColumnIndex)
    val fileDisplayName = cursor.getString(nameColumnIndex)
    val fileTitle = cursor.getString(titleColumnIndex)
    val filePath = cursor.getString(pathColumnIndex)
    var mimeType = cursor.getString(mimeColumnIndex)
    var type = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    if (type == null) {
        type = getExtensionByPath(filePath)
        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(type)
    }
    val result = MyCustomFile()
    result.fileId = (fileId)
    result.filePath = (filePath)
    result.fileSize = (fileSize)
    result.fileTitle = (fileTitle)
    result.fileDisplayName = (fileDisplayName)
    result.mimeType = (mimeType)
    result.type = (type)
    return result
}