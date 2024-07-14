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

    private val monitoringViewModel: MonitoringViewModel by activityViewModels { MonitoringViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = FragmentMonitoringBinding.inflate(inflater, container, false)
        val root: View = binding.root

        updateStopTimeIfServiceIsRunning()

        monitoringViewModel.timeTillEnd.observe(
            viewLifecycleOwner,
            RemainingTimeViewAdapter(
                binding.numberPickerHours,
                binding.numberPickerMinutes
            )
        )

        binding.buttonStopped.setOnClickListener {
            TimePickerDialogFragment().show(
                requireActivity().supportFragmentManager,
                TimePickerDialogFragment.TAG
            )
        }

        binding.buttonReady.setOnClickListener {
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
                // TODO: show information that click must be repeated or start monitoring on permission granted
                return@setOnClickListener
            }

            if (!monitoringViewModel.timeTillEnd.isInitialized ||
                monitoringViewModel.timeTillEnd.value == Pair(0, 0)
            ) {
                return@setOnClickListener
            }

            Intent(activity, MonitoringService::class.java).apply {
                putExtra(
                    MonitoringService.INTENT_TIME_EXTRA_TAG,
                    monitoringViewModel.endingTime.value
                )
            }.also { intent ->
                requireActivity().startService(intent)
            }
            monitoringViewModel.setServiceRunning(true)
        }

        binding.buttonRunning.setOnClickListener {
            monitoringViewModel.setServiceRunning(false)
            Intent(activity, MonitoringService::class.java).also { intent ->
                requireActivity().stopService(intent)
            }
        }

        binding.buttonFlipper.apply {
            setInAnimation(requireContext(), android.R.anim.fade_in)
            setOutAnimation(requireContext(), android.R.anim.fade_out)
        }

        monitoringViewModel.monitoringState.observe(
            viewLifecycleOwner
        ) { value ->
            // TODO: use two buttons again (one for start/stop, one for time selection
            binding.buttonFlipper.displayedChild = when (value) {
                MonitoringState.STOPPED, null -> 0
                MonitoringState.READY -> 1
                MonitoringState.RUNNING -> 2
            }
        }

        return root
    }

    private fun updateStopTimeIfServiceIsRunning() {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as MonitoringService.MonitoringBinder
                binder.stopTime?.let { time ->
                    monitoringViewModel.setEndingTime(time)
                    monitoringViewModel.setServiceRunning(true)
                }

                requireActivity().unbindService(this)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
            }
        }

        Intent(context, MonitoringService::class.java).also { intent ->
            requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
