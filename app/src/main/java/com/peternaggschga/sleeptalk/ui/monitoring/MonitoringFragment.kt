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
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.peternaggschga.sleeptalk.databinding.FragmentMonitoringBinding
import com.peternaggschga.sleeptalk.domain.monitoring.MonitoringService

class MonitoringFragment : Fragment() {

    private var _binding: FragmentMonitoringBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val monitoringViewModel = ViewModelProvider(this)[MonitoringViewModel::class.java]

        _binding = FragmentMonitoringBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textMonitoring
        monitoringViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        binding.buttonChooseTime.setOnClickListener {
            TODO()
            monitoringViewModel.endingTime = SystemClock.uptimeMillis() + 60 * 1000 * 10
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

            if ((monitoringViewModel.endingTime ?: 0) < SystemClock.uptimeMillis()) {
                Toast.makeText(
                    context,
                    "Please select how long you will be sleeping first!", // TODO: use string resource
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            activity?.startService(
                Intent(activity, MonitoringService::class.java).apply {
                    putExtra(
                        MonitoringService.INTENT_TIME_EXTRA_TAG,
                        monitoringViewModel.endingTime
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
