# bluetooth_classic

## A Flutter plugin to connect to Bluetooth Classic devices, mainly designed to work with serial communication.

![Pub Version (including pre-releases)](https://img.shields.io/pub/v/bluetooth_classic)
![Pub Popularity](https://img.shields.io/pub/popularity/bluetooth_classic)

## Features

- check that permissions are granted to use the device
- get already paired devices
- scan for devices
- connect to a device
- read from connected device
- write to connected device

## Installation

To install the package, simply run `flutter pub add bluetooth_classic`

## Api Documentation

For a list of functions and their definitions, you can look at the documentation on [pub.dev](https://pub.dev/packages/bluetooth_classic)

## Basic example

### Getting Paired Devices

```dart
import 'package:bluetooth_classic/bluetooth_classic.dart';

final _bluetoothClassicPlugin = BluetoothClassic();
await _bluetoothClassicPlugin.initPermissions()
List<Device> _discoveredDevices = await _bluetoothClassicPlugin.getPairedDevices();
```

### Scanning for Devices

```dart
import 'package:bluetooth_classic/bluetooth_classic.dart';
import 'package:bluetooth_classic/models/device.dart';

final _bluetoothClassicPlugin = BluetoothClassic();
List<Device> _discoveredDevices = [];
_bluetoothClassicPlugin.onDeviceDiscovered().listen(
  (event) {
    _discoveredDevices = [..._discoveredDevices, event];
  },
);
await _bluetoothClassicPlugin.startScan();
// when you want to stop scanning
await _bluetoothClassicPlugin.stopScan();

```


## Example Code

You can find the latest example code in the [example folder](/example/lib/main.dart)
