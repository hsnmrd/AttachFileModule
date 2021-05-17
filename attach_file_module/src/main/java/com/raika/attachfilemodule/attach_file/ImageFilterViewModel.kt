package com.raika.attachfilemodule.attach_file

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.view.ViewGroup
import androidx.core.net.toUri
import com.raika.attachfilemodule.R
import com.raika.attachfilemodule.file.bitmapToFile
import kotlinx.android.synthetic.main.root_adapter_photo_filter.view.*
import kotlinx.android.synthetic.main.root_dialog_preview.view.*
import smartadapter.viewholder.SmartViewHolder


class ImageFilterViewModel(var parentView: ViewGroup) :
    SmartViewHolder<ImageFilterModel>(parentView, R.layout.root_adapter_photo_filter) {
    
    var currentBitmap : Bitmap? = null
//    var currentFilter : Filter = None()
//    private val gpuImage = GPUImage(parentView.context)
    private var isRendered = false
    
    override fun bind(item: ImageFilterModel) {
        val ivRoot = itemView.iv_root_adapter_photo_filter
        
        val thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(item.file.path), 50, 50)
        val file = bitmapToFile(parentView.context, thumbnail, "_thumb_filter" + ".png")
        val imageUri = file.toUri()
        if (!isRendered) {
            isRendered = true
        }
//        gpuImage.setFilter(item.filter)

    }
    
}