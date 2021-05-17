package com.raika.attachfilemodule.attach_file

import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.raika.attachfilemodule.R
import kotlin.math.abs


class BSTask(
    private var layout: Int,
    private var withDimBehind: Boolean = true,
    private var withCancelOption: Boolean = true,
    private var onBackPress: ((keyCode: Int, keyEvent: KeyEvent, bsTask: BSTask, tools: BSTaskUtils) -> Boolean)? = null,
    private var onViewCreate: (tools: BSTaskUtils) -> Unit,
) : BottomSheetDialogFragment() {
    
    private var bottomSheetBehavior: BottomSheetBehavior<*>? = null
    private lateinit var rootView: View
    
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
                
                onViewCreate(BSTaskUtils(view, this, bottomSheetBehavior))
                
                bottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        when(newState) {
                            BottomSheetBehavior.STATE_HIDDEN -> {
                                dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                                dismiss()
                            }
                            BottomSheetBehavior.STATE_COLLAPSED -> {
                                dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                                dismiss()
                            }
                            else -> {}
                        }
                    }
    
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        windowParams?.dimAmount = abs(slideOffset)
                        dialog.window?.attributes = windowParams
                    }
                })
            }
            
        }
        
        return view
    }
    
    fun show(fragmentManager: FragmentManager): BSTask {
        this.show(fragmentManager, "")
        return this
    }
    
    override fun onResume() {
        super.onResume()
        dialog?.setOnKeyListener { _, keyCode, keyEvent ->
            if (onBackPress != null) {
                onBackPress?.invoke(keyCode, keyEvent, this, BSTaskUtils(rootView, this, bottomSheetBehavior))!!
            } else {
                true
            }
        }
    }
}