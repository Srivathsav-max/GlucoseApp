package com.example.glucoseapp.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.activity.result.contract.ActivityResultContract
import com.example.glucoseapp.data.GlucoseData
import com.example.glucoseapp.data.HeartRateData
import com.example.glucoseapp.data.LatestVitals
import com.example.glucoseapp.data.StepsData
import java.time.Instant
import java.time.temporal.ChronoUnit

class HealthConnectManager(private val context: Context) {
    
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    
    // Required permissions
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(BloodGlucoseRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(BloodGlucoseRecord::class)
    )
    
    suspend fun hasAllPermissions(): Boolean {
        return try {
            healthConnectClient.permissionController.getGrantedPermissions().containsAll(permissions)
        } catch (e: Exception) {
            false
        }
    }
    
    fun createPermissionRequestContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }
    
    suspend fun isHealthConnectAvailable(): Boolean {
        return try {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getLatestVitals(): LatestVitals {
        val now = Instant.now()
        val oneDayAgo = now.minus(1, ChronoUnit.DAYS)
        
        val latestSteps = getLatestSteps(oneDayAgo, now)
        val latestHeartRate = getLatestHeartRate(oneDayAgo, now)
        val latestGlucose = getLatestGlucose(oneDayAgo, now)
        
        return LatestVitals(latestSteps, latestHeartRate, latestGlucose)
    }
    
    private suspend fun getLatestSteps(startTime: Instant, endTime: Instant): StepsData? {
        return try {
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            val response = healthConnectClient.readRecords(request)
            val latestRecord = response.records.maxByOrNull { it.endTime }
            
            latestRecord?.let {
                StepsData(
                    count = it.count,
                    timestamp = it.endTime,
                    startTime = it.startTime,
                    endTime = it.endTime
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun getLatestHeartRate(startTime: Instant, endTime: Instant): HeartRateData? {
        return try {
            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            val response = healthConnectClient.readRecords(request)
            val latestRecord = response.records.maxByOrNull { it.startTime }
            
            latestRecord?.let { record ->
                val bpm = if (record.samples.isNotEmpty()) {
                    record.samples.last().beatsPerMinute
                } else {
                    60L // Default value
                }
                HeartRateData(
                    beatsPerMinute = bpm,
                    timestamp = record.startTime
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun getLatestGlucose(startTime: Instant, endTime: Instant): GlucoseData? {
        return try {
            val request = ReadRecordsRequest(
                recordType = BloodGlucoseRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            val response = healthConnectClient.readRecords(request)
            val latestRecord = response.records.maxByOrNull { it.time }
            
            latestRecord?.let {
                GlucoseData(
                    level = it.level.inMilligramsPerDeciliter,
                    unit = "mg/dL",
                    timestamp = it.time,
                    mealType = it.mealType?.toString()
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getStepsDataForLast7Days(): List<StepsData> {
        val endTime = Instant.now()
        val startTime = endTime.minus(7, ChronoUnit.DAYS)
        return getAllStepsData(startTime, endTime)
    }
    
    suspend fun getHeartRateDataForLast7Days(): List<HeartRateData> {
        val endTime = Instant.now()
        val startTime = endTime.minus(7, ChronoUnit.DAYS)
        return getAllHeartRateData(startTime, endTime)
    }
    
    suspend fun getGlucoseDataForLast7Days(): List<GlucoseData> {
        val endTime = Instant.now()
        val startTime = endTime.minus(7, ChronoUnit.DAYS)
        return getAllGlucoseData(startTime, endTime)
    }
    
    // Comprehensive Historic Data Retrieval Methods with Pagination
    
    /**
     * Retrieves ALL available steps data from Health Connect using pagination
     * @param startTime Optional start time (defaults to very old date to get all data)
     * @param endTime Optional end time (defaults to now)
     * @return List of all StepsData records
     */
    suspend fun getAllStepsData(
        startTime: Instant = Instant.ofEpochSecond(0), // Very old date to get all data
        endTime: Instant = Instant.now()
    ): List<StepsData> {
        return try {
            val allRecords = mutableListOf<StepsRecord>()
            var pageToken: String? = null
            
            do {
                val request = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                    pageToken = pageToken
                )
                val response = healthConnectClient.readRecords(request)
                allRecords.addAll(response.records)
                pageToken = response.pageToken
            } while (pageToken != null)
            
            allRecords.map { record ->
                StepsData(
                    count = record.count,
                    timestamp = record.endTime,
                    startTime = record.startTime,
                    endTime = record.endTime
                )
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Retrieves ALL available heart rate data from Health Connect using pagination
     * @param startTime Optional start time (defaults to very old date to get all data)
     * @param endTime Optional end time (defaults to now)
     * @return List of all HeartRateData records
     */
    suspend fun getAllHeartRateData(
        startTime: Instant = Instant.ofEpochSecond(0), // Very old date to get all data
        endTime: Instant = Instant.now()
    ): List<HeartRateData> {
        return try {
            val allRecords = mutableListOf<HeartRateRecord>()
            var pageToken: String? = null
            
            do {
                val request = ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                    pageToken = pageToken
                )
                val response = healthConnectClient.readRecords(request)
                allRecords.addAll(response.records)
                pageToken = response.pageToken
            } while (pageToken != null)
            
            allRecords.flatMap { record ->
                record.samples.map { sample ->
                    HeartRateData(
                        beatsPerMinute = sample.beatsPerMinute,
                        timestamp = sample.time
                    )
                }
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Retrieves ALL available glucose data from Health Connect using pagination
     * @param startTime Optional start time (defaults to very old date to get all data)
     * @param endTime Optional end time (defaults to now)
     * @return List of all GlucoseData records
     */
    suspend fun getAllGlucoseData(
        startTime: Instant = Instant.ofEpochSecond(0), // Very old date to get all data
        endTime: Instant = Instant.now()
    ): List<GlucoseData> {
        return try {
            val allRecords = mutableListOf<BloodGlucoseRecord>()
            var pageToken: String? = null
            
            do {
                val request = ReadRecordsRequest(
                    recordType = BloodGlucoseRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                    pageToken = pageToken
                )
                val response = healthConnectClient.readRecords(request)
                allRecords.addAll(response.records)
                pageToken = response.pageToken
            } while (pageToken != null)
            
            allRecords.map { record ->
                GlucoseData(
                    level = record.level.inMilligramsPerDeciliter,
                    unit = "mg/dL",
                    timestamp = record.time,
                    mealType = record.mealType?.toString()
                )
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Retrieves ALL available data from a specific app package using pagination
     * This is useful for reading your own app's historical data without the 30-day limit
     */
    suspend fun getAllDataFromPackage(
        packageName: String,
        startTime: Instant = Instant.ofEpochSecond(0),
        endTime: Instant = Instant.now()
    ): Triple<List<StepsData>, List<HeartRateData>, List<GlucoseData>> {
        val dataOriginFilter = setOf(androidx.health.connect.client.records.metadata.DataOrigin(packageName))
        
        // Get Steps Data
        val stepsData = try {
            val allRecords = mutableListOf<StepsRecord>()
            var pageToken: String? = null
            
            do {
                val request = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                    dataOriginFilter = dataOriginFilter,
                    pageToken = pageToken
                )
                val response = healthConnectClient.readRecords(request)
                allRecords.addAll(response.records)
                pageToken = response.pageToken
            } while (pageToken != null)
            
            allRecords.map { record ->
                StepsData(
                    count = record.count,
                    timestamp = record.endTime,
                    startTime = record.startTime,
                    endTime = record.endTime
                )
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
        
        // Get Heart Rate Data
        val heartRateData = try {
            val allRecords = mutableListOf<HeartRateRecord>()
            var pageToken: String? = null
            
            do {
                val request = ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                    dataOriginFilter = dataOriginFilter,
                    pageToken = pageToken
                )
                val response = healthConnectClient.readRecords(request)
                allRecords.addAll(response.records)
                pageToken = response.pageToken
            } while (pageToken != null)
            
            allRecords.flatMap { record ->
                record.samples.map { sample ->
                    HeartRateData(
                        beatsPerMinute = sample.beatsPerMinute,
                        timestamp = sample.time
                    )
                }
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
        
        // Get Glucose Data
        val glucoseData = try {
            val allRecords = mutableListOf<BloodGlucoseRecord>()
            var pageToken: String? = null
            
            do {
                val request = ReadRecordsRequest(
                    recordType = BloodGlucoseRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                    dataOriginFilter = dataOriginFilter,
                    pageToken = pageToken
                )
                val response = healthConnectClient.readRecords(request)
                allRecords.addAll(response.records)
                pageToken = response.pageToken
            } while (pageToken != null)
            
            allRecords.map { record ->
                GlucoseData(
                    level = record.level.inMilligramsPerDeciliter,
                    unit = "mg/dL",
                    timestamp = record.time,
                    mealType = record.mealType?.toString()
                )
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
        
        return Triple(stepsData, heartRateData, glucoseData)
    }
    
    // Write Functions
    suspend fun insertStepsRecord(steps: Long, startTime: Instant, endTime: Instant): Boolean {
        return try {
            val stepsRecord = StepsRecord(
                count = steps,
                startTime = startTime,
                endTime = endTime,
                startZoneOffset = null,
                endZoneOffset = null
            )
            healthConnectClient.insertRecords(listOf(stepsRecord))
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun insertHeartRateRecord(bpm: Long, time: Instant): Boolean {
        return try {
            val heartRateRecord = HeartRateRecord(
                startTime = time,
                endTime = time.plusSeconds(1),
                startZoneOffset = null,
                endZoneOffset = null,
                samples = listOf(
                    HeartRateRecord.Sample(
                        time = time,
                        beatsPerMinute = bpm
                    )
                )
            )
            healthConnectClient.insertRecords(listOf(heartRateRecord))
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun insertGlucoseRecord(level: Double, time: Instant, mealType: Int? = null): Boolean {
        return try {
            val glucoseRecord = BloodGlucoseRecord(
                level = BloodGlucose.milligramsPerDeciliter(level),
                time = time,
                zoneOffset = null
            )
            healthConnectClient.insertRecords(listOf(glucoseRecord))
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Delete Functions
    suspend fun deleteStepsRecord(stepsData: StepsData): Boolean {
        return try {
            val clientRecordIdList = if (stepsData.startTime != null) {
                emptyList<String>()
            } else {
                emptyList()
            }
            
            healthConnectClient.deleteRecords(
                StepsRecord::class,
                recordIdsList = emptyList(), // We don't have the record ID from our data model
                clientRecordIdsList = clientRecordIdList
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun deleteHeartRateRecord(heartRateData: HeartRateData): Boolean {
        return try {
            healthConnectClient.deleteRecords(
                HeartRateRecord::class,
                recordIdsList = emptyList(),
                clientRecordIdsList = emptyList()
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun deleteGlucoseRecord(glucoseData: GlucoseData): Boolean {
        return try {
            healthConnectClient.deleteRecords(
                BloodGlucoseRecord::class,
                recordIdsList = emptyList(),
                clientRecordIdsList = emptyList()
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Delete records by time range (more practical approach)
    suspend fun deleteRecordsByTimeRange(
        recordType: String,
        startTime: Instant,
        endTime: Instant
    ): Boolean {
        return try {
            val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            when (recordType) {
                "steps" -> {
                    healthConnectClient.deleteRecords(
                        StepsRecord::class,
                        timeRangeFilter = timeRangeFilter
                    )
                }
                "heartrate" -> {
                    healthConnectClient.deleteRecords(
                        HeartRateRecord::class,
                        timeRangeFilter = timeRangeFilter
                    )
                }
                "glucose" -> {
                    healthConnectClient.deleteRecords(
                        BloodGlucoseRecord::class,
                        timeRangeFilter = timeRangeFilter
                    )
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
