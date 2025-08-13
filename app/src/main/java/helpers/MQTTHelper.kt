package com.example.cyclink.helpers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import com.example.cyclink.helpers.SensorDataMessage
import kotlinx.serialization.decodeFromString
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
                    Log.e(TAG, "Connection lost", cause)
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    Log.d(TAG, "📡 Message received on topic: $topic")
                    if (topic == subscribeTopic && message != null) {
                        try {
                            val jsonString = String(message.payload)
                            Log.d(TAG, "📡 Raw message: $jsonString")

                            val jsonObject = JSONObject(jsonString)
                            val sensorData = SensorDataMessage(
                                date = jsonObject.getLong("date"),
                                value = jsonObject.getJSONArray("value").let { array ->
                                    (0 until array.length()).map { array.getDouble(it) }
                                },
                                userId = jsonObject.getInt("userId"),
                                measureType = jsonObject.getString("measureType")
                            )

                            Log.d(TAG, "📊 Parsed sensor data: ${sensorData.measureType} = ${sensorData.value.size} values")
                            onSensorDataReceived(sensorData)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse sensor data", e)
                        }
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d(TAG, "Message delivered")
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