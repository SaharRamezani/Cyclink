package com.example.cyclink.helpers

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class FirestoreHelper {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "FirestoreHelper"
        private const val COLLECTION_SENSOR_DATA = "sensor_records"
    }

    fun saveSensorRecord(
        sensorRecord: SensorRecord,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "No authenticated user found")
            onError(Exception("No authenticated user"))
            return
        }

        val recordWithUserId = sensorRecord.copy(userId = currentUser.uid)

        db.collection(COLLECTION_SENSOR_DATA)
            .add(recordWithUserId)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "✅ Sensor record saved with ID: ${documentReference.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error saving sensor record", e)
                onError(e)
            }
    }

    fun getSensorRecordsForSession(
        sessionId: String,
        onSuccess: (List<SensorRecord>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onError(Exception("No authenticated user"))
            return
        }

        db.collection(COLLECTION_SENSOR_DATA)
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("sessionId", sessionId)
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { documents ->
                val records = documents.mapNotNull { doc ->
                    try {
                        doc.toObject(SensorRecord::class.java)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse sensor record", e)
                        null
                    }
                }
                onSuccess(records)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting sensor records", e)
                onError(e)
            }
    }
}