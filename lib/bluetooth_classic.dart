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

  Stream<Device> onDeviceDiscovered() {
    return BluetoothClassicPlatform.instance.onDeviceDiscovered();
  }

  Future<bool> connect(String address, String serviceUUID) {
    return BluetoothClassicPlatform.instance.connect(address, serviceUUID);
  }
}
