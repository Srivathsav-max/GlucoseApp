package com.example.glucoseapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.glucoseapp.data.GlucoseData
import com.example.glucoseapp.data.HeartRateData
import com.example.glucoseapp.data.StepsData
import com.example.glucoseapp.databinding.ItemHealthDataBinding

sealed class HealthDataItem {
    data class StepsItem(val data: StepsData) : HealthDataItem()
    data class HeartRateItem(val data: HeartRateData) : HealthDataItem()
    data class GlucoseItem(val data: GlucoseData) : HealthDataItem()
}

class HealthDataAdapter : RecyclerView.Adapter<HealthDataAdapter.HealthDataViewHolder>() {
    
    private var items = listOf<HealthDataItem>()
    
    fun submitStepsData(stepsData: List<StepsData>) {
        items = stepsData.map { HealthDataItem.StepsItem(it) }
        notifyDataSetChanged()
    }
    
    fun submitHeartRateData(heartRateData: List<HeartRateData>) {
        items = heartRateData.map { HealthDataItem.HeartRateItem(it) }
        notifyDataSetChanged()
    }
    
    fun submitGlucoseData(glucoseData: List<GlucoseData>) {
        items = glucoseData.map { HealthDataItem.GlucoseItem(it) }
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HealthDataViewHolder {
        val binding = ItemHealthDataBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HealthDataViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: HealthDataViewHolder, position: Int) {
        holder.bind(items[position])
    }
    
    override fun getItemCount(): Int = items.size
    
    class HealthDataViewHolder(private val binding: ItemHealthDataBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: HealthDataItem) {
            when (item) {
                is HealthDataItem.StepsItem -> {
                    val steps = item.data
                    binding.valueText.text = "${steps.count} steps"
                    binding.dateText.text = steps.getFormattedDate()
                    binding.timeText.text = steps.getFormattedDateTime()
                    binding.additionalInfo.visibility = View.GONE
                }
                
                is HealthDataItem.HeartRateItem -> {
                    val heartRate = item.data
                    binding.valueText.text = "${heartRate.beatsPerMinute} bpm"
                    binding.dateText.text = heartRate.getFormattedDate()
                    binding.timeText.text = heartRate.getFormattedDateTime()
                    binding.additionalInfo.visibility = View.GONE
                }
                
                is HealthDataItem.GlucoseItem -> {
                    val glucose = item.data
                    binding.valueText.text = glucose.getFormattedLevel()
                    binding.dateText.text = glucose.getFormattedDate()
                    binding.timeText.text = glucose.getFormattedDateTime()
                    
                    glucose.mealType?.let { mealType ->
                        binding.additionalInfo.text = "Meal type: $mealType"
                        binding.additionalInfo.visibility = View.VISIBLE
                    } ?: run {
                        binding.additionalInfo.visibility = View.GONE
                    }
                }
            }
        }
    }
}
