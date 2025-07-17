package com.example.glucoseapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.glucoseapp.data.GlucoseData
import com.example.glucoseapp.data.HeartRateData
import com.example.glucoseapp.data.LatestVitals
import com.example.glucoseapp.data.StepsData
import com.example.glucoseapp.health.HealthConnectManager
import kotlinx.coroutines.launch
import java.time.Instant

class HealthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val healthConnectManager = HealthConnectManager(application)
    
    private val _latestVitals = MutableLiveData<LatestVitals>()
    val latestVitals: LiveData<LatestVitals> = _latestVitals
    
    private val _stepsData = MutableLiveData<List<StepsData>>()
    val stepsData: LiveData<List<StepsData>> = _stepsData
    
    private val _heartRateData = MutableLiveData<List<HeartRateData>>()
    val heartRateData: LiveData<List<HeartRateData>> = _heartRateData
    
    private val _glucoseData = MutableLiveData<List<GlucoseData>>()
    val glucoseData: LiveData<List<GlucoseData>> = _glucoseData
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _hasPermissions = MutableLiveData<Boolean>()
    val hasPermissions: LiveData<Boolean> = _hasPermissions
    
    private val _isHealthConnectAvailable = MutableLiveData<Boolean>()
    val isHealthConnectAvailable: LiveData<Boolean> = _isHealthConnectAvailable
    
    private val _detailedData = MutableLiveData<List<Any>>()
    val detailedData: LiveData<List<Any>> = _detailedData
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    fun checkHealthConnectAvailability() {
        viewModelScope.launch {
            try {
                val isAvailable = healthConnectManager.isHealthConnectAvailable()
                _isHealthConnectAvailable.value = isAvailable
                
                if (isAvailable) {
                    checkPermissions()
                }
            } catch (e: Exception) {
                _error.value = "Error checking Health Connect availability: ${e.message}"
                _isHealthConnectAvailable.value = false
            }
        }
    }
    
    fun checkPermissions() {
        viewModelScope.launch {
            try {
                val hasAllPermissions = healthConnectManager.hasAllPermissions()
                _hasPermissions.value = hasAllPermissions
                
                if (hasAllPermissions) {
                    loadLatestVitals()
                }
            } catch (e: Exception) {
                _error.value = "Error checking permissions: ${e.message}"
                _hasPermissions.value = false
            }
        }
    }
    
    fun loadLatestVitals() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val vitals = healthConnectManager.getLatestVitals()
                _latestVitals.value = vitals
                
            } catch (e: Exception) {
                _error.value = "Error loading latest vitals: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadStepsData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val steps = healthConnectManager.getStepsDataForLast7Days()
                _stepsData.value = steps
                
            } catch (e: Exception) {
                _error.value = "Error loading steps data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadHeartRateData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val heartRate = healthConnectManager.getHeartRateDataForLast7Days()
                _heartRateData.value = heartRate
                
            } catch (e: Exception) {
                _error.value = "Error loading heart rate data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadGlucoseData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val glucose = healthConnectManager.getGlucoseDataForLast7Days()
                _glucoseData.value = glucose
                
            } catch (e: Exception) {
                _error.value = "Error loading glucose data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun getPermissions() = healthConnectManager.permissions
    
    fun createPermissionRequestContract() = healthConnectManager.createPermissionRequestContract()
    
    fun clearError() {
        _error.value = null
    }
    
    fun loadDetailedData(dataType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = when (dataType) {
                    "steps" -> healthConnectManager.getStepsDataForLast7Days()
                    "heartrate" -> healthConnectManager.getHeartRateDataForLast7Days()
                    "glucose" -> healthConnectManager.getGlucoseDataForLast7Days()
                    else -> emptyList()
                }
                _detailedData.value = data
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load detailed data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Write operations
    fun addStepsRecord(steps: Long, startTime: Instant, endTime: Instant) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = healthConnectManager.insertStepsRecord(steps, startTime, endTime)
                if (success) {
                    _errorMessage.value = "Steps record added successfully"
                    loadLatestVitals() // Refresh data
                } else {
                    _errorMessage.value = "Failed to add steps record"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error adding steps: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun addHeartRateRecord(bpm: Long, time: Instant) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = healthConnectManager.insertHeartRateRecord(bpm, time)
                if (success) {
                    _errorMessage.value = "Heart rate record added successfully"
                    loadLatestVitals() // Refresh data
                } else {
                    _errorMessage.value = "Failed to add heart rate record"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error adding heart rate: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun addGlucoseRecord(level: Double, time: Instant, mealType: Int? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = healthConnectManager.insertGlucoseRecord(level, time, mealType)
                if (success) {
                    _errorMessage.value = "Glucose record added successfully"
                    loadLatestVitals() // Refresh data
                } else {
                    _errorMessage.value = "Failed to add glucose record"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error adding glucose: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Delete operations
    fun deleteRecordsByTimeRange(recordType: String, startTime: Instant, endTime: Instant) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = healthConnectManager.deleteRecordsByTimeRange(recordType, startTime, endTime)
                if (success) {
                    _errorMessage.value = "Records deleted successfully"
                    loadLatestVitals() // Refresh data
                    loadDetailedData(recordType) // Refresh detailed data if applicable
                } else {
                    _errorMessage.value = "Failed to delete records"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting records: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
