package com.raika.attachfilemodule.attach_file

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.net.toUri
import androidx.fragment.app.FragmentManager
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.util.Util
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.raika.attachfilemodule.R
import com.raika.attachfilemodule.file.toBase64WithReduceSize
import kotlinx.android.synthetic.main.root_dialog_compress_video.view.*
import java.io.File
import kotlin.math.abs

class BSTaskVideoPreview(
    var layout: Int,
    var file: File,
    var withDimBehind: Boolean = true,
    var withCancelOption: Boolean = true,
    var onFileChoose: (fileModel: FileModel, base64: String) -> Unit
) : BottomSheetDialogFragment() {
    
    var bottomSheetBehavior: BottomSheetBehavior<*>? = null
    lateinit var rootView: View
    private var simpleExoPlayer: SimpleExoPlayer? = null
    
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
        
                    prepareExoPlayerFromFileUri(pv_root_dialog_preview_video, context, file.toUri())
        
                    fab_done_video.setOnClickListener {
                        toBase64WithReduceSize(File(file.path)) { base64 ->
                            onFileChoose(FileModel(File(file.path), file.path.substring(file.path.lastIndexOf(".")).substring(1), file.path.substring(file.path.lastIndexOf("/")).drop(1)), base64)
                            dismiss()
                        }
                    }
        
                }
    
                bottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        when (newState) {
                            BottomSheetBehavior.STATE_HIDDEN -> {
                                dismiss()
                                simpleExoPlayer?.stop()
                            }
                            else -> {}
                        }
                    }
    
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        windowParams?.dimAmount = 1 - abs(slideOffset)
                        dialog.window?.attributes = windowParams
                    }
                })
                
//                onViewCreate(BSTaskUtils(view, this, bottomSheetBehavior))
    
            }
            
        }
        
        return view
    }
    
    fun show(fragmentManager: FragmentManager): BSTaskVideoPreview {
        this.show(fragmentManager, "")
        return this
    }
    
    private fun prepareExoPlayerFromFileUri(exoPlayer: PlayerView, context: Context, uri: Uri) {
        val simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(context)
        this.simpleExoPlayer = simpleExoPlayer
        val dataSpec = DataSpec(uri)
        val fileDataSource = FileDataSource()
        try {
            fileDataSource.open(dataSpec)
        } catch (e: FileDataSource.FileDataSourceException) {
            e.printStackTrace()
        }
        DataSource.Factory { fileDataSource }
        simpleExoPlayer.prepare(buildMediaSourceNew(context, uri))
        exoPlayer.player = simpleExoPlayer
        simpleExoPlayer.playWhenReady = true
    }
    
    private fun buildMediaSourceNew(context: Context, uri: Uri): MediaSource? {
        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
            context, Util.getUserAgent(context, context.getString(R.string.app_name))
        )
        return ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
    }
    
    override fun onStop() {
        super.onStop()
        simpleExoPlayer?.stop()
    }
    
    override fun onPause() {
        super.onPause()
        simpleExoPlayer?.playWhenReady = false
        simpleExoPlayer?.playbackState
    }
    
    override fun onResume() {
        super.onResume()
        simpleExoPlayer?.playWhenReady = true
        simpleExoPlayer?.playbackState
    }
}