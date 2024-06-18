package com.peternaggschga.sleeptalk.ui.monitoring

import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
            val intent = Intent(activity, MonitoringService::class.java).apply {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MINUTE, 1)
                putExtra(MonitoringService.INTENT_TIME_EXTRA_TAG, calendar.time)
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
