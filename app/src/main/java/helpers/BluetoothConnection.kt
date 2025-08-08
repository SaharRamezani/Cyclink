package com.example.cyclink.helpers

import android.bluetooth.*
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.util.*

class BluetoothConnection(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var serverSocket: BluetoothServerSocket? = null
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // UUID for SPP (Serial Port Profile) - matches what your laptop sends
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    companion object {
        private const val TAG = "BluetoothServer"
        private const val SERVICE_NAME = "IoTDataReceiver"
    }

    fun startServer(onMessageReceived: (SensorData) -> Unit) {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported")
            return
        }

        scope.launch {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                    SERVICE_NAME, SPP_UUID
                )
                isRunning = true
                Log.d(TAG, "Bluetooth server started, waiting for connections...")

                while (isRunning) {
                    try {
                        val socket = serverSocket?.accept()
                        socket?.let {
                            Log.d(TAG, "Connection accepted from ${it.remoteDevice.name}")
                            handleConnection(it, onMessageReceived)
                        }
                    } catch (e: IOException) {
                        if (isRunning) {
                            Log.e(TAG, "Server socket accept failed", e)
                        }
                        break
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to start server", e)
            }
        }
    }

    private fun handleConnection(
        socket: BluetoothSocket,
        onMessageReceived: (SensorData) -> Unit
    ) {
        scope.launch {
            val inputStream: InputStream = socket.inputStream
            val buffer = ByteArray(1024)

            try {
                while (isRunning) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val message = String(buffer, 0, bytesRead).trim()
                        Log.d(TAG, "Received: $message")

                        // Parse the JSON message
                        try {
                            val SensorData = parseSensorData(message)
                            withContext(Dispatchers.Main) {
                                onMessageReceived(SensorData)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse message: $message", e)
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Connection lost", e)
            } finally {
                socket.close()
            }
        }
    }

    private fun parseSensorData(jsonString: String): SensorData {
        // Using basic JSON parsing (you might want to use Gson or kotlinx.serialization)
        val json = jsonString.trim()

        // Simple JSON parsing - for production use Gson or kotlinx.serialization
        val dateRegex = "\"date\":(\\d+)".toRegex()
        val userIdRegex = "\"userId\":(\\d+)".toRegex()
        val measureTypeRegex = "\"measureType\":\"([^\"]+)\"".toRegex()
        val valueRegex = "\"value\":\\[([^\\]]+)\\]".toRegex()

        val date = dateRegex.find(json)?.groupValues?.get(1)?.toLong() ?: 0L
        val userId = userIdRegex.find(json)?.groupValues?.get(1)?.toInt() ?: 0
        val measureType = measureTypeRegex.find(json)?.groupValues?.get(1) ?: ""
        val valueString = valueRegex.find(json)?.groupValues?.get(1) ?: ""

        val values = valueString.split(",").map { it.trim().toDouble() }

        return SensorData(date, values, userId, measureType)
    }

    fun stopServer() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to close server socket", e)
        }
        scope.cancel()
    }
}