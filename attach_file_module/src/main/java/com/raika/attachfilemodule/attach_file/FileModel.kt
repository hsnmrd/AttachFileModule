package com.raika.attachfilemodule.attach_file

import java.io.File

data class FileModel(
    val file: File? = null,
    val fileFormat: String = "",
    val fileName: String = "",
    var isSmallerThanLimitSize: Boolean = true
)