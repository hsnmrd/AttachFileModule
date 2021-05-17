package com.raika.attachfilemodule.attach_file

import android.annotation.SuppressLint
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.google.android.material.card.MaterialCardView
import com.raika.attachfilemodule.R
import com.raika.attachfilemodule.file.calculateFileSize
import com.raika.attachfilemodule.file.fileSize
import java.io.File
import java.util.*

class BSTaskAddFileAdapter(var limitSize: Int = -1) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var dataList: MutableList<FileModel> = ArrayList()
    private var formatFiles: String = ""

    companion object {
        const val TYPE_GRID = 0
        const val TYPE_LIST = 2
        const val TYPE_EMPTY_ITEM = -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_GRID -> {
                val itemView: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.root_adapter_file_grid, parent, false)
                MyViewHolderGrid(itemView)
            }
            TYPE_LIST -> {
                val itemView: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.root_adapter_file_list, parent, false)
                MyViewHolderList(itemView)
            }
            TYPE_EMPTY_ITEM -> {
                val itemView: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.root_adapter_cl_empty, parent, false)
                MyViewHolderEmpty(itemView)
            }
            else -> {
                val itemView: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.root_adapter_file_grid, parent, false)
                MyViewHolderGrid(itemView)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val data = dataList[position]
        when (holder.itemViewType) {

            TYPE_GRID -> {
                holder as MyViewHolderGrid

                holder.cvRoot.setOnClickListener { }

                if (data.isSmallerThanLimitSize) {
                    holder.ivLimit.visibility = View.VISIBLE
                } else {
                    holder.ivLimit.visibility = View.GONE
                }

                when (data.file?.absolutePath) {
                    "from storage" -> {
                        when (data.fileFormat) {
                            "documentFormats" -> holder.ivRoot.setImageResource(R.drawable.ic_doc_file)
                            "compressFormats" -> holder.ivRoot.setImageResource(R.drawable.ic_compress_file)
                            "audioFormats" -> holder.ivRoot.setImageResource(R.drawable.ic_music_file)
                            "videoFormats" -> holder.ivRoot.setImageResource(R.drawable.ic_video_file)
                            "imageFormats" -> data.file.let {
                                holder.ivRoot.load(it) {
                                    transformations(RoundedCornersTransformation(50f))
                                }
                            }
                        }
                    }
                    else -> {
                        when (data.fileFormat) {
                            "documentFormats" -> holder.ivRoot.setImageResource(R.drawable.ic_doc_file)
                            "compressFormats" -> holder.ivRoot.setImageResource(R.drawable.ic_compress_file)
                            "audioFormats" -> holder.ivRoot.setImageResource(R.drawable.ic_music_file)
                            "videoFormats" -> holder.ivRoot.setImageResource(R.drawable.ic_video_file)
                            "imageFormats" -> data.file?.let {
                                holder.ivRoot.load(it) {
                                    transformations(RoundedCornersTransformation(50f))
                                }
                            }
                        }
                    }
                }
            }

            TYPE_LIST -> {
                holder as MyViewHolderList
                holder.tvRoot.text =
                    data.file?.absolutePath?.substring(data.file.absolutePath.lastIndexOf("/") + 1)
                holder.tvFileSize.text = data.file?.fileSize()

                holder.cvRoot.setOnClickListener { }

                when (data.fileFormat) {
                    "documentFormats" -> holder.ivRoot.setImageResource(R.drawable.ic_doc_file)
                    "compressFormats" -> holder.ivRoot.setImageResource(R.drawable.ic_compress_file)
                    "audioFormats" -> holder.ivRoot.setImageResource(R.drawable.ic_music_file)
                    "videoFormats" -> holder.ivRoot.setImageResource(R.drawable.ic_video_file)
                    "imageFormats" -> data.file?.let {
                        holder.ivRoot.load(it) {
                            transformations(RoundedCornersTransformation(50f))
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount() = dataList.size

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return when (dataList[position].fileFormat) {
            "emptyList" -> TYPE_EMPTY_ITEM
            "imageFormats" -> TYPE_GRID
            else -> TYPE_LIST
        }
    }

    fun setData(format: String, dataList: List<File>) {
        formatFiles = format
        this.dataList.clear()
        dataList.forEach {
            this.dataList.add(
                FileModel(
                    it,
                    format,
                    it.absolutePath,
                    calculateFileSize(Uri.parse(it.absolutePath), limitSize)
                )
            )
        }
    }

    fun addData(dataList: List<File>) {
        dataList.forEach {
            if (this.dataList.none { fileSheet -> fileSheet.file?.absolutePath == it.absolutePath }) {
                this.dataList.add(FileModel(it, formatFiles))
            }
        }
    }

    fun getData(): MutableList<FileModel> {
        return this.dataList
    }

    internal inner class MyViewHolderEmpty(view: View) : RecyclerView.ViewHolder(view)

    internal inner class MyViewHolderLoadMore(view: View) : RecyclerView.ViewHolder(view)

    internal inner class MyViewHolderGrid(view: View) : RecyclerView.ViewHolder(view) {
        val ivRoot: AppCompatImageView = view.findViewById(R.id.iv_adapter_file_root)
        val ivLimit: AppCompatImageView = view.findViewById(R.id.iv_adapter_file_root_limit)
        val cvRoot: MaterialCardView = view.findViewById(R.id.cv_adapter_file_root)
    }

    internal inner class MyViewHolderList(view: View) : RecyclerView.ViewHolder(view) {
        val ivRoot: AppCompatImageView = view.findViewById(R.id.iv_adapter_file_root)
        val tvRoot: TextView = view.findViewById(R.id.tv_adapter_file_list)
        val tvFileSize: TextView = view.findViewById(R.id.tv_adapter_file_list_size)
        val cvRoot: MaterialCardView = view.findViewById(R.id.cv_adapter_file_root)
    }

    internal inner class MyViewHolderPaging(view: View) : RecyclerView.ViewHolder(view)

}