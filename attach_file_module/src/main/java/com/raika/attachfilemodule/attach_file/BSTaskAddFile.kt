package com.raika.attachfilemodule.attach_file

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.shape.CornerFamily
import com.raika.attachfilemodule.R
import com.raika.attachfilemodule.RecyclerTouchListener
import com.raika.attachfilemodule.file.FileGenerating.getRealPath
import com.raika.attachfilemodule.file.calculateFileSize
import com.raika.attachfilemodule.file.toBase64
import com.raika.modulepermission.permission.PERMISSION_CAMERA
import com.raika.modulepermission.permission.PERMISSION_READ_STORAGE
import com.raika.modulepermission.permission.PERMISSION_WRITE_STORAGE
import com.raika.modulepermission.permission.PermissionModule
import com.raika.popupmodule.popup.MenuModel
import com.raika.popupmodule.popup.showPopupMenuOnClicked
import kotlinx.android.synthetic.main.root_bottom_sheet_add_file.*
import kotlinx.android.synthetic.main.root_bottom_sheet_add_file.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

sealed class BSTaskState {
    object Cropping : BSTaskState()
    object Viewing : BSTaskState()
    object Editing : BSTaskState()
}

class BSTaskAddFile(
    var defaultEnableTab: BSTaskTab,
    var maxSize: Int = -1,
    var showImageFiles: Boolean = false,
    var showVideoFiles: Boolean = false,
    var showCompressFiles: Boolean = false,
    var showDocumentFiles: Boolean = false,
    var showMusicFiles: Boolean = false,
    var onFileChoose: (fileModel: FileModel, base64: String) -> Unit,
) : BottomSheetDialogFragment() {

    companion object {
        private const val IMAGE_CAMERA_RESULT = 1
        private const val IMAGE_FILE_MANAGER_RESULT = 2
        private const val VIDEO_CAMERA_RESULT = 3
        private const val VIDEO_FILE_MANAGER_RESULT = 4
        private const val AUDIO_FILE_MANAGER_RESULT = 5
        private const val DOCUMENT_FILE_MANAGER_RESULT = 6
        private const val COMPRESS_FILE_MANAGER_RESULT = 7
    }


    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var bsAdapter: BSTaskAddFileAdapter? = null
    private var fileList: MutableList<File> = ArrayList()
    private var currentTab: BSTaskTab? = null
    private var imageUri: Uri? = null
    private var fileUri: Uri? = null
    private var state: MutableLiveData<BSTaskState> = MutableLiveData(BSTaskState.Viewing)

    private lateinit var appbar: AppBarLayout

    private lateinit var fabImage: ExtendedFloatingActionButton
    private lateinit var fabDocument: ExtendedFloatingActionButton
    private lateinit var fabZip: ExtendedFloatingActionButton
    private lateinit var fabVideo: ExtendedFloatingActionButton
    private lateinit var fabMusic: ExtendedFloatingActionButton
    private lateinit var llImage: LinearLayout
    private lateinit var llDocument: LinearLayout
    private lateinit var llZip: LinearLayout
    private lateinit var llVideo: LinearLayout
    private lateinit var llMusic: LinearLayout
    private lateinit var rvRoot: RecyclerView
    private lateinit var tvToolbarTitle: TextView

    var screenHeight: Int? = 0
    var bottomSheetBehaviorFiles: BottomSheetBehavior<*>? = null

    override fun getTheme(): Int = R.style.BottomSheetDialogAddFile

    override fun setHasOptionsMenu(hasMenu: Boolean) {
        super.setHasOptionsMenu(true)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setCanceledOnTouchOutside(true)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.root_bottom_sheet_add_file, container, false)

        appbar = view.findViewById(R.id.abl_root_bottom_sheet_add_file_top_toolbar)
        fabDocument = view.findViewById(R.id.fab_bottom_sheet_add_file_document)
        fabImage = view.findViewById(R.id.fab_bottom_sheet_add_file_image)
        fabVideo = view.findViewById(R.id.fab_bottom_sheet_add_file_video)
        fabMusic = view.findViewById(R.id.fab_bottom_sheet_add_file_music)
        fabZip = view.findViewById(R.id.fab_bottom_sheet_add_file_zip)
        llDocument = view.findViewById(R.id.ll_bottom_sheet_add_file_document)
        llImage = view.findViewById(R.id.ll_bottom_sheet_add_file_image)
        llVideo = view.findViewById(R.id.ll_bottom_sheet_add_file_video)
        llMusic = view.findViewById(R.id.ll_bottom_sheet_add_file_music)
        llZip = view.findViewById(R.id.ll_bottom_sheet_add_file_zip)
        rvRoot = view.findViewById(R.id.rv_bottom_sheet_add_file_root)
        tvToolbarTitle = view.findViewById(R.id.tv_bottom_sheet_add_file_toolbar_title)


        dialog?.setOnKeyListener { _, keyCode, keyEvent ->
            handleRootBottomSheetBackListener(keyCode, keyEvent)
        }

        dialog?.setOnShowListener {

            val dialog = it as BottomSheetDialog
            val content: View? = it.window?.findViewById(R.id.design_bottom_sheet)
            (content?.layoutParams as CoordinatorLayout.LayoutParams).behavior = null

            val bottomSheet =
                dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            screenHeight = context?.resources?.displayMetrics?.heightPixels

            view?.cv_root_bottom_sheet_add_file_root?.shapeAppearanceModel =
                cv_root_bottom_sheet_add_file_root.shapeAppearanceModel.toBuilder()
                    .setTopLeftCorner(CornerFamily.ROUNDED, 1f * 150)
                    .setTopRightCorner(CornerFamily.ROUNDED, 1f * 150)
                    .setBottomRightCornerSize(0f)
                    .setBottomLeftCornerSize(0f)
                    .build()

            val parentFile = view?.cv_root_bottom_sheet_add_file_root
            val paramsFile = parentFile?.layoutParams as CoordinatorLayout.LayoutParams
            val behaviorFile = paramsFile.behavior
            bottomSheetBehaviorFiles = behaviorFile as BottomSheetBehavior?

            var isToolbarVisible = true

            bottomSheetBehaviorFiles?.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {

                        BottomSheetBehavior.STATE_DRAGGING -> dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                        BottomSheetBehavior.STATE_SETTLING -> dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                        BottomSheetBehavior.STATE_HALF_EXPANDED -> dialog.window?.addFlags(
                            WindowManager.LayoutParams.FLAG_DIM_BEHIND
                        )

                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            view.cv_root_bottom_sheet_add_file_slide_from_here.alpha = 1f
                            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                        }

                        BottomSheetBehavior.STATE_EXPANDED -> {
                            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                            YoYo.with(Techniques.SlideOutDown).duration(200)
                                .playOn(view.ll_bottom_tool_option)
                            isToolbarVisible = false
                        }

                        BottomSheetBehavior.STATE_HIDDEN -> {
                            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                            dismiss()
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {

                    if (bottomSheet.y <= screenHeight!! / 2) {
                        val alpha = bottomSheet.y / (screenHeight!! / 1.3f)
                        view.cv_root_bottom_sheet_add_file_slide_from_here.alpha = alpha
                    }

                    if (bottomSheet.y <= 200) {
                        val alpha = bottomSheet.y / 200
                        view.cv_root_bottom_sheet_add_file_root?.shapeAppearanceModel =
                            cv_root_bottom_sheet_add_file_root.shapeAppearanceModel.toBuilder()
                                .setTopLeftCorner(
                                    CornerFamily.ROUNDED, alpha * 150
                                ).setTopRightCorner(CornerFamily.ROUNDED, alpha * 150)
                                .setBottomRightCornerSize(0f).setBottomLeftCornerSize(0f).build()

                    } else {
                        view.cv_root_bottom_sheet_add_file_root?.shapeAppearanceModel =
                            cv_root_bottom_sheet_add_file_root.shapeAppearanceModel.toBuilder()
                                .setTopLeftCorner(
                                    CornerFamily.ROUNDED, 1f * 150
                                ).setTopRightCorner(CornerFamily.ROUNDED, 1f * 150)
                                .setBottomRightCornerSize(0f).setBottomLeftCornerSize(0f).build()
                    }

                    if (bottomSheet.y <= 150) {
                        val alpha = bottomSheet.y / 150
                        appbar.alpha = 1 - alpha
                    } else {
                        if (!isToolbarVisible) {
                            YoYo.with(Techniques.SlideInUp).duration(200)
                                .playOn(view.ll_bottom_tool_option)
                            isToolbarVisible = true
                        }
                        appbar.alpha = 0f
                    }

                }
            })

            bottomSheetBehaviorFiles?.state = BottomSheetBehavior.STATE_COLLAPSED
            bottomSheet?.requestLayout()
        }

        view.fab_bottom_sheet_add_file_toolbar_option.setOnClickListener {
            if (appbar.alpha == 1f) {
                when (currentTab) {
                    BSTaskTab.Image -> {
                        context?.showPopupMenuOnClicked(
                            targetView = view.fab_bottom_sheet_add_file_toolbar_option,
                            dataList = mutableListOf(
                                MenuModel(id = 1, title = "گالری", icon = R.drawable.ic_gallery),
                                MenuModel(id = 2, title = "دوربین", icon = R.drawable.ic_camera),
                            )
                        ) {
                            when (it.item.id.toInt()) {
                                1 -> openImageGallery()
                                2 -> openCamera()
                            }
                        }
                    }
                    BSTaskTab.Video -> {
                        context?.showPopupMenuOnClicked(
                            targetView = view.fab_bottom_sheet_add_file_toolbar_option,
                            dataList = mutableListOf(
                                MenuModel(id = 1, title = "گالری", icon = R.drawable.ic_gallery),
                                MenuModel(
                                    id = 2,
                                    title = "ضبط ویدیو",
                                    icon = R.drawable.ic_video_record
                                ),
                            )
                        ) {
                            when (it.item.id.toInt()) {
                                1 -> openVideoGallery()
                                2 -> recordVideo()
                            }
                        }
                    }
                    BSTaskTab.Compress -> {
                        context?.showPopupMenuOnClicked(
                            targetView = view.fab_bottom_sheet_add_file_toolbar_option,
                            dataList = mutableListOf(
                                MenuModel(
                                    id = 1,
                                    title = "مدیریت فایل",
                                    icon = R.drawable.ic_folder
                                ),
                            )
                        ) { openCompressFileManager() }
                    }
                    BSTaskTab.Document -> {
                        context?.showPopupMenuOnClicked(
                            targetView = view.fab_bottom_sheet_add_file_toolbar_option,
                            dataList = mutableListOf(
                                MenuModel(
                                    id = 1,
                                    title = "مدیریت فایل",
                                    icon = R.drawable.ic_folder
                                ),
                            )
                        ) { openDocumentFileManager() }
                    }
                    BSTaskTab.Music -> {
                        context?.showPopupMenuOnClicked(
                            targetView = view.fab_bottom_sheet_add_file_toolbar_option,
                            dataList = mutableListOf(
                                MenuModel(
                                    id = 1,
                                    title = "مدیریت فایل",
                                    icon = R.drawable.ic_folder
                                ),
                            )
                        ) { openMusicFileManager() }
                    }
                    null -> {
                    }
                }
            }
        }

        appbar.setOnTouchListener { v, event ->
            true
        }

        view.fab_bottom_sheet_add_file_toolbar_back.setOnClickListener {
            if (bottomSheetBehaviorFiles?.state == BottomSheetBehavior.STATE_COLLAPSED) {
                dismiss()
            } else {
                bottomSheetBehaviorFiles?.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        if (showImageFiles) {
            llImage.visibility = View.VISIBLE
        } else {
            llImage.visibility = View.GONE
        }

        if (showVideoFiles) {
            llVideo.visibility = View.VISIBLE
        } else {
            llVideo.visibility = View.GONE
        }

        if (showMusicFiles) {
            llMusic.visibility = View.VISIBLE
        } else {
            llMusic.visibility = View.GONE
        }

        if (showCompressFiles) {
            llZip.visibility = View.VISIBLE
        } else {
            llZip.visibility = View.GONE
        }

        if (showDocumentFiles) {
            llDocument.visibility = View.VISIBLE
        } else {
            llDocument.visibility = View.GONE
        }

        return view
    }


    private fun handleRootBottomSheetBackListener(keyCode: Int, keyEvent: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.action == KeyEvent.ACTION_UP) {
            if (bottomSheetBehaviorFiles?.state == BottomSheetBehavior.STATE_COLLAPSED) {
                dismiss()
            } else {
                bottomSheetBehaviorFiles?.state = BottomSheetBehavior.STATE_COLLAPSED
            }
            true
        } else false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bsAdapter = BSTaskAddFileAdapter(maxSize)

        rvRoot.adapter = bsAdapter
        rvRoot.layoutManager =
            object : GridLayoutManager(context, 3, RecyclerView.VERTICAL, false) {
                override fun isLayoutRTL(): Boolean {
                    return true
                }
            }

        rvRoot.addOnItemTouchListener(
            RecyclerTouchListener(
                activity,
                rvRoot,
                object : RecyclerTouchListener.ClickListener {
                    override fun onClick(view: View?, position: Int) {
                        synchronized(this) {
                            bsAdapter?.getData()?.let {
                                var isImage = false
                                var isVideo = false
                                ReadFileFromStorage.imageFormats.forEach { format ->
                                    if (format == it[position].file?.absolutePath.toString()
                                            .substring(
                                                it[position].file?.absolutePath.toString()
                                                    .lastIndexOf(".")
                                            ).substring(1)
                                    ) {
                                        it[position].file?.let { file -> openImagePreview(file) }
                                        isImage = true
                                    }
                                }
                                if (!isImage) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        ReadFileFromStorage.videoFormats.forEach { format ->
                                            if (format == it[position].file?.absolutePath.toString()
                                                    .substring(
                                                        it[position].file?.absolutePath.toString()
                                                            .lastIndexOf(".")
                                                    ).substring(1)
                                            ) {
                                                it[position].file?.let { file ->
                                                    openVideoPreview(
                                                        file
                                                    )
                                                }
                                                isVideo = true
                                            }
                                        }
                                    }
                                }
                                if (!isImage && !isVideo) {
                                    toBase64(it[position].file) { base64 ->
                                        GlobalScope.launch {
                                            withContext(Dispatchers.Main) {
                                                onFileChoose(
                                                    FileModel(
                                                        it[position].file,
                                                        it[position].file?.absolutePath.toString()
                                                            .substring(
                                                                it[position].file?.absolutePath.toString()
                                                                    .lastIndexOf(".")
                                                            ).substring(1),
                                                        it[position].file?.absolutePath.toString()
                                                            .substring(
                                                                it[position].file?.absolutePath.toString()
                                                                    .lastIndexOf("/")
                                                            ).drop(1)
                                                    ), base64
                                                )
                                            }
                                        }
                                    }
                                    this@BSTaskAddFile.dismiss()
                                }
                            }
                        }
                    }

                    override fun onLongClick(view: View?, position: Int) {}
                })
        )


        currentTab = defaultEnableTab
        when (defaultEnableTab) {
            BSTaskTab.Image -> imageFilesChosen()
            BSTaskTab.Video -> videoFileChosen()
            BSTaskTab.Compress -> zipFileChosen()
            BSTaskTab.Document -> docFileChosen()
            BSTaskTab.Music -> musicFileChosen()
        }


        fabDocument.setOnClickListener { docFileChosen() }
        fabZip.setOnClickListener { zipFileChosen() }
        fabImage.setOnClickListener { imageFilesChosen() }
        fabMusic.setOnClickListener { musicFileChosen() }
        fabVideo.setOnClickListener { videoFileChosen() }
    }

    private fun docFileChosen() {
        tvToolbarTitle.text = "انتخاب سند"
        fabImage.shrink()
        fabDocument.extend()
        fabMusic.shrink()
        fabVideo.shrink()
        fabZip.shrink()
        if (currentTab == BSTaskTab.Document) {
            bottomSheetBehaviorFiles?.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            bottomSheetBehaviorFiles?.state = BottomSheetBehavior.STATE_COLLAPSED
            currentTab = BSTaskTab.Document
        }
        loadDocuments()
    }

    private fun zipFileChosen() {
        tvToolbarTitle.text = "انتخاب فایل"
        fabImage.shrink()
        fabDocument.shrink()
        fabMusic.shrink()
        fabVideo.shrink()
        fabZip.extend()
        if (currentTab == BSTaskTab.Compress) {
            bottomSheetBehaviorFiles?.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            bottomSheetBehaviorFiles?.state = BottomSheetBehavior.STATE_COLLAPSED
            currentTab = BSTaskTab.Compress
        }
        loadZips()
    }

    private fun videoFileChosen() {
        tvToolbarTitle.text = "انتخاب ویدیو"
        fabImage.shrink()
        fabDocument.shrink()
        fabMusic.shrink()
        fabVideo.extend()
        fabZip.shrink()
        if (currentTab == BSTaskTab.Video) {
            bottomSheetBehaviorFiles?.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            bottomSheetBehaviorFiles?.state = BottomSheetBehavior.STATE_COLLAPSED
            currentTab = BSTaskTab.Video
        }

        loadVideos()
    }

    private fun musicFileChosen() {
        tvToolbarTitle.text = "انتخاب موسیقی"
        fabImage.shrink()
        fabDocument.shrink()
        fabMusic.extend()
        fabVideo.shrink()
        fabZip.shrink()
        if (currentTab == BSTaskTab.Music) {
            bottomSheetBehaviorFiles?.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            bottomSheetBehaviorFiles?.state = BottomSheetBehavior.STATE_COLLAPSED
            currentTab = BSTaskTab.Music
        }
        loadMusics()
    }

    private fun imageFilesChosen() {
        tvToolbarTitle.text = "انتخاب عکس"
        fabImage.extend()
        fabDocument.shrink()
        fabMusic.shrink()
        fabVideo.shrink()
        fabZip.shrink()
        if (currentTab == BSTaskTab.Image) {
            bottomSheetBehaviorFiles?.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            bottomSheetBehaviorFiles?.state = BottomSheetBehavior.STATE_COLLAPSED
            currentTab = BSTaskTab.Image
        }
        loadImages()
    }

    private fun openImageGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, IMAGE_FILE_MANAGER_RESULT)
    }

    private fun openVideoGallery() {
        val intent = Intent()
        intent.type = "video/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, VIDEO_FILE_MANAGER_RESULT)
    }

    private fun openMusicFileManager() {
        val intent = Intent()
        intent.type = "audio/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, AUDIO_FILE_MANAGER_RESULT)
    }

    private fun openDocumentFileManager() {
        val mimeTypes = arrayOf(
            "application/pdf",
            "application/msword",
            "application/vnd.ms-powerpoint",
            "application/vnd.ms-excel",
            "text/plain"
        )
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.type = if (mimeTypes.size == 1) mimeTypes[0] else "*/*"
            if (mimeTypes.isNotEmpty()) {
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }
        } else {
            var mimeTypesStr = ""
            for (mimeType in mimeTypes) {
                mimeTypesStr += "$mimeType|"
            }
            intent.type = mimeTypesStr.substring(0, mimeTypesStr.length - 1)
        }
        startActivityForResult(
            Intent.createChooser(intent, "ChooseFile"),
            DOCUMENT_FILE_MANAGER_RESULT
        )
    }

    private fun openCompressFileManager() {
        val mimeTypes =
            arrayOf("application/zip, application/octet-stream, application/x-zip-compressed, multipart/x-zip")
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.type = if (mimeTypes.size == 1) mimeTypes[0] else "*/*"
            if (mimeTypes.isNotEmpty()) {
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }
        } else {
            var mimeTypesStr = ""
            for (mimeType in mimeTypes) {
                mimeTypesStr += "$mimeType|"
            }
            intent.type = mimeTypesStr.substring(0, mimeTypesStr.length - 1)
        }
        startActivityForResult(
            Intent.createChooser(intent, "ChooseFile"),
            COMPRESS_FILE_MANAGER_RESULT
        )
    }

    private fun recordVideo() {
        checkCameraPermission(context) {
            checkStoragePermission(context) {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.TITLE, "my_video_title")
                values.put(MediaStore.Images.Media.DESCRIPTION, "my_video_description")
                fileUri = context?.contentResolver?.insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    values
                )
                val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
                startActivityForResult(intent, VIDEO_CAMERA_RESULT)
            }
        }
    }

    private fun openCamera() {
        checkCameraPermission(context) {
            checkStoragePermission(context) {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.TITLE, "my_pic_title")
                values.put(MediaStore.Images.Media.DESCRIPTION, "my_pic_description")
                imageUri = context?.contentResolver?.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                startActivityForResult(intent, IMAGE_CAMERA_RESULT)
            }
        }
    }

    private fun loadVideos() {
        fileList.clear()
        bsAdapter?.setData("videoFormats", mutableListOf())
        bsAdapter?.notifyDataSetChanged()
        checkStoragePermission(context) {
            ReadFileFromStorage(context as Activity).getFiles(
                ReadFileFromStorage.videoFormats,
                maxSize
            ) { files ->
                fileList.addAll(files)
                fileList.sortByDescending { Date(it.lastModified()) }
                rvRoot.adapter = bsAdapter
                rvRoot.layoutManager =
                    object : GridLayoutManager(context, 2, RecyclerView.VERTICAL, false) {
                        override fun isLayoutRTL(): Boolean {
                            return true
                        }
                    }
                bsAdapter?.addData(fileList)
                bsAdapter?.notifyDataSetChanged()
            }
        }
    }

    private fun loadMusics() {
        fileList.clear()
        bsAdapter?.setData("audioFormats", mutableListOf())
        bsAdapter?.notifyDataSetChanged()
        checkStoragePermission(context) {
            ReadFileFromStorage(context as Activity).getFiles(
                ReadFileFromStorage.musicFormats,
                maxSize
            ) { files ->
                fileList.addAll(files)
                fileList.sortByDescending { Date(it.lastModified()) }
                rvRoot.adapter = bsAdapter
                rvRoot.layoutManager =
                    object : GridLayoutManager(context, 2, RecyclerView.VERTICAL, false) {
                        override fun isLayoutRTL(): Boolean {
                            return true
                        }
                    }
                bsAdapter?.addData(fileList)
                bsAdapter?.notifyDataSetChanged()
            }
        }
    }

    private fun loadImages() {
        fileList.clear()
        bsAdapter?.setData("imageFormats", mutableListOf())
        bsAdapter?.notifyDataSetChanged()
        checkStoragePermission(context) {
            ReadFileFromStorage(context as Activity).getFiles(
                ReadFileFromStorage.imageFormats,
                maxSize
            ) { files ->
                fileList.addAll(files)
                fileList.sortByDescending { Date(it.lastModified()) }
                rvRoot.adapter = bsAdapter
                rvRoot.layoutManager =
                    object : GridLayoutManager(context, 3, RecyclerView.VERTICAL, false) {
                        override fun isLayoutRTL(): Boolean {
                            return true
                        }
                    }
                bsAdapter?.addData(fileList)
                bsAdapter?.notifyDataSetChanged()
            }
        }
    }

    private fun loadZips() {
        fileList.clear()
        bsAdapter?.setData("compressFormats", mutableListOf())
        bsAdapter?.notifyDataSetChanged()
        checkStoragePermission(context) {
            ReadFileFromStorage(context as Activity).getFiles(
                ReadFileFromStorage.compressFormats,
                maxSize
            ) { files ->
                fileList.addAll(files)
                fileList.sortByDescending { Date(it.lastModified()) }
                rvRoot.adapter = bsAdapter
                rvRoot.layoutManager =
                    object : GridLayoutManager(context, 2, RecyclerView.VERTICAL, false) {
                        override fun isLayoutRTL(): Boolean {
                            return true
                        }
                    }
                bsAdapter?.addData(fileList)
                bsAdapter?.notifyDataSetChanged()
            }
        }
    }

    private fun loadDocuments() {
        fileList.clear()
        bsAdapter?.setData("documentFormats", mutableListOf())
        bsAdapter?.notifyDataSetChanged()
        checkStoragePermission(context) {
            ReadFileFromStorage(context as Activity).getFiles(
                ReadFileFromStorage.documentFormats,
                maxSize
            ) { files ->
                fileList.addAll(files)
                fileList.sortByDescending { Date(it.lastModified()) }
                rvRoot.adapter = bsAdapter
                rvRoot.layoutManager =
                    object : GridLayoutManager(context, 2, RecyclerView.VERTICAL, false) {
                        override fun isLayoutRTL(): Boolean {
                            return true
                        }
                    }
                bsAdapter?.addData(fileList)
                bsAdapter?.notifyDataSetChanged()
            }
        }
    }

    private fun checkStoragePermission(context: Context?, listener: () -> Unit) {
        context?.let {
            PermissionModule(
                it,
                PERMISSION_READ_STORAGE,
                PERMISSION_WRITE_STORAGE
            ).ask { listener() }
        }
    }

    private fun checkCameraPermission(context: Context?, listener: () -> Unit) {
        context?.let {
            PermissionModule(it, PERMISSION_CAMERA).ask { listener() }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_CAMERA_RESULT && resultCode == Activity.RESULT_OK) {
            val path: String? = getRealPath(context, imageUri)
            openImagePreview(File(path.toString()))
        }
        if (requestCode == VIDEO_CAMERA_RESULT && resultCode == Activity.RESULT_OK) {
            val path: String? = getRealPath(context, fileUri)
            openVideoPreview(File(path.toString()))
        }
        if (data != null) {
            if (requestCode == IMAGE_FILE_MANAGER_RESULT && resultCode == Activity.RESULT_OK) {
                val path: String? = getRealPath(context, data.data)
                openImagePreview(File(path.toString()))
            }
            if (requestCode == VIDEO_FILE_MANAGER_RESULT && resultCode == Activity.RESULT_OK) {
                val path: String? = getRealPath(context, data.data)
                openVideoPreview(File(path.toString()))
            }
            if (requestCode == AUDIO_FILE_MANAGER_RESULT || requestCode == COMPRESS_FILE_MANAGER_RESULT || requestCode == DOCUMENT_FILE_MANAGER_RESULT && resultCode == Activity.RESULT_OK) {
                val path: String? = getRealPath(context, data.data)
                if (calculateFileSize(Uri.parse(path), maxSize)) {
                    var base64Root: String
                    toBase64(File(path.toString())) { base64 ->
                        base64Root = base64
                        GlobalScope.launch {
                            withContext(Dispatchers.Main) {
                                onFileChoose(
                                    FileModel(
                                        File(path.toString()),
                                        path?.substring(path.lastIndexOf("."))?.substring(1)
                                            .toString(),
                                        path?.substring(path.lastIndexOf("/"))?.drop(1).toString()
                                    ),
                                    base64Root
                                )
                                this@BSTaskAddFile.dismiss()
                            }
                        }
                    }
                    this.dismiss()
                } else {
                    Toast.makeText(context, "حجم بیشتر از حد مجاز", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun show(fragmentManager: FragmentManager) {
        this.show(fragmentManager, "")
    }

    fun openImagePreview(file: File) {
        BSTaskImagePreview(
            layout = R.layout.root_dialog_preview,
            file = file,
            withDimBehind = true,
            withCancelOption = false
        ) { fileSheet, base64 ->
            fileSheet.file?.let {
                GlobalScope.launch {
                    withContext(Dispatchers.Main) {
                        onFileChoose(
                            FileModel(
                                File(fileSheet.file.path),
                                fileSheet.file.path.substring(fileSheet.file.path.lastIndexOf("."))
                                    .substring(1),
                                fileSheet.file.path.substring(fileSheet.file.path.lastIndexOf("/"))
                                    .drop(1)
                            ), base64
                        )
                        dismiss()
                    }
                }
            }
        }.show(childFragmentManager)
    }


    fun openVideoPreview(file: File) {
        BSTaskVideoPreview(
            layout = R.layout.root_dialog_compress_video,
            file = file,
            withDimBehind = true,
            withCancelOption = false
        ) { fileSheet, base64 ->
            fileSheet.file?.let {
                GlobalScope.launch {
                    withContext(Dispatchers.Main) {
                        onFileChoose(
                            FileModel(
                                File(fileSheet.file.path),
                                fileSheet.file.path.substring(fileSheet.file.path.lastIndexOf("."))
                                    .substring(1),
                                fileSheet.file.path.substring(fileSheet.file.path.lastIndexOf("/"))
                                    .drop(1)
                            ), base64
                        )
                    }
                }
                dismiss()
            }
        }.show(childFragmentManager)
    }
}