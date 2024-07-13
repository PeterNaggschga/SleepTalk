package com.peternaggschga.sleeptalk.ui.monitoring

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.peternaggschga.sleeptalk.databinding.FragmentMonitoringBinding
import com.peternaggschga.sleeptalk.domain.monitoring.MonitoringService

class MonitoringFragment : Fragment() {

    private var _binding: FragmentMonitoringBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var serviceBinder: MonitoringService.MonitoringBinder? = null

    private val monitoringViewModel: MonitoringViewModel by activityViewModels { MonitoringViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        Intent(context, MonitoringService::class.java).also { intent ->
            requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        _binding = FragmentMonitoringBinding.inflate(inflater, container, false)
        val root: View = binding.root

        monitoringViewModel.timeTillEnd.observe(
            viewLifecycleOwner,
            RemainingTimeViewAdapter(
                binding.numberPickerHours,
                binding.numberPickerMinutes
            )
        )

        binding.buttonChooseTime.setOnClickListener {
            TimePickerDialogFragment().show(
                requireActivity().supportFragmentManager,
                TimePickerDialogFragment.TAG
            )
        }

        binding.buttonStartMonitoring.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity as Activity,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    0
                )
                return@setOnClickListener
            }

            if (!monitoringViewModel.timeTillEnd.isInitialized ||
                monitoringViewModel.timeTillEnd.value == Pair(0, 0)
            ) {
                return@setOnClickListener
            }

            requireActivity().startService(
                Intent(activity, MonitoringService::class.java).apply {
                    putExtra(
                        MonitoringService.INTENT_TIME_EXTRA_TAG,
                        monitoringViewModel.endingTime.value
                    )
                }
            )
        }

        return root
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serviceBinder = service as MonitoringService.MonitoringBinder
            serviceBinder?.let { binder ->
                binder.getService().stopTime?.let { time ->
                    monitoringViewModel.setEndingTime(time)
                }
            }

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBinder = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
