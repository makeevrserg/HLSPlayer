package com.makeevrserg.hlsplayer.ui.record

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.makeevrserg.hlsplayer.R
import com.makeevrserg.hlsplayer.databinding.CameraRecordFragmentBinding
import com.makeevrserg.hlsplayer.utils.Utils
import kotlinx.android.synthetic.main.login_popup.*

class CameraRecordFragment : Fragment() {

    private val viewModel: CameraRecordViewModel by lazy {
        ViewModelProvider(this).get(CameraRecordViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: CameraRecordFragmentBinding =
            DataBindingUtil.inflate(inflater, R.layout.camera_record_fragment, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner



        binding.buttonDate.setOnClickListener {
            activity?.let { activity ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val datePickerDialog = DatePickerDialog(activity)
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
        viewModel.date.observe(viewLifecycleOwner, {
            it?.let { date ->
                binding.buttonDate.text = "Дата: $date"
            }
        })
        viewModel.player.observe(viewLifecycleOwner, {
            it?.let { player ->
                binding.playerView.player = player

            }
        })

        viewModel.authorizationRequest.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let { handleEvent ->
                if (!handleEvent)
                    return@observe

                activity?.let { activity ->
                    val view = inflater.inflate(R.layout.login_popup, null)
                    AlertDialog
                        .Builder(activity)
                        .setMessage("Необходима авторизация")
                        .setView(view)
                        .setPositiveButton("Войти") { _, _ ->
                            viewModel.login(
                                view.findViewById<TextInputEditText>(R.id.tiedLogin).text.toString(),
                                view.findViewById<TextInputEditText>(R.id.tiedPassword).text.toString()
                            )

                        }
                        .setCancelable(false)
                        .create()
                        .show()
                }
            }
        })


        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                viewModel.onCameraSelected(p2)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}

        }
        viewModel.message.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    it,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        })

        viewModel.updatedTimeLine.observe(viewLifecycleOwner, {
            binding.progressBar.progress = it

            val bounds = binding.progressBar.thumb?.bounds?.left ?: return@observe
            binding.textViewTime.x = bounds.toFloat()
            binding.textViewTime.text = Utils.getTimeFromSeconds(it)

        })
        binding.progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, p: Boolean) {
                val bounds = seekBar?.thumb?.bounds?.left ?: return
                binding.textViewTime.x = bounds.toFloat()
                binding.textViewTime.text = Utils.getTimeFromSeconds(progress)

            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {
                viewModel.onProgressChanged(p0?.progress ?: return)

            }

        })

        viewModel.cameraNames.observe(viewLifecycleOwner, {
            binding.spinner.adapter = ArrayAdapter<String>(
                context ?: return@observe,
                R.layout.support_simple_spinner_dropdown_item,
                it
            )
        })

        return binding.root
    }


}