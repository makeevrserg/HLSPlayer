package com.makeevrserg.hlsplayer.ui.auth

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.makeevrserg.hlsplayer.R
import com.makeevrserg.hlsplayer.databinding.AuthFragmentBinding
import java.util.*

class AuthFragment : Fragment() {

    private val viewModel: AuthViewModel by lazy {
        ViewModelProvider(this).get(AuthViewModel::class.java)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: AuthFragmentBinding =
            DataBindingUtil.inflate(inflater, R.layout.auth_fragment, container, false)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner


        binding.buttonAuth.setOnClickListener {
            viewModel.onAuthClicked(
                login = binding.editTextLogin.text.toString(),
                password = binding.editTextPassword.text.toString()
            )
        }
        binding.buttonUserInfo.setOnClickListener {
            viewModel.onUserInfoClicked()
        }
        binding.buttonLogout.setOnClickListener {
            viewModel.onUserLogout()
        }
        binding.buttonCameras.setOnClickListener {
            viewModel.onCamerasClicked()
        }

        viewModel.logMessage.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                binding.textViewLog.text = it
            }
        })
        viewModel.message.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    it,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        })


        setupRecyclerView(binding)

        viewModel.mediaURL.observe(viewLifecycleOwner, {
            it?.let {
                this.findNavController().navigate(
                    AuthFragmentDirections.actionAuthFragmentToStreamFragment().setHlsUrl(it)
                )
                viewModel.doneShowUrl()
            }
        })

        viewModel.cameraTimestamps.observe(viewLifecycleOwner, { timestamps ->
            activity?.let { fragmentActivity ->
                val list = mutableListOf<String>()
                for (timestamp in timestamps)
                    list.add(timestamp.started_at)
                AlertDialog
                    .Builder(fragmentActivity)
                    .setTitle("Выдор даты")
                    .setItems(list.toTypedArray()) { _, position ->
                        viewModel.onTimestampSelected(position)

                    }.create().show()
            }
        })

        viewModel.selectCameraDialog.observe(viewLifecycleOwner, { showDialog ->

            if (showDialog.getContentIfNotHandled() != true)
                return@observe

            activity?.let { fragmentActivity ->
                AlertDialog.Builder(fragmentActivity).setTitle("Что открыть")
                    .setItems(
                        arrayOf("LiveStream", "Видео по времени")
                    ) { _, which ->
                        when (which) {
                            0 -> {
                                viewModel.onLiveStreamSelected()
                                return@setItems
                            }
                            1 -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    val datePickerDialog = DatePickerDialog(fragmentActivity)
                                    datePickerDialog.setOnDateSetListener { _, year, month, day ->
                                        viewModel.onDateSelected(
                                            year,
                                            month,
                                            day
                                        )

                                    }
                                    datePickerDialog.show()


                                }
                            }
                        }
                    }.create().show()
            }
        })
        return binding.root
    }

    private fun setupRecyclerView(binding: AuthFragmentBinding) {
        val camerasAdapter = CamerasAdapter(viewModel)
        binding.camerasList.adapter = camerasAdapter

        viewModel.cameras.observe(viewLifecycleOwner, {
            it?.let {
                camerasAdapter.submitList(it)
            }
        })
    }


}