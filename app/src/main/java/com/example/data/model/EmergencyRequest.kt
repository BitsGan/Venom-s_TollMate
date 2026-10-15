package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class EmergencyRequest(
    @DocumentId val id: String = "",
    val emergencyId: String = "",
    val journeyId: String = "",
    val userId: String = "",
    val vehicleNumber: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationDescription: String = "",
    val emergencyType: String = "ACCIDENT", // ACCIDENT, BREAKDOWN, MEDICAL, CRIME_SAFETY, OTHER
    val status: String = "REPORTED", // REPORTED, DISPATCHED, RESOLVED
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
