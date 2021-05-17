package com.raika.attachfilemodule.attach_file

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.raika.attachfilemodule.R
import com.raika.attachfilemodule.file.bitmapToFile
import com.raika.attachfilemodule.file.toBase64WithReduceSize
import kotlinx.android.synthetic.main.root_dialog_preview.view.*
import java.io.File
import kotlin.math.abs

class BSTaskImagePreview(
    var layout: Int,
    var file: File,
    var withDimBehind: Boolean = true,
    var withCancelOption: Boolean = true,
    var onFileChoose: (fileModel: FileModel, base64: String) -> Unit
) : BottomSheetDialogFragment() {
    
    private var state: MutableLiveData<BSTaskState> = MutableLiveData(BSTaskState.Viewing)
    var bottomSheetBehavior: BottomSheetBehavior<*>? = null
    lateinit var rootView: View
    
    override fun getTheme(): Int = R.style.BottomSheetDialogAddFile
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setCanceledOnTouchOutside(withCancelOption)
        }
    }
    
    override fun isCancelable(): Boolean {
        return withCancelOption
    }
    
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        
        val view = inflater.inflate(layout, container, false)
        rootView = view
        
        dialog?.setOnShowListener {
            
            val dialog = it as BottomSheetDialog
            
            val windowParams: WindowManager.LayoutParams? = dialog.window?.attributes
            windowParams?.dimAmount = 1.0f
            dialog.window?.attributes = windowParams
            
            if (!withDimBehind) {
                dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
            
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
            
            view.post {
                val parent = view.parent as View
                val params = parent.layoutParams as CoordinatorLayout.LayoutParams
                val behavior = params.behavior
                bottomSheetBehavior = behavior as BottomSheetBehavior?
                bottomSheetBehavior?.peekHeight = view.height
                
                
                view?.apply {
                    
                    iv_root.setImageURI(Uri.fromFile(file))
                    crop_view.setImageUriAsync(Uri.fromFile(file))
                    crop_view.visibility = View.GONE
                    fab_done.setOnClickListener {
                        if (state.value == BSTaskState.Viewing) {
                            toBase64WithReduceSize(File(file.path)) { base64 ->
                                onFileChoose(FileModel(File(file.path), file.absolutePath.substring(file.absolutePath.lastIndexOf(".")).substring(1), file.absolutePath.substring(file.absolutePath.lastIndexOf("/")).drop(1)), base64)
                                dismiss()
                            }
                        } else {
                            iv_root.setImageBitmap(crop_view.croppedImage)
                            crop_view.setImageBitmap(crop_view.croppedImage)
                            state.postValue(BSTaskState.Viewing)
                            file = bitmapToFile(context, crop_view.croppedImage, System.currentTimeMillis().toString() + ".png")
                        }
                    }
                    
                    state.observe(activity as LifecycleOwner) { bsTaskState ->
                        when (bsTaskState) {
                            BSTaskState.Cropping -> {
                                crop_view.visibility = View.VISIBLE
                                rv_root_dialog_preview_filters.visibility = View.INVISIBLE
                                iv_root.visibility = View.INVISIBLE
                                bottomSheetBehavior?.isHideable = false
                                ll_options.visibility = View.VISIBLE
                                ll_options_root.visibility = View.VISIBLE
                            }
                            BSTaskState.Viewing -> {
                                crop_view.visibility = View.INVISIBLE
                                rv_root_dialog_preview_filters.visibility = View.INVISIBLE
                                iv_root.visibility = View.VISIBLE
                                bottomSheetBehavior?.isHideable = true
                                ll_options.visibility = View.VISIBLE
                                ll_options_root.visibility = View.VISIBLE
                            }
                            BSTaskState.Editing -> {
                                crop_view.visibility = View.INVISIBLE
                                rv_root_dialog_preview_filters.visibility = View.VISIBLE
                                iv_root.visibility = View.INVISIBLE
                                bottomSheetBehavior?.isHideable = true
                                ll_options.visibility = View.VISIBLE
                                ll_options_root.visibility = View.INVISIBLE
                            }
                        }
                    }
                    
                    
                    fab_crop.setOnClickListener {
                        state.postValue(BSTaskState.Cropping)
                    }
                    
                    fab_filter.setOnClickListener {
                        state.postValue(BSTaskState.Editing)
                    }
                }
                
                bottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {}
    
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        windowParams?.dimAmount = 1 - abs(slideOffset)
                        dialog.window?.attributes = windowParams
                    }
                })
            }
            
        }
        
        return view
    }
    
    fun show(fragmentManager: FragmentManager): BSTaskImagePreview {
        this.show(fragmentManager, "")
        return this
    }
    
    override fun onResume() {
        super.onResume()
        dialog?.setOnKeyListener { _, keyCode, keyEvent ->
            if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.action == KeyEvent.ACTION_UP) {
                when (state.value) {
                    BSTaskState.Cropping -> state.value = (BSTaskState.Viewing)
                    BSTaskState.Viewing -> dismiss()
                    BSTaskState.Editing -> state.value = (BSTaskState.Viewing)
                }
                true
            } else false
        }
    }
}