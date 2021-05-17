package com.raika.attachfilemodule.attach_file

sealed class BSTaskTab {
    object Image: BSTaskTab()
    object Video: BSTaskTab()
    object Compress: BSTaskTab()
    object Document: BSTaskTab()
    object Music: BSTaskTab()
}