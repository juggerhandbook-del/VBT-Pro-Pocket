package com.vbt.vbtpocket.engine

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@SuppressLint("MissingPermission")
class VbtBleManager(private val context: Context) {

    private val SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val RX_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    private val TX_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner = bluetoothAdapter?.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var isScanning = false

    // Callbacks para el ViewModel
    var onConnectionStateChange: ((Boolean) -> Unit)? = null
    var onDataReceived: ((List<IMUFrame>) -> Unit)? = null
    var onStatusMessage: ((String) -> Unit)? = null // Para avisar a la UI ("Buscando...", "Conectado")

    // --- LÓGICA DE AUTO-CONNECT ---
    fun scanAndConnect() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            onStatusMessage?.invoke("Error: Bluetooth apagado")
            return
        }
        if (scanner == null) return

        onStatusMessage?.invoke("Buscando VBT_POCKET...")
        isScanning = true
        scanner.startScan(scanCallback)

        // Timeout de seguridad: Detener escaneo a los 10 segundos si no lo encuentra
        Handler(Looper.getMainLooper()).postDelayed({
            if (isScanning) {
                scanner.stopScan(scanCallback)
                isScanning = false
                if (bluetoothGatt == null) onStatusMessage?.invoke("No se encontró el encoder")
            }
        }, 10000)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            // Buscamos el nombre exacto que le pusiste en el firmware C++
            if (device.name == "VBT_POCKET") {
                isScanning = false
                scanner?.stopScan(this)
                onStatusMessage?.invoke("Encoder encontrado. Conectando...")
                connect(device.address)
            }
        }
    }

    // --- CONEXIÓN GATT ---
    private fun connect(deviceAddress: String) {
        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        }
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        onStatusMessage?.invoke("Desconectado")
    }

    fun startStreaming() { sendCommand("R") }
    fun stopStreaming() { sendCommand("S") }

    private fun sendCommand(cmd: String) {
        rxCharacteristic?.let {
            it.value = cmd.toByteArray()
            it.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            bluetoothGatt?.writeCharacteristic(it)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                onStatusMessage?.invoke("Configurando MTU...")
                Handler(Looper.getMainLooper()).postDelayed({ gatt.requestMtu(512) }, 500)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                onConnectionStateChange?.invoke(false)
                onStatusMessage?.invoke("Desconectado")
                gatt.close()
                bluetoothGatt = null
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(SERVICE_UUID) ?: return
            rxCharacteristic = service.getCharacteristic(RX_CHAR_UUID)
            val txCharacteristic = service.getCharacteristic(TX_CHAR_UUID) ?: return

            gatt.setCharacteristicNotification(txCharacteristic, true)
            val descriptor = txCharacteristic.getDescriptor(CCCD_UUID)
            descriptor?.let {
                it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(it)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onConnectionStateChange?.invoke(true)
                onStatusMessage?.invoke("¡Listo para entrenar!")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == TX_CHAR_UUID) {
                val frames = parseBinaryData(characteristic.value)
                if (frames.isNotEmpty()) onDataReceived?.invoke(frames)
            }
        }
    }

    private fun parseBinaryData(data: ByteArray): List<IMUFrame> {
        val frames = mutableListOf<IMUFrame>()
        if (data.size < 32) return frames
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        while (buffer.remaining() >= 32) {
            frames.add(IMUFrame(
                ax = buffer.float, ay = buffer.float, az = buffer.float,
                gx = buffer.float, gy = buffer.float, gz = buffer.float,
                mag = buffer.float, timestamp = buffer.int.toLong() and 0xFFFFFFFFL
            ))
        }
        return frames
    }
}