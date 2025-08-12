package com.example.cyclink.helpers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class MQTTHelper(private val context: Context) {
    private var mqttClient: MqttClient? = null
    private val brokerUrl = "tcp://raspberrypi.local:1883"
    private val clientId = "CyclinkApp_${System.currentTimeMillis()}"
    private val topic = "cyclink/sensor_data"

    companion object {
        private const val TAG = "MQTTHelper"
    }

    fun connect(onConnected: () -> Unit = {}, onError: (String) -> Unit = {}) {
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
                    // Handle incoming messages if needed
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d(TAG, "Message delivered")
                }
            })

            mqttClient?.connect(options)
            Log.d(TAG, "Connected to MQTT broker")
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

                mqttClient?.publish(topic, message)
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