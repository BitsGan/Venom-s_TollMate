package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class TollPlaza(
    @DocumentId val id: String = "",
    val tollId: String = "",
    val name: String = "",
    val highway: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val standardRate: Double = 100.0,
    val commercialRate: Double = 220.0,
    val availableLanes: Int = 8,
    val emergencyContact: String = "1033",
    val facilities: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)
