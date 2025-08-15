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
    private val publishTopic = "cyclink/sensor_data"
    private val subscribeTopic = "sensor/howdy/data"

    companion object {
        private const val TAG = "MQTTHelper"
    }

    fun sendSensorDataToMqtt(sensorRecord: SensorRecord, userId: String) {
        try {
            val sensorTopic = "sensorData/$userId"
            val sensorMessage = Json.encodeToString(sensorRecord.copy(userId = userId))

            publishMessage(sensorTopic, sensorMessage) { success ->
                if (success) {
                    Log.d(TAG, "📡 Complete sensor data sent to MQTT")
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
        onConnected: () -> Unit = {},
        onLocationUpdate: (SensorRecord) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                mqttClient = MqttClient(brokerUrl, "${clientId}_$userId", MemoryPersistence())

                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 30
                    keepAliveInterval = 60
                }

                mqttClient?.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        Log.w(TAG, "MQTT connection lost for user $userId", cause)
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        try {
                            message?.let { msg ->
                                val messageStr = String(msg.payload)
                                Log.d(TAG, "📨 Location data received for user $userId on topic: $topic")

                                val sensorRecord = Json.decodeFromString<SensorRecord>(messageStr)
                                if (sensorRecord.userId == userId) {
                                    onLocationUpdate(sensorRecord)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing location message for user $userId", e)
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        Log.d(TAG, "Message delivery complete for user $userId")
                    }
                })

                // Connect synchronously
                mqttClient?.connect(options)

                // Subscribe after successful connection
                val userTopic = "sensorData/$userId"
                mqttClient?.subscribe(userTopic, 1)
                Log.d(TAG, "✅ Connected to MQTT and subscribed to $userTopic")

                withContext(Dispatchers.Main) {
                    onConnected()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to MQTT for user $userId", e)
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun publishMessage(topic: String, message: String, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (mqttClient?.isConnected == true) {
                    val mqttMessage = MqttMessage(message.toByteArray())
                    mqttMessage.qos = 1

                    mqttClient?.publish(topic, mqttMessage)
                    Log.d(TAG, "✅ Successfully published message to $topic")

                    withContext(Dispatchers.Main) {
                        callback(true)
                    }
                } else {
                    Log.w(TAG, "MQTT client not connected")
                    withContext(Dispatchers.Main) {
                        callback(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in publishMessage", e)
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }

    fun connect(
        onConnected: () -> Unit = {},
        onError: (String) -> Unit = {},
        onSensorDataReceived: (SensorDataMessage) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                mqttClient = MqttClient(brokerUrl, clientId, MemoryPersistence())

                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 30
                    keepAliveInterval = 60
                }

                mqttClient?.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        Log.w(TAG, "MQTT connection lost", cause)
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        try {
                            message?.let { msg ->
                                val messageStr = String(msg.payload)
                                Log.d(TAG, "📨 MQTT message received on topic: $topic")

                                val jsonObject = JSONObject(messageStr)
                                if (jsonObject.has("measureType") &&
                                    jsonObject.has("value") &&
                                    jsonObject.has("userId")) {

                                    val sensorData = Json.decodeFromString<SensorDataMessage>(messageStr)
                                    onSensorDataReceived(sensorData)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing MQTT message", e)
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        Log.d(TAG, "Message delivery complete")
                    }
                })

                mqttClient?.connect(options)
                mqttClient?.subscribe(subscribeTopic, 1)
                Log.d(TAG, "✅ Connected to MQTT broker and subscribed to $subscribeTopic")

                withContext(Dispatchers.Main) {
                    onConnected()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to MQTT broker", e)
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun publishSensorData(data: MQTTSensorData) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (mqttClient?.isConnected == true) {
                    val jsonString = Json.encodeToString(data)
                    val message = MqttMessage(jsonString.toByteArray())
                    message.qos = 1

                    mqttClient?.publish(publishTopic, message)
                    Log.d(TAG, "Published sensor data: $jsonString")
                } else {
                    Log.w(TAG, "MQTT client not connected")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to publish sensor data", e)
            }
        }
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
            mqttClient?.close()
            Log.d(TAG, "Disconnected from MQTT broker")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from MQTT", e)
        }
    }
}