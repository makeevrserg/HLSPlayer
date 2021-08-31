package com.makeevrserg.hlsplayer.ui.auth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.makeevrserg.hlsplayer.databinding.CameraItemBinding
import com.makeevrserg.hlsplayer.network.cubicapi.response.CameraItem

class CamerasAdapter(private val viewModel: AuthViewModel) : ListAdapter<CameraItem, CameraViewHolder>(CameraDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CameraViewHolder {
        return CameraViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: CameraViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(viewModel, item)
    }
}

class CameraViewHolder private constructor(private val binding: CameraItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(viewModel: AuthViewModel, camera: CameraItem) {
        binding.viewmodel = viewModel
        binding.camera = camera
        binding.executePendingBindings()
    }

    companion object {
        fun from(parent: ViewGroup): CameraViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = CameraItemBinding.inflate(layoutInflater, parent, false)

            return CameraViewHolder(binding)
        }
    }
}

/**
 * Callback for calculating the diff between two non-null items in a list.
 *
 * Used by ListAdapter to calculate the minimum number of changes between and old list and a new
 * list that's been passed to `submitList`.
 */
class CameraDiffCallback : DiffUtil.ItemCallback<CameraItem>() {
    override fun areItemsTheSame(oldItem: CameraItem, newItem: CameraItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: CameraItem, newItem: CameraItem): Boolean {
        return oldItem == newItem
    }
}