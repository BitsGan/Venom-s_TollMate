package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.EmergencyRequest
import com.example.data.model.Journey
import com.example.data.model.ServiceRequest
import com.example.data.model.TollPlaza
import com.example.data.model.Transaction
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TollMateRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- User Profile ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { it.toObject(UserProfile::class.java) }
            .catch { error ->
                handleFirestoreError(error as Exception, OperationType.GET, "users/$userId")
                emit(null)
            }

    suspend fun createOrUpdateProfileOnLogin(
        userId: String,
        email: String,
        displayName: String,
        defaultVehicle: String = ""
    ) {
        val userRef = db.collection("users").document(userId)
        try {
            val snapshot = userRef.get().await()
            if (!snapshot.exists()) {
                val data = hashMapOf<String, Any>(
                    "userId" to userId,
                    "email" to email,
                    "displayName" to displayName,
                    "vehicleNumber" to defaultVehicle,
                    "walletBalance" to 500.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(data, SetOptions.merge()).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path)
            throw e
        }
    }

    suspend fun updateVehicleNumber(vehicleNumber: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.set(
                mapOf(
                    "vehicleNumber" to vehicleNumber.uppercase(Locale.US).trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Journey Management (Stage 1 & Stage 2) ---

    fun generateUniqueJourneyId(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        val dateStr = dateFormat.format(Date())
        val randomStr = UUID.randomUUID().toString().replace("-", "").take(5).uppercase(Locale.US)
        return "JRN-$dateStr-$randomStr"
    }

    suspend fun createJourney(
        journeyId: String,
        vehicleNumber: String,
        startLocation: String,
        destination: String,
        startLat: Double? = null,
        startLng: Double? = null,
        destLat: Double? = null,
        destLng: Double? = null
    ): Result<Journey> {
        val uid = requireUserId()
        val normalizedVehicle = vehicleNumber.uppercase(Locale.US).replace(" ", "").replace("-", "").trim()
        val journeyDoc = db.collection("journeys").document(journeyId)

        val payload = hashMapOf<String, Any>(
            "journeyId" to journeyId,
            "userId" to uid,
            "vehicleNumber" to normalizedVehicle,
            "startLocation" to startLocation.trim(),
            "destination" to destination.trim(),
            "status" to "ACTIVE",
            "totalTollsDeducted" to 0.0,
            "tollsPassedCount" to 0,
            "servicesUsedCount" to 0,
            "totalDistanceKm" to calculateEstimatedDistance(startLocation, destination),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (startLat != null) payload["startLatitude"] = startLat
        if (startLng != null) payload["startLongitude"] = startLng
        if (destLat != null) payload["destLatitude"] = destLat
        if (destLng != null) payload["destLongitude"] = destLng

        return try {
            journeyDoc.set(payload).await()
            val created = Journey(
                id = journeyId,
                journeyId = journeyId,
                userId = uid,
                vehicleNumber = normalizedVehicle,
                startLocation = startLocation.trim(),
                destination = destination.trim(),
                startLatitude = startLat,
                startLongitude = startLng,
                destLatitude = destLat,
                destLongitude = destLng,
                status = "ACTIVE"
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, journeyDoc.path)
            Result.failure(e)
        }
    }

    fun observeActiveJourney(userId: String): Flow<Journey?> =
        db.collection("journeys")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "ACTIVE")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.firstOrNull()?.toObject(Journey::class.java)
            }
            .catch { error ->
                handleFirestoreError(error as Exception, OperationType.LIST, "journeys")
                emit(null)
            }

    fun observeUserJourneys(userId: String): Flow<List<Journey>> =
        db.collection("journeys")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Journey::class.java).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                handleFirestoreError(error as Exception, OperationType.LIST, "journeys")
                emit(emptyList())
            }

    suspend fun endJourney(journeyId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("journeys").document(journeyId)
        return try {
            docRef.update(
                mapOf(
                    "status" to "COMPLETED",
                    "endTime" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Toll Plazas & Live Toll Tracking (Stage 3 & 4) ---

    fun observeTolls(): Flow<List<TollPlaza>> =
        db.collection("tolls")
            .snapshots()
            .map { snapshot ->
                val list = snapshot.toObjects(TollPlaza::class.java)
                if (list.isEmpty()) getBundledTolls() else list
            }
            .catch { error ->
                handleFirestoreError(error as Exception, OperationType.LIST, "tolls")
                emit(getBundledTolls())
            }

    fun getBundledTolls(): List<TollPlaza> = listOf(
        TollPlaza(
            id = "TOLL-MH-01",
            tollId = "TOLL-MH-01",
            name = "Khalapur Toll Plaza",
            highway = "Yashwantrao Chavan Mumbai-Pune Expressway (NE-1)",
            latitude = 18.8472,
            longitude = 73.2797,
            standardRate = 120.0,
            commercialRate = 280.0,
            availableLanes = 16,
            emergencyContact = "1033 / 9822498224",
            facilities = listOf("Fuel Station", "EV Supercharger", "Food Court / McD", "Restrooms", "Tyre Puncture Shop", "Ambulance Station", "Parking")
        ),
        TollPlaza(
            id = "TOLL-MH-02",
            tollId = "TOLL-MH-02",
            name = "Khedshivapur Toll Plaza",
            highway = "Pune-Bangalore Highway (NH-48)",
            latitude = 18.3245,
            longitude = 73.8654,
            standardRate = 115.0,
            commercialRate = 240.0,
            availableLanes = 12,
            emergencyContact = "1033",
            facilities = listOf("Fuel Station", "Restrooms", "Drinking Water", "Vehicle Repair", "Food Stalls", "First Aid")
        ),
        TollPlaza(
            id = "TOLL-MH-03",
            tollId = "TOLL-MH-03",
            name = "Talegaon Toll Plaza",
            highway = "Mumbai-Pune Expressway (NE-1)",
            latitude = 18.7362,
            longitude = 73.6823,
            standardRate = 160.0,
            commercialRate = 340.0,
            availableLanes = 14,
            emergencyContact = "1033",
            facilities = listOf("EV Charging", "Food Court", "Drinking Water", "Restrooms", "Tow Crane")
        ),
        TollPlaza(
            id = "TOLL-DL-01",
            tollId = "TOLL-DL-01",
            name = "Kherki Daula Toll Plaza",
            highway = "Delhi-Jaipur Expressway (NH-48)",
            latitude = 28.3975,
            longitude = 76.9942,
            standardRate = 95.0,
            commercialRate = 210.0,
            availableLanes = 20,
            emergencyContact = "1033",
            facilities = listOf("Fuel Station", "EV Charging", "Medical Aid", "Restrooms", "Fastag Helpdesk")
        ),
        TollPlaza(
            id = "TOLL-KA-01",
            tollId = "TOLL-KA-01",
            name = "Attibele Toll Plaza",
            highway = "Bangalore-Hosur Highway (NH-44)",
            latitude = 12.7834,
            longitude = 77.7712,
            standardRate = 60.0,
            commercialRate = 140.0,
            availableLanes = 10,
            emergencyContact = "1033",
            facilities = listOf("Restrooms", "Drinking Water", "Tyre Assistance", "Emergency Roadside")
        )
    )

    // --- Service Requests (Stage 4) ---

    suspend fun createServiceRequest(
        journeyId: String,
        serviceCategory: String,
        serviceName: String,
        tollId: String,
        vehicleNumber: String,
        notes: String = "",
        estimatedCharge: Double = 0.0
    ): Result<String> {
        val uid = requireUserId()
        val requestId = "REQ-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase(Locale.US)
        val docRef = db.collection("service_requests").document(requestId)

        val payload = hashMapOf<String, Any>(
            "requestId" to requestId,
            "journeyId" to journeyId,
            "userId" to uid,
            "serviceCategory" to serviceCategory,
            "serviceName" to serviceName,
            "tollId" to tollId,
            "vehicleNumber" to vehicleNumber,
            "notes" to notes,
            "status" to "REQUESTED",
            "estimatedCharge" to estimatedCharge,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            // Increment servicesUsedCount on the journey document
            val journeyRef = db.collection("journeys").document(journeyId)
            journeyRef.update(
                mapOf(
                    "servicesUsedCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(requestId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeJourneyServices(journeyId: String): Flow<List<ServiceRequest>> {
        val uid = try { requireUserId() } catch (e: Exception) { "" }
        if (uid.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return db.collection("service_requests")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ServiceRequest::class.java).filter { it.journeyId == journeyId }
            }
            .catch { error ->
                handleFirestoreError(error as Exception, OperationType.LIST, "service_requests")
                emit(emptyList())
            }
    }

    // --- Digital Wallet & Transactions ---

    fun observeTransactions(userId: String): Flow<List<Transaction>> =
        db.collection("transactions")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Transaction::class.java).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                handleFirestoreError(error as Exception, OperationType.LIST, "transactions")
                emit(emptyList())
            }

    suspend fun rechargeWallet(amount: Double): Result<Unit> {
        val uid = requireUserId()
        val txId = "TXN-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase(Locale.US)
        val txDoc = db.collection("transactions").document(txId)
        val userDoc = db.collection("users").document(uid)

        val txData = hashMapOf<String, Any>(
            "transactionId" to txId,
            "userId" to uid,
            "type" to "WALLET_TOPUP",
            "amount" to amount,
            "title" to "Wallet Top-up",
            "description" to "Added ₹${amount.toInt()} to in-app toll wallet",
            "disclaimer" to "In-app simulated toll deduction. Not verified FASTag or official NHAI toll payment.",
            "status" to "SUCCESS",
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            txDoc.set(txData).await()
            userDoc.set(
                mapOf(
                    "walletBalance" to FieldValue.increment(amount),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, txDoc.path)
            Result.failure(e)
        }
    }

    suspend fun recordSimulatedTollDeduction(
        journeyId: String,
        tollName: String,
        amount: Double
    ): Result<Unit> {
        val uid = requireUserId()
        val txId = "TOLL-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase(Locale.US)
        val txDoc = db.collection("transactions").document(txId)
        val userDoc = db.collection("users").document(uid)
        val journeyDoc = db.collection("journeys").document(journeyId)

        val txData = hashMapOf<String, Any>(
            "transactionId" to txId,
            "userId" to uid,
            "journeyId" to journeyId,
            "type" to "TOLL_DEDUCTION",
            "amount" to amount,
            "title" to "Toll: $tollName",
            "description" to "Simulated in-app toll plaza pass",
            "disclaimer" to "In-app simulated toll deduction. Not verified FASTag or official NHAI toll payment.",
            "status" to "SUCCESS",
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            // 1. Ensure user profile exists
            try {
                val userSnap = userDoc.get().await()
                if (!userSnap.exists()) {
                    val email = auth.currentUser?.email ?: ""
                    val name = auth.currentUser?.displayName ?: "Traveler"
                    createOrUpdateProfileOnLogin(uid, email, name)
                }
            } catch (e: Exception) {
                Log.w("TollMateRepo", "User check warning: ${e.message}")
            }

            // 2. Record transaction document
            txDoc.set(txData).await()

            // 3. Deduct from wallet balance safely using SetOptions.merge()
            userDoc.set(
                mapOf(
                    "walletBalance" to FieldValue.increment(-amount),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()

            // 4. Safely update active journey if it exists and belongs to user
            if (journeyId.isNotBlank() && !journeyId.startsWith("SIM-")) {
                try {
                    val jSnap = journeyDoc.get().await()
                    if (jSnap.exists() && jSnap.getString("userId") == uid) {
                        journeyDoc.update(
                            mapOf(
                                "totalTollsDeducted" to FieldValue.increment(amount),
                                "tollsPassedCount" to FieldValue.increment(1),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                } catch (e: Exception) {
                    Log.w("TollMateRepo", "Non-fatal journey toll update warning: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("TollMateRepo", "Error recording toll deduction: ${e.message}", e)
            handleFirestoreError(e, OperationType.WRITE, txDoc.path)
            Result.failure(e)
        }
    }

    suspend fun updatePhonePeNumber(phonePeNumber: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            docRef.set(
                mapOf(
                    "phonePeNumber" to phonePeNumber.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordPhonePeTransfer(
        amount: Double,
        mobileNumber: String,
        note: String,
        deductFromInAppWallet: Boolean = true
    ): Result<String> {
        val uid = requireUserId()
        val txId = "PHONEPE-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase(Locale.US)
        val txDoc = db.collection("transactions").document(txId)
        val userDoc = db.collection("users").document(uid)
        val cleanMobile = mobileNumber.trim()
        val vpa = if (cleanMobile.contains("@")) cleanMobile else "${cleanMobile.filter { it.isDigit() }}@ybl"

        val txData = hashMapOf<String, Any>(
            "transactionId" to txId,
            "userId" to uid,
            "type" to "PHONEPE_TRANSFER",
            "amount" to amount,
            "title" to "PhonePe Transfer to $cleanMobile",
            "description" to note.ifBlank { "Direct UPI transfer to mobile $cleanMobile" },
            "disclaimer" to "Direct PhonePe UPI transfer initiated to mobile number via NPCI UPI Intent.",
            "phonePeNumber" to cleanMobile,
            "upiId" to vpa,
            "recipient" to cleanMobile,
            "status" to "SUCCESS",
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            txDoc.set(txData).await()
            val userUpdates = hashMapOf<String, Any>(
                "phonePeNumber" to cleanMobile,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (deductFromInAppWallet) {
                userUpdates["walletBalance"] = FieldValue.increment(-amount)
            }
            userDoc.set(userUpdates, SetOptions.merge()).await()
            Result.success(txId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("TollMateRepo", "Error recording PhonePe transfer: ${e.message}", e)
            handleFirestoreError(e, OperationType.WRITE, txDoc.path)
            Result.failure(e)
        }
    }

    suspend fun recordPhonePeTollPayment(
        journeyId: String,
        tollName: String,
        amount: Double,
        phonePeMobile: String
    ): Result<String> {
        val uid = requireUserId()
        val txId = "PHONEPE-TOLL-" + UUID.randomUUID().toString().replace("-", "").take(6).uppercase(Locale.US)
        val txDoc = db.collection("transactions").document(txId)
        val journeyDoc = db.collection("journeys").document(journeyId)

        val txData = hashMapOf<String, Any>(
            "transactionId" to txId,
            "userId" to uid,
            "journeyId" to journeyId,
            "type" to "PHONEPE_TRANSFER",
            "amount" to amount,
            "title" to "PhonePe Toll: $tollName",
            "description" to "Direct PhonePe UPI toll payment for $tollName",
            "disclaimer" to "Direct PhonePe toll transfer initiated via NPCI UPI Intent.",
            "phonePeNumber" to phonePeMobile.trim(),
            "status" to "SUCCESS",
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            txDoc.set(txData).await()

            if (journeyId.isNotBlank() && !journeyId.startsWith("SIM-")) {
                try {
                    val jSnap = journeyDoc.get().await()
                    if (jSnap.exists() && jSnap.getString("userId") == uid) {
                        journeyDoc.update(
                            mapOf(
                                "totalTollsDeducted" to FieldValue.increment(amount),
                                "tollsPassedCount" to FieldValue.increment(1),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                } catch (e: Exception) {
                    Log.w("TollMateRepo", "Non-fatal journey update: ${e.message}")
                }
            }
            Result.success(txId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("TollMateRepo", "Error recording PhonePe toll payment: ${e.message}", e)
            handleFirestoreError(e, OperationType.WRITE, txDoc.path)
            Result.failure(e)
        }
    }


    // --- Emergency Roadside Assistance (Stage 5) ---

    suspend fun reportEmergency(
        journeyId: String,
        vehicleNumber: String,
        emergencyType: String,
        locationDescription: String,
        notes: String = "",
        lat: Double? = null,
        lng: Double? = null
    ): Result<String> {
        val uid = requireUserId()
        val emergencyId = "SOS-" + UUID.randomUUID().toString().replace("-", "").take(6).uppercase(Locale.US)
        val docRef = db.collection("emergency_requests").document(emergencyId)

        val payload = hashMapOf<String, Any>(
            "emergencyId" to emergencyId,
            "journeyId" to journeyId,
            "userId" to uid,
            "vehicleNumber" to vehicleNumber,
            "emergencyType" to emergencyType,
            "locationDescription" to locationDescription,
            "status" to "REPORTED",
            "notes" to notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (lat != null) payload["latitude"] = lat
        if (lng != null) payload["longitude"] = lng

        return try {
            docRef.set(payload).await()
            Result.success(emergencyId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    private fun calculateEstimatedDistance(start: String, dest: String): Double {
        val combined = (start.lowercase() + " " + dest.lowercase())
        return when {
            combined.contains("pune") && combined.contains("mumbai") -> 148.0
            combined.contains("delhi") && combined.contains("jaipur") -> 268.0
            combined.contains("bangalore") && combined.contains("mysore") -> 144.0
            combined.contains("chennai") && combined.contains("bangalore") -> 346.0
            combined.contains("hyderabad") && combined.contains("vijayawada") -> 275.0
            else -> 180.0
        }
    }
}
