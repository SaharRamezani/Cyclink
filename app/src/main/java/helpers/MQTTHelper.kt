package com.example.cyclink.helpers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.json.JSONObject

class MQTTHelper(private val context: Context) {
    private var mqttClient: MqttClient? = null
    private val brokerUrl = "tcp://192.168.103.151:1883"
    private val clientId = "CyclinkApp_${System.currentTimeMillis()}"

    companion object {
        private const val TAG = "MQTTHelper"
    }

    private fun getUserTopic(userId: String): String {
        return "sensorData/$userId"
    }

    private fun getSubscribeTopic(): String {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
        return "sensor/howdy/$userId"
    }

    fun sendSensorDataToMqtt(sensorRecord: SensorRecord, userId: String) {
        try {
            val sensorMessage = Json.encodeToString(sensorRecord.copy(userId = userId))
            val userTopic = getUserTopic(userId)

            publishMessage(userTopic, sensorMessage) { success ->
                if (success) {
                    Log.d(TAG, "✅ Sensor data sent to MQTT topic: $userTopic")
                } else {
                    Log.e(TAG, "❌ Failed to send sensor data to MQTT")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending sensor data to MQTT: ${e.message}")
        }
    }

    fun connectAndSubscribeToUser(
        userId: String,
        onConnected: () -> Unit,
        onLocationUpdate: (SensorRecord) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val userTopic = getUserTopic(userId)
            connect(
                onConnected = {
                    subscribe(userTopic) { topic, message ->
                        try {
                            val json = JSONObject(message)
                            val sensorRecord = parseSensorRecord(json)
                            onLocationUpdate(sensorRecord)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing sensor data: ${e.message}")
                        }
                    }
                    onConnected()
                },
                onError = onError,
                onSensorDataReceived = { _ -> } // Not used in this context
            )
        } catch (e: Exception) {
            onError("Failed to connect: ${e.message}")
        }
    }

    private fun parseSensorRecord(json: JSONObject): SensorRecord {
        return SensorRecord(
            timestamp = System.currentTimeMillis(),
            userId = json.optString("userId", ""),
            sessionId = json.optString("sessionId", ""),
            heartRate = json.optDouble("heartRate").takeIf { !it.isNaN() },
            breathFrequency = json.optDouble("breathFrequency").takeIf { !it.isNaN() },
            hrv = json.optDouble("hrv").takeIf { !it.isNaN() },
            intensity = json.optDouble("intensity").takeIf { !it.isNaN() },
            accelerationX = json.optDouble("accelerationX").takeIf { !it.isNaN() },
            accelerationY = json.optDouble("accelerationY").takeIf { !it.isNaN() },
            accelerationZ = json.optDouble("accelerationZ").takeIf { !it.isNaN() },
            ecg = parseDoubleArray(json.optJSONArray("ecg")),
            respiration = parseDoubleArray(json.optJSONArray("respiration")),
            r2rIntervals = parseDoubleArray(json.optJSONArray("r2rIntervals")),
            phoneAccelerationX = json.optDouble("phoneAccelerationX").takeIf { !it.isNaN() },
            phoneAccelerationY = json.optDouble("phoneAccelerationY").takeIf { !it.isNaN() },
            phoneAccelerationZ = json.optDouble("phoneAccelerationZ").takeIf { !it.isNaN() },
            latitude = json.optDouble("latitude"),
            longitude = json.optDouble("longitude"),
            altitude = json.optDouble("altitude").takeIf { !it.isNaN() },
            gpsAccuracy = json.optDouble("gpsAccuracy").takeIf { !it.isNaN() }?.toFloat(),
            gpsSpeed = json.optDouble("gpsSpeed").takeIf { !it.isNaN() }?.toFloat(),
            gpsBearing = json.optDouble("gpsBearing").takeIf { !it.isNaN() }?.toFloat()
        )
    }

    fun publishMessage(topic: String, message: String, callback: (Boolean) -> Unit) {
        try {
            mqttClient?.let { client ->
                if (client.isConnected) {
                    val mqttMessage = MqttMessage(message.toByteArray())
                    mqttMessage.qos = 1
                    client.publish(topic, mqttMessage)
                    callback(true)
                } else {
                    Log.w(TAG, "MQTT client not connected")
                    callback(false)
                }
            } ?: run {
                Log.w(TAG, "MQTT client is null")
                callback(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error publishing message: ${e.message}")
            callback(false)
        }
    }

    private fun subscribe(topic: String, onMessageReceived: (String, String) -> Unit) {
        try {
            mqttClient?.subscribe(topic) { topic, message ->
                onMessageReceived(topic, String(message.payload))
            }
            Log.d(TAG, "✅ Subscribed to topic: $topic")
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to topic $topic: ${e.message}")
        }
    }

    fun connect(
        onConnected: () -> Unit,
        onError: (String) -> Unit,
        onSensorDataReceived: (SensorDataMessage) -> Unit
    ) {
        try {
            mqttClient = MqttClient(brokerUrl, clientId, MemoryPersistence())

            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 30
            }

            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.w(TAG, "MQTT connection lost: ${cause?.message}")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    message?.let { msg ->
                        val messageStr = String(msg.payload)
                        Log.d(TAG, "📨 Message received on $topic: $messageStr")

                        try {
                            val json = JSONObject(messageStr)
                            val sensorData = SensorDataMessage(
                                measureType = json.optString("measureType", ""),
                                value = parseDoubleArray(json.optJSONArray("value")),
                                userId = json.optInt("userId", 0), // Changed from optString to optInt
                                date = json.optLong("date", System.currentTimeMillis()) // Changed from optString to optLong
                            )
                            onSensorDataReceived(sensorData)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing sensor data: ${e.message}")
                        }
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d(TAG, "Message delivery complete")
                }
            })

            mqttClient?.connect(options)

            // Subscribe to personal sensor topic
            val personalTopic = getSubscribeTopic()
            mqttClient?.subscribe(personalTopic)

            Log.d(TAG, "✅ Connected to MQTT broker and subscribed to $personalTopic")
            onConnected()

        } catch (e: Exception) {
            Log.e(TAG, "❌ MQTT connection failed: ${e.message}")
            onError(e.message ?: "Unknown connection error")
        }
    }

    private fun parseDoubleArray(jsonArray: org.json.JSONArray?): List<Double> {
        return jsonArray?.let { array ->
            (0 until array.length()).map { array.getDouble(it) }
        } ?: emptyList()
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
            mqttClient?.close()
            Log.d(TAG, "Disconnected from MQTT broker")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from MQTT: ${e.message}")
        }
    }
}