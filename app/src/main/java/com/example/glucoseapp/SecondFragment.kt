package com.example.glucoseapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText

import com.example.glucoseapp.adapter.HealthDataAdapter
import com.example.glucoseapp.databinding.FragmentSecondBinding
import com.example.glucoseapp.viewmodel.HealthViewModel

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Detailed Health Data Fragment - Shows 7-day history for specific health metric
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!
    
    private val dataType: String by lazy {
        arguments?.getString("dataType") ?: "steps"
    }
    private val healthViewModel: HealthViewModel by viewModels()
    private lateinit var adapter: HealthDataAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        updateTitleAndLoadData()
    }
    
    private fun setupRecyclerView() {
        adapter = HealthDataAdapter()
        binding.healthDataRecycler.adapter = adapter
    }
    
    private fun setupObservers() {
        healthViewModel.stepsData.observe(viewLifecycleOwner) { stepsData ->
            if (dataType == "steps") {
                if (stepsData.isNotEmpty()) {
                    adapter.submitStepsData(stepsData)
                    binding.noDataContainer.visibility = View.GONE
                    binding.errorContainer.visibility = View.GONE
                    binding.healthDataRecycler.visibility = View.VISIBLE
                } else {
                    showNoData()
                }
            }
        }
        
        healthViewModel.heartRateData.observe(viewLifecycleOwner) { heartRateData ->
            if (dataType == "heartrate") {
                if (heartRateData.isNotEmpty()) {
                    adapter.submitHeartRateData(heartRateData)
                    binding.noDataContainer.visibility = View.GONE
                    binding.errorContainer.visibility = View.GONE
                    binding.healthDataRecycler.visibility = View.VISIBLE
                } else {
                    showNoData()
                }
            }
        }
        
        healthViewModel.glucoseData.observe(viewLifecycleOwner) { glucoseData ->
            if (dataType == "glucose") {
                if (glucoseData.isNotEmpty()) {
                    adapter.submitGlucoseData(glucoseData)
                    binding.noDataContainer.visibility = View.GONE
                    binding.errorContainer.visibility = View.GONE
                    binding.healthDataRecycler.visibility = View.VISIBLE
                } else {
                    showNoData()
                }
            }
        }
        
        healthViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarDetail.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        healthViewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.errorTextDetail.text = error
                binding.errorContainer.visibility = View.VISIBLE
                binding.healthDataRecycler.visibility = View.GONE
                binding.noDataContainer.visibility = View.GONE
            } else {
                binding.errorContainer.visibility = View.GONE
            }
        }
        
        // Observe error messages from write/delete operations
        healthViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                // Refresh data after successful operations
                if (message.contains("successfully")) {
                    loadDataForType(dataType)
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.buttonBack.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
        
        binding.refreshDetailButton.setOnClickListener {
            loadDataForType(dataType)
        }
        
        binding.addDataButton.setOnClickListener {
            showAddDataDialog()
        }
        
        binding.deleteDataButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }
    
    private fun updateTitleAndLoadData() {
        val title = when (dataType) {
            "steps" -> "Steps - Last 7 Days"
            "heartrate" -> "Heart Rate - Last 7 Days"
            "glucose" -> "Blood Glucose - Last 7 Days"
            else -> "Health Data - Last 7 Days"
        }
        binding.titleText.text = title
        
        loadDataForType(dataType)
    }
    
    private fun loadDataForType(dataType: String) {
        when (dataType) {
            "steps" -> healthViewModel.loadStepsData()
            "heartrate" -> healthViewModel.loadHeartRateData()
            "glucose" -> healthViewModel.loadGlucoseData()
        }
    }
    
    private fun showNoData() {
        binding.healthDataRecycler.visibility = View.GONE
        binding.noDataContainer.visibility = View.VISIBLE
        binding.errorContainer.visibility = View.GONE
    }
    
    private fun showAddDataDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_health_data, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
            
        val titleText = dialogView.findViewById<android.widget.TextView>(R.id.dialogTitle)
        val valueInput = dialogView.findViewById<TextInputEditText>(R.id.valueInput)
        val dateInput = dialogView.findViewById<TextInputEditText>(R.id.dateInput)
        val timeInput = dialogView.findViewById<TextInputEditText>(R.id.timeInput)
        val mealTypeInput = dialogView.findViewById<TextInputEditText>(R.id.mealTypeInput)
        val mealTypeLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.mealTypeInputLayout)
        val addButton = dialogView.findViewById<android.widget.Button>(R.id.addButton)
        val cancelButton = dialogView.findViewById<android.widget.Button>(R.id.cancelButton)
        
        // Configure dialog based on data type
        when (dataType) {
            "steps" -> {
                titleText.text = "Add Steps Data"
                valueInput.hint = "Steps count"
                mealTypeLayout.visibility = View.GONE
            }
            "heartrate" -> {
                titleText.text = "Add Heart Rate Data"
                valueInput.hint = "BPM"
                mealTypeLayout.visibility = View.GONE
            }
            "glucose" -> {
                titleText.text = "Add Glucose Data"
                valueInput.hint = "Glucose level (mg/dL)"
                mealTypeLayout.visibility = View.VISIBLE
            }
        }
        
        // Set current date and time as default
        val now = LocalDate.now()
        val currentTime = LocalTime.now()
        dateInput.setText(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        timeInput.setText(currentTime.format(DateTimeFormatter.ofPattern("HH:mm")))
        
        addButton.setOnClickListener {
            val valueText = valueInput.text.toString()
            val dateText = dateInput.text.toString()
            val timeText = timeInput.text.toString()
            val mealTypeText = mealTypeInput.text.toString()
            
            if (valueText.isBlank() || dateText.isBlank() || timeText.isBlank()) {
                Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            try {
                val date = LocalDate.parse(dateText, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val time = LocalTime.parse(timeText, DateTimeFormatter.ofPattern("HH:mm"))
                val instant = date.atTime(time).toInstant(ZoneOffset.UTC)
                
                when (dataType) {
                    "steps" -> {
                        val steps = valueText.toLong()
                        val endTime = instant.plusSeconds(3600) // 1 hour duration
                        healthViewModel.addStepsRecord(steps, instant, endTime)
                    }
                    "heartrate" -> {
                        val bpm = valueText.toLong()
                        healthViewModel.addHeartRateRecord(bpm, instant)
                    }
                    "glucose" -> {
                        val level = valueText.toDouble()
                        val mealType = if (mealTypeText.isNotBlank()) mealTypeText.toIntOrNull() else null
                        healthViewModel.addGlucoseRecord(level, instant, mealType)
                    }
                }
                
                dialog.dismiss()
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Invalid input: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete All Data")
            .setMessage("Are you sure you want to delete all ${getDataTypeDisplayName()} data from the last 7 days? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val now = Instant.now()
                val sevenDaysAgo = now.minusSeconds(7 * 24 * 60 * 60)
                healthViewModel.deleteRecordsByTimeRange(dataType, sevenDaysAgo, now)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun getDataTypeDisplayName(): String {
        return when (dataType) {
            "steps" -> "steps"
            "heartrate" -> "heart rate"
            "glucose" -> "glucose"
            else -> "health"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}