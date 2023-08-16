package com.matteogassend.bluetooth_classic

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.Log

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import java.io.IOException
import java.util.UUID

/** BluetoothClassicPlugin */
class BluetoothClassicPlugin: FlutterPlugin, MethodCallHandler, PluginRegistry.RequestPermissionsResultListener, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private lateinit var bluetoothDeviceChannel: EventChannel
  private var bluetoothDeviceChannelSink: EventSink ?= null
  private lateinit var bluetoothReadChannel: EventChannel
  private var bluetoothReadChannelSink : EventSink ?= null
  private lateinit var bluetoothStatusChannel: EventChannel
  private var bluetoothStatusChannelSink: EventSink ?= null
  private lateinit var ba: BluetoothAdapter
  private lateinit var pluginActivity: Activity
  private lateinit var application: Context
  private lateinit var looper: Looper
  private val myPermissionCode = 34264
  private var activeResult: Result? = null
  private var permissionGranted: Boolean = false
  private var thread: ConnectedThread ?= null
  private var socket: BluetoothSocket ?= null
  private var device: BluetoothDevice ?= null


  private inner class ConnectedThread(socket: BluetoothSocket): Thread() {

    private val inputStream = socket.inputStream
    private val outputStream = socket.outputStream
    private val buffer: ByteArray = ByteArray(1024)
    var readStream = true
    override fun run() {
      var numBytes: Int
      while (readStream) {
        try {
          numBytes = inputStream.read(buffer)
          android.util.Log.i("Bluetooth Read", "read $buffer")
          Handler(Looper.getMainLooper()).post { publishBluetoothData(ByteArray(numBytes) { buffer[it] }) }
        } catch (e: IOException) {
          android.util.Log.e("Bluetooth Read", "input stream disconnected", e)
          Handler(looper).post { publishBluetoothStatus(0) }
          readStream = false
        }
      }
    }

    fun write(bytes: ByteArray) {
      try {
        outputStream.write(bytes)
      } catch (e: IOException) {
        readStream = false
        android.util.Log.e("Bluetooth Write", "could not send data to other device", e)
        Handler(looper).post { publishBluetoothStatus(0) }
      }
    }
  }

  override fun onDetachedFromActivity() {
    TODO("Not yet implemented")
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    TODO("Not yet implemented")
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    pluginActivity = binding.activity
  }

  override fun onDetachedFromActivityForConfigChanges() {
    TODO("Not yet implemented")
  }

  private val receiver = object: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
      when(intent.action) {
        BluetoothDevice.ACTION_FOUND -> {
          val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
          publishBluetoothDevice(device)
        }
      }
    }
  }

  private fun publishBluetoothData(data: ByteArray) {
    bluetoothReadChannelSink?.success(data)
  }
  private fun publishBluetoothStatus(status: Int) {
    android.util.Log.i("Bluetooth Device Status", "Status updated to $status")
    bluetoothStatusChannelSink?.success(status)
  }

  private fun publishBluetoothDevice(device: BluetoothDevice) {
    Log.i("device_discovery", device.address)
    bluetoothDeviceChannelSink?.success(hashMapOf("address" to device.address, "name" to device.name))
  }

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.matteogassend/bluetooth_classic")
    bluetoothDeviceChannel = EventChannel(flutterPluginBinding.binaryMessenger, "com.matteogassend/bluetooth_classic/devices")
    bluetoothReadChannel = EventChannel(flutterPluginBinding.binaryMessenger, "com.matteogassend/bluetooth_classic/read")
    bluetoothStatusChannel = EventChannel(flutterPluginBinding.binaryMessenger, "com.matteogassend/bluetooth_classic/status")
    ba = BluetoothAdapter.getDefaultAdapter()
    channel.setMethodCallHandler(this)
    looper = flutterPluginBinding.applicationContext.mainLooper
    application = flutterPluginBinding.applicationContext
    bluetoothDeviceChannel.setStreamHandler(object: StreamHandler {
      override fun onListen(arguments: Any?, events: EventSink?) {
          bluetoothDeviceChannelSink = events
      }

      override fun onCancel(arguments: Any?) {
        bluetoothDeviceChannelSink = null
      }
    })
    bluetoothReadChannel.setStreamHandler(object: StreamHandler {
      override fun onCancel(arguments: Any?) {
        bluetoothReadChannelSink = null
      }

      override fun onListen(arguments: Any?, events: EventSink?) {
        bluetoothReadChannelSink = events
      }
    })
    bluetoothStatusChannel.setStreamHandler(object: StreamHandler {
      override fun onCancel(arguments: Any?) {
        bluetoothStatusChannelSink = null
      }

      override fun onListen(arguments: Any?, events: EventSink?) {
        bluetoothStatusChannelSink = events
      }
    })
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    Log.i("method_call", call.method)
    when(call.method) {
      "getPlatformVersion" -> result.success("Android ${Build.VERSION.RELEASE}")
      "initPermissions" -> initPermissions(result)
      "getDevices" -> getDevices(result)
      "startDiscovery" -> startScan(result)
      "stopDiscovery" -> stopScan(result)
      "connect" -> connect(result, call.argument<String>("deviceId")!!,
        call.argument<String>("serviceUUID")!!
      )
      "disconnect" -> disconnect(result)
      "write" -> write(result, call.argument<String>("message")!!)
      else -> result.notImplemented()
    }
  }

  private fun write(result: Result, message: String) {
    Log.i("write_handle", "inside write handle")
    if (thread != null) {
      thread!!.write(message.toByteArray())
      result.success(true)
    } else {
      result.error("write_impossible", "could not send message to unconnected device", null)
    }
  }

  private fun disconnect(result: Result) {
    device = null
    android.util.Log.i("Bluetooth Disconnect", "device removed from memory")
    thread!!.interrupt()
    android.util.Log.i("Bluetooth Disconnect", "read thread closed")
    thread = null
    android.util.Log.i("Bluetooth Disconnect", "read thread freed")
    socket!!.close()
    android.util.Log.i("Bluetooth Disconnect", "rfcomm socket closed")
    publishBluetoothStatus(0)
    android.util.Log.i("Bluetooth Disconnect", "disconnected")
    result.success(true)
  }

  private fun connect(result: Result, deviceId: String, serviceUuid: String) {
    try {
      publishBluetoothStatus(1)
      device = ba.getRemoteDevice(deviceId)
      android.util.Log.i("Bluetooth Connection", "device found")
      assert(device != null)
      socket = device?.createRfcommSocketToServiceRecord(UUID.fromString(serviceUuid))
      android.util.Log.i("Bluetooth Connection", "rfcommsocket found")
      assert(socket != null)
      socket?.connect()
      android.util.Log.i("Bluetooth Connection", "socket connected")
      thread = ConnectedThread(socket!!)
      android.util.Log.i("Bluetooth Connection", "thread created")
      assert(thread != null)
      thread!!.start()
      android.util.Log.i("Bluetooth Connection", "thread running")
      result.success(true)
      publishBluetoothStatus(2)

    } catch (e: IOException) {
      android.util.Log.e("Bluetooth Connection", "connection failed", e)
      publishBluetoothStatus(0)
      result.error("connection_failed", "could not connect to device $deviceId", null)
    }
  }

  private fun startScan(result: Result) {
    Log.i("start_scan", "scan started")
    application.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    ba.startDiscovery()
    result.success(true)
  }

  private  fun stopScan(result: Result) {
    Log.i("stop_scan", "scan stopped")
    ba.cancelDiscovery()
    result.success(true)
  }

  private fun initPermissions(result: Result) {
    if (activeResult != null) {
      result.error("init_running", "only one initialize call allowed at a time", null)
    }
    activeResult = result
    checkPermissions(application)
  }

  private fun arePermissionsGranted(application: Context) {
    val permissions = mutableListOf(
      Manifest.permission.BLUETOOTH,
      Manifest.permission.BLUETOOTH_ADMIN,
      Manifest.permission.ACCESS_FINE_LOCATION,
      Manifest.permission.ACCESS_COARSE_LOCATION
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
      permissions.add(Manifest.permission.BLUETOOTH_SCAN)
    }

    for (perm in permissions) {
      permissionGranted = ContextCompat.checkSelfPermission(application, perm) == PackageManager.PERMISSION_GRANTED
    }
  }

  private fun checkPermissions(application: Context) {
    arePermissionsGranted(application)
    if (!permissionGranted) {
      Log.i("permission_check", "permissions not granted, asking")
      val permissions = mutableListOf(
          Manifest.permission.BLUETOOTH,
          Manifest.permission.BLUETOOTH_ADMIN,
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION
      )
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        permissions.add(Manifest.permission.BLUETOOTH_SCAN)
      }
      ActivityCompat.requestPermissions(pluginActivity, permissions.toTypedArray(), myPermissionCode)
    } else {
      Log.i("permission_check", "permissions granted, continuing")
      completeCheckPermissions()
    }
  }

  private fun completeCheckPermissions() {
    if ( permissionGranted ) {
      // Do some other work if needed
      activeResult?.success(true)
    } else {
      activeResult?.error("permissions_not_granted", "if permissions are not granted, you will not be able to use this plugin", null)
    }
    // Conveniently plugin invocations are all asynchronous
    activeResult = null
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ): Boolean {
    when (requestCode) {
      myPermissionCode -> {
        permissionGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        completeCheckPermissions()
        return true
      }
    }
    return false
  }

  @SuppressLint("MissingPermission")
  fun getDevices(result: Result) {
    val devices = ba.bondedDevices
    val list = mutableListOf<HashMap<String, String>>()

    for (data in devices) {
      val hash = HashMap<String, String>()
      hash["address"] = data.address
      hash["name"] = data.name
      list.add(hash)
    }
    result.success(list.toList())
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}
