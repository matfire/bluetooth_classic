import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'bluetooth_classic_platform_interface.dart';
import 'models/device.dart';

/// An implementation of [BluetoothClassicPlatform] that uses method channels.
class MethodChannelBluetoothClassic extends BluetoothClassicPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel =
      const MethodChannel('com.matteogassend/bluetooth_classic');

  /// The event channel used to receive discovered devices events
  final deviceDiscoveryChannel =
      const EventChannel("com.matteogassend/bluetooth_classic/devices");
  final deviceStatusChannel =
      const EventChannel("com.matteogassend/bluetooth_classic/status");
  final deviceDataChannel =
      const EventChannel("com.matteogassend/bluetooth_classic/read");

  /// stream mapped to deviceDiscoveryChannel
  Stream<dynamic>? _deviceDiscoveryStream;

  /// stream mapped to deviceStatusChannel
  Stream<dynamic>? _deviceStatusStream;

  /// stream mapped to deviceDataChannel
  Stream<dynamic>? _deviceDataReceivedStream;

  /// user facing stream controller for device discovery
  final StreamController<Device> discoveryStream = StreamController();

  final StreamController<int> statusStream = StreamController();

  final StreamController<Uint8List> dataReceivedStream = StreamController();

  void _onDeviceDiscovered(Device device) {
    discoveryStream.add(device);
  }

  void _onDeviceStatus(int status) {
    statusStream.add(status);
  }

  void _onDeviceDataReceived(Uint8List data) {
    dataReceivedStream.add(data);
  }

  @override
  Future<String?> getPlatformVersion() async {
    final version =
        await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<bool> initPermissions() async {
    var res = await methodChannel.invokeMethod<bool>("initPermissions");
    return res!;
  }

  @override
  Future<List<Device>> getPairedDevices() async {
    var res = await methodChannel.invokeListMethod("getDevices");
    return res!.map((e) => Device.fromMap(e)).toList();
  }

  @override
  Future<bool> startScan() async {
    var res = await methodChannel.invokeMethod<bool>("startDiscovery");
    _deviceDiscoveryStream = deviceDiscoveryChannel.receiveBroadcastStream();
    _deviceDiscoveryStream!.listen((event) {
      _onDeviceDiscovered(Device.fromMap(event));
    });
    return res!;
  }

  @override
  Future<bool> stopScan() async {
    var res = await methodChannel.invokeMethod<bool>("stopDiscovery");
    if (_deviceDiscoveryStream != null) {
      _deviceDiscoveryStream = null;
    }
    return res!;
  }

  @override
  Stream<Device> onDeviceDiscovered() {
    return discoveryStream.stream;
  }

  @override
  Future<bool> connect(String address, String serviceUUID) async {
    var res =
        await methodChannel.invokeMethod<bool>("connect", <String, String>{
      "deviceId": address,
      "serviceUUID": serviceUUID,
    });
    return res!;
  }

  @override
  Future<bool> disconnect() async {
    var res = await methodChannel.invokeMethod<bool>("disconnect");
    return res!;
  }

  @override
  Stream<int> onDeviceStatusChanged() {
    _deviceStatusStream = deviceStatusChannel.receiveBroadcastStream();
    _deviceStatusStream!.listen((event) {
      _onDeviceStatus(event);
    });
    return statusStream.stream;
  }

  @override
  Stream<Uint8List> onDeviceDataReceived() {
    _deviceDataReceivedStream = deviceDataChannel.receiveBroadcastStream();
    _deviceDataReceivedStream!.listen((event) {
      _onDeviceDataReceived(event);
    });
    return dataReceivedStream.stream;
  }

  @override
  Future<bool> write(String message) async {
    var res = await methodChannel
        .invokeMethod<bool>("write", <String, String>{"message": message});
    return res!;
  }
}
