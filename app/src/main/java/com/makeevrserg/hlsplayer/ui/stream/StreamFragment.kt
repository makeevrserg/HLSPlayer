package com.makeevrserg.hlsplayer.ui.stream

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.makeevrserg.hlsplayer.R
import com.makeevrserg.hlsplayer.databinding.StreamFragmentBinding

class StreamFragment : Fragment() {


    private val viewModel: StreamViewModel by lazy {
        ViewModelProvider(this).get(StreamViewModel::class.java)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: StreamFragmentBinding =
            DataBindingUtil.inflate(inflater, R.layout.stream_fragment, container, false)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner


        lifecycle.addObserver(viewModel.HLSPlayerObserver())




        viewModel.player.observe(viewLifecycleOwner,{
            binding.playerView.player = it
        })



        return binding.root
    }






}