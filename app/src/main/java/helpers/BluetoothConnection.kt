package com.example.cyclink.helpers

import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.example.cyclink.R
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
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

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
                serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
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
            val input = socket.inputStream
            val buf = ByteArray(4096)
            val sb = StringBuilder()
            Log.d(TAG, "📡 Handling connection from ${socket.remoteDevice.name}/${socket.remoteDevice.address}")
            try {
                while (isRunning) {
                    val n = try { input.read(buf) } catch (_: IOException) { -1 }
                    if (n <= 0) break
                    sb.append(String(buf, 0, n))
                    var nl = sb.indexOf("\n")
                    while (nl != -1) {
                        val line = sb.substring(0, nl).trim()
                        sb.delete(0, nl + 1)
                        if (line.isNotEmpty()) {
                            Log.d(TAG, "⬅️ $line")
                            try {
                                val sd = parseSensorData(line)
                                withContext(Dispatchers.Main) { onMessageReceived(sd) }
                            } catch (e: Exception) {
                                Log.e(TAG, "🛑 Bad JSON, skipping: $line", e)
                            }
                        }
                        nl = sb.indexOf("\n")
                    }
                }
            } finally {
                try { socket.close() } catch (_: IOException) {}
                Log.d(TAG, "🔚 Connection closed ${socket.remoteDevice.address}")
            }
        }
    }

    private fun parseSensorData(json: String): SensorData {
        fun rx(p: String) = Regex(p, RegexOption.IGNORE_CASE)
        val date = rx("\"date\"\\s*:\\s*(\\d+)").find(json)?.groupValues?.get(1)?.toLong() ?: 0L
        val userId = rx("\"userId\"\\s*:\\s*(\\d+)").find(json)?.groupValues?.get(1)?.toInt() ?: 0
        val measureType = rx("\"measureType\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)?.lowercase() ?: ""

        val arr = rx("\"value\"\\s*:\\s*\\[([^\\]]*)\\]").find(json)?.groupValues?.get(1)
        val scalar = rx("\"value\"\\s*:\\s*([-+]?[0-9]*\\.?[0-9]+)").find(json)?.groupValues?.get(1)

        val values: List<Double> = when {
            arr != null -> arr.split(",").mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() }?.toDoubleOrNull() }
            scalar != null -> listOfNotNull(scalar.toDoubleOrNull())
            else -> emptyList()
        }
        if (values.isEmpty()) throw IllegalArgumentException("No numeric values")

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