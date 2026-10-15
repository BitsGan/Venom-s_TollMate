package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Transaction(
    @DocumentId val id: String = "",
    val transactionId: String = "",
    val userId: String = "",
    val journeyId: String = "",
    val type: String = "WALLET_TOPUP", // WALLET_TOPUP, TOLL_DEDUCTION, SERVICE_PAYMENT, PHONEPE_TRANSFER, UPI_TRANSFER
    val amount: Double = 0.0,
    val title: String = "",
    val description: String = "",
    val disclaimer: String = "In-app simulated toll deduction. Not verified FASTag or official NHAI toll payment.",
    val status: String = "SUCCESS",
    val phonePeNumber: String = "",
    val upiId: String = "",
    val recipient: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
