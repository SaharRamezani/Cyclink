package com.example.cyclink.helpers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import com.example.cyclink.team.LocationData
import org.json.JSONObject

class MQTTHelper(private val context: Context) {
    private var mqttClient: MqttClient? = null
    private val brokerUrl = "tcp://192.168.103.151:1883" // Replace with your MQTT broker URL
    private val clientId = "CyclinkApp_${System.currentTimeMillis()}"
    private val publishTopic = "cyclink/sensor_data"
    private val subscribeTopic = "sensor/howdy/data"

    companion object {
        private const val TAG = "MQTTHelper"
    }

    fun sendSensorDataToMqtt(sensorRecord: SensorRecord, userId: String) {
        try {
            // Create a proper serializable LocationData object
            val locationData = LocationData(
                latitude = sensorRecord.latitude,
                longitude = sensorRecord.longitude,
                userId = userId,
                timestamp = sensorRecord.timestamp
            )

            val locationTopic = "location/$userId"
            val locationMessage = Json.encodeToString(locationData)

            publishMessage(locationTopic, locationMessage) { success ->
                if (success) {
                    Log.d(TAG, "📡 Location data sent to MQTT: ${sensorRecord.latitude}, ${sensorRecord.longitude}")
                } else {
                    Log.e(TAG, "❌ Failed to send location data to MQTT")
                }
            }

            // Send complete sensor data
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

    fun publishMessage(topic: String, message: String, callback: (Boolean) -> Unit) {
        try {
            if (mqttClient?.isConnected == true) {
                mqttClient?.publish(topic, MqttMessage(message.toByteArray()))?.apply {
                    var actionCallback = object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            callback(true)
                        }

                        override fun onFailure(
                            asyncActionToken: IMqttToken?,
                            exception: Throwable?
                        ) {
                            Log.e(TAG, "Failed to publish message to $topic", exception)
                            callback(false)
                        }
                    }
                }
            } else {
                Log.w(TAG, "MQTT client not connected")
                callback(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in publishMessage", e)
            callback(false)
        }
    }

    fun connect(
        onConnected: () -> Unit = {},
        onError: (String) -> Unit = {},
        onSensorDataReceived: (SensorDataMessage) -> Unit = {}
    ) {
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
                            Log.d(TAG, "📨 Message content: $messageStr")

                            // Parse JSON message
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

            // Subscribe to sensor data topic
            mqttClient?.subscribe(subscribeTopic, 1)
            Log.d(TAG, "✅ Connected to MQTT broker and subscribed to $subscribeTopic")
            onConnected()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to MQTT broker", e)
            onError(e.message ?: "Unknown error")
        }
    }

    fun publishSensorData(data: MQTTSensorData) {
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