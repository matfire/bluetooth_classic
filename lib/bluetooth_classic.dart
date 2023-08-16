import 'dart:typed_data';

import 'bluetooth_classic_platform_interface.dart';
import 'models/device.dart';

class BluetoothClassic {
  Future<String?> getPlatformVersion() {
    return BluetoothClassicPlatform.instance.getPlatformVersion();
  }

  Future<List<Device>> getPairedDevices() {
    return BluetoothClassicPlatform.instance.getPairedDevices();
  }

  Future<bool> initPermissions() {
    return BluetoothClassicPlatform.instance.initPermissions();
  }

  Future<bool> startScan() {
    return BluetoothClassicPlatform.instance.startScan();
  }

  Future<bool> stopScan() {
    return BluetoothClassicPlatform.instance.stopScan();
  }

  Future<bool> disconnect() {
    return BluetoothClassicPlatform.instance.disconnect();
  }

  Stream<Device> onDeviceDiscovered() {
    return BluetoothClassicPlatform.instance.onDeviceDiscovered();
  }

  Stream<int> onDeviceStatusChanged() {
    return BluetoothClassicPlatform.instance.onDeviceStatusChanged();
  }

  Stream<Uint8List> onDeviceDataReceived() {
    return BluetoothClassicPlatform.instance.onDeviceDataReceived();
  }

  Future<bool> connect(String address, String serviceUUID) {
    return BluetoothClassicPlatform.instance.connect(address, serviceUUID);
  }

  Future<bool> write(String message) {
    return BluetoothClassicPlatform.instance.write(message);
  }
}
