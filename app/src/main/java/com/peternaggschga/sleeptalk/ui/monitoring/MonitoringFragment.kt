package com.peternaggschga.sleeptalk.ui.monitoring

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val monitoringViewModel: MonitoringViewModel by activityViewModels { MonitoringViewModel.Factory }

        _binding = FragmentMonitoringBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // TODO: bind with MonitoringService

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

            val timeTillEnd = monitoringViewModel.timeTillEnd.value ?: return@setOnClickListener

            activity?.startService(
                Intent(activity, MonitoringService::class.java).apply {
                    putExtra(
                        MonitoringService.INTENT_TIME_EXTRA_TAG,
                        SystemClock.uptimeMillis() + (timeTillEnd.first * 60 + timeTillEnd.second) * 60 * 1000
                    )
                }
            )
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
