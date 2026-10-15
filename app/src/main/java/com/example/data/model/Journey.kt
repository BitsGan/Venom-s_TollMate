package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Journey(
    @DocumentId val id: String = "",
    val journeyId: String = "",
    val userId: String = "",
    val vehicleNumber: String = "",
    val startLocation: String = "",
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val destination: String = "",
    val destLatitude: Double? = null,
    val destLongitude: Double? = null,
    val status: String = "ACTIVE", // ACTIVE, COMPLETED, CANCELLED
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val totalTollsDeducted: Double = 0.0,
    val tollsPassedCount: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val servicesUsedCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
