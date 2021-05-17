package com.raika.attachfilemodule.attach_file

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.raika.attachfilemodule.attach_file.BSTask


data class BSTaskUtils(
    var rootView: View,
    var bsTask: BSTask,
    var bottomSheetBehavior: BottomSheetBehavior<*>?,
)