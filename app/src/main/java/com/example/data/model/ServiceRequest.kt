package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ServiceRequest(
    @DocumentId val id: String = "",
    val requestId: String = "",
    val journeyId: String = "",
    val userId: String = "",
    val tollId: String = "",
    val serviceCategory: String = "FOOD", // FUEL, EV_CHARGING, REPAIR, TYRE_PUNCTURE, ROADSIDE_ASSISTANCE, MEDICAL, FOOD, RESTROOM, WATER, PARKING
    val serviceName: String = "",
    val vehicleNumber: String = "",
    val notes: String = "",
    val status: String = "REQUESTED", // REQUESTED, CONFIRMED, COMPLETED, CANCELLED
    val estimatedCharge: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
