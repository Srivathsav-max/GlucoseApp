package com.example.glucoseapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.health.connect.client.PermissionController
import com.example.glucoseapp.databinding.FragmentFirstBinding
import com.example.glucoseapp.viewmodel.HealthViewModel

/**
 * Health Dashboard Fragment - Shows latest vitals from Health Connect
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    
    private val healthViewModel: HealthViewModel by viewModels()
    
    // Permission request launcher
    private val requestPermissionActivityContract = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(setOf(
            "android.permission.health.READ_STEPS",
            "android.permission.health.READ_HEART_RATE",
            "android.permission.health.READ_BLOOD_GLUCOSE"
        ))) {
            // All permissions granted
            healthViewModel.checkPermissions()
        } else {
            // Some permissions denied
            Toast.makeText(requireContext(), "Health Connect permissions are required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        setupClickListeners()
        
        // Check Health Connect availability
        healthViewModel.checkHealthConnectAvailability()
    }
    
    private fun setupObservers() {
        healthViewModel.isHealthConnectAvailable.observe(viewLifecycleOwner) { isAvailable ->
            if (!isAvailable) {
                binding.errorText.text = "Health Connect is not available on this device"
                binding.errorText.visibility = View.VISIBLE
            }
        }
        
        healthViewModel.hasPermissions.observe(viewLifecycleOwner) { hasPermissions ->
            if (hasPermissions) {
                binding.permissionLayout.visibility = View.GONE
                healthViewModel.loadLatestVitals()
            } else {
                binding.permissionLayout.visibility = View.VISIBLE
            }
        }
        
        healthViewModel.latestVitals.observe(viewLifecycleOwner) { vitals ->
            updateVitalsDisplay(vitals)
        }
        
        healthViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        healthViewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.errorText.text = error
                binding.errorText.visibility = View.VISIBLE
            } else {
                binding.errorText.visibility = View.GONE
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.requestPermissionsButton.setOnClickListener {
            requestHealthConnectPermissions()
        }
        
        binding.refreshButton.setOnClickListener {
            healthViewModel.loadLatestVitals()
        }
        
        // Navigate to detailed views when cards are clicked
        binding.stepsCard.setOnClickListener {
            val bundle = Bundle().apply {
                putString("dataType", "steps")
            }
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment, bundle)
        }
        
        binding.heartRateCard.setOnClickListener {
            val bundle = Bundle().apply {
                putString("dataType", "heartrate")
            }
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment, bundle)
        }
        
        binding.glucoseCard.setOnClickListener {
            val bundle = Bundle().apply {
                putString("dataType", "glucose")
            }
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment, bundle)
        }
    }
    
    private fun requestHealthConnectPermissions() {
        val permissions = setOf(
            "android.permission.health.READ_STEPS",
            "android.permission.health.READ_HEART_RATE",
            "android.permission.health.READ_BLOOD_GLUCOSE",
            "android.permission.health.WRITE_STEPS",
            "android.permission.health.WRITE_HEART_RATE",
            "android.permission.health.WRITE_BLOOD_GLUCOSE"
        )
        requestPermissionActivityContract.launch(permissions)
    }
    
    private fun updateVitalsDisplay(vitals: com.example.glucoseapp.data.LatestVitals) {
        // Update Steps
        vitals.latestSteps?.let { steps ->
            binding.stepsValue.text = steps.count.toString()
            binding.stepsTime.text = "Last updated: ${steps.getFormattedDateTime()}"
        } ?: run {
            binding.stepsValue.text = "No data"
            binding.stepsTime.text = "--"
        }
        
        // Update Heart Rate
        vitals.latestHeartRate?.let { heartRate ->
            binding.heartRateValue.text = heartRate.beatsPerMinute.toString()
            binding.heartRateTime.text = "Last updated: ${heartRate.getFormattedDateTime()}"
        } ?: run {
            binding.heartRateValue.text = "--"
            binding.heartRateTime.text = "No data"
        }
        
        // Update Glucose
        vitals.latestGlucose?.let { glucose ->
            binding.glucoseValue.text = glucose.getFormattedLevel()
            binding.glucoseTime.text = "Last updated: ${glucose.getFormattedDateTime()}"
        } ?: run {
            binding.glucoseValue.text = "--"
            binding.glucoseTime.text = "No data"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}