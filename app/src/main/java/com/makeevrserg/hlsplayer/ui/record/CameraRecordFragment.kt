package com.makeevrserg.hlsplayer.ui.record

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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


        //Обновление даты
        viewModel.date.observe(viewLifecycleOwner, {
            binding.buttonDate.text = "Дата: ${it ?: return@observe}"
        })

        //Инициализируем плеер
        viewModel.player.observe(viewLifecycleOwner, {
            binding.playerView.player = it ?: return@observe
        })

        //Слушатель события сообщения для отображения их на Snackbar'е
        viewModel.message.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    it,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        })

        //Обновление позиции ползунка
        viewModel.updatedTimeLine.observe(viewLifecycleOwner, {
            //Сам прогрессбар
            binding.progressBar.progress = it

            //Индикатор прогресса
            binding.indicatorProgressBar.progress =
                binding.progressBar.progress / binding.progressBar.max.toFloat()

            //TextView с  временем
            //Чтобы не вылезало за рамки layout'а - ставим ограничение
            if ((binding.progressBar.progress / binding.progressBar.max) < 0.8) {
                val bounds = binding.progressBar.thumb?.bounds?.left ?: return@observe
                binding.textViewTime.x = bounds.toFloat()
                binding.textViewTime.text = Utils.getTimeFromSeconds(it)
            }

        })

        //После получения эвентов загружаем их на наш индикатор.
        //Key - время в секундах, Int - Color
        viewModel.events.observe(viewLifecycleOwner, {
            binding.indicatorProgressBar.indicatorPositions = it
            binding.indicatorProgressBar.invalidate()
        })


        //Слушатель реквеста авторизации
        viewModel.authorizationRequest.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let { handleEvent ->
                if (!handleEvent)
                    return@observe
                showAuthDialog(inflater)
            }
        })

        //Обновляем спиннер с камерами
        viewModel.cameraNames.observe(viewLifecycleOwner, {
            binding.spinner.adapter = ArrayAdapter<String>(
                context ?: return@observe,
                R.layout.support_simple_spinner_dropdown_item,
                it
            )
        })

        //Создаём диалоговое окно для выбора даты
        binding.buttonDate.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val datePickerDialog = DatePickerDialog(activity ?: return@setOnClickListener)
                datePickerDialog.setOnDateSetListener(onDateSelected())
                datePickerDialog.show()
            }
        }

        //Слушатель выбора камер
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                viewModel.onCameraSelected(index)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }


        binding.buttonPrevEvent.setOnClickListener {
            viewModel.onPrevEventClicked()
        }
        binding.buttonNextEvent.setOnClickListener {
            viewModel.onNextEventClicked()
        }
        binding.buttonNext10sec.setOnClickListener {
            viewModel.onNext10SecClicked()
        }
        binding.buttonPrev10sec.setOnClickListener {
            viewModel.onPrev10SecClicked()
        }


        binding.indicatorProgressBar.progress =
            binding.progressBar.progress / binding.progressBar.max.toFloat()

        //Устанавливаем паддинг прогрессБару, чтобы он совпадал с индикатором.
        binding.progressBar.setPadding(0, 0, 0, 0)



        binding.progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            //При изменении позиции сикбара мы должны сместить TextView со временем и обновить индикатор
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, p: Boolean) {
                binding.indicatorProgressBar.progress =
                    binding.progressBar.progress / binding.progressBar.max.toFloat()
                if ((binding.progressBar.progress / binding.progressBar.max) < 0.8) {
                    val bounds = seekBar?.thumb?.bounds?.left ?: return
                    binding.textViewTime.x = bounds.toFloat()
                    binding.textViewTime.text = Utils.getTimeFromSeconds(progress)
                }

            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            //Уведомляем viewModel о том, что пользователь выбрал новое время
            override fun onStopTrackingTouch(p0: SeekBar?) {
                viewModel.onProgressChanged(p0?.progress ?: return)
            }

        })



        return binding.root
    }

    /**
     * Диалог с авторизацией
     */
    private fun showAuthDialog(inflater: LayoutInflater) {
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

    /**
     * Листенер выбранной даты
     */
    private fun onDateSelected() = DatePickerDialog.OnDateSetListener { _, year, month, day ->
        viewModel.onDateSelected(
            year,
            month,
            day
        )

    }


}