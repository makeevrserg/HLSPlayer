package com.makeevrserg.hlsplayer.ui.stream

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
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

        val arguments = StreamFragmentArgs.fromBundle(requireArguments())



        viewModel.setUrl(arguments.hlsUrl)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner


        lifecycle.addObserver(viewModel.HLSPlayerObserver())


        viewModel.mediaUrl.observe(viewLifecycleOwner,{
            binding.playerView.useController = !(it?.contains("m3u8")?:false)
        })

        viewModel.player.observe(viewLifecycleOwner, {
            it?.let { binding.playerView.player = it }
        })
        /**
         * Сообщения об ошибках/подключении и названиях стримов
         */
        viewModel.message.observe(viewLifecycleOwner, {
            Snackbar.make(
                requireActivity().findViewById(android.R.id.content),
                it,
                Snackbar.LENGTH_SHORT
            ).show()
        })



        return binding.root
    }


}