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
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.peternaggschga.sleeptalk.MonitoringService
import com.peternaggschga.sleeptalk.databinding.FragmentMonitoringBinding

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

        binding.buttonMonitoring.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.

                ActivityCompat.requestPermissions(
                    activity as Activity,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    0
                )
            }

            val intent = Intent(activity, MonitoringService::class.java).apply {
                putExtra(
                    MonitoringService.INTENT_TIME_EXTRA_TAG,
                    SystemClock.uptimeMillis() + 60 * 1000
                )
            }
            activity?.startService(intent)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
