# bluetooth_classic

## A Flutter plugin to connect to Bluetooth Classic devices, mainly designed to work with serial communication.

![Pub Version (including pre-releases)](https://img.shields.io/pub/v/bluetooth_classic)
![Pub Popularity](https://img.shields.io/pub/popularity/bluetooth_classic)

## Features

- check that permissions are granted to use the device
- get already paired devices
- scan for devices
- connect & disconnect to a device
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
await _bluetoothClassicPlugin.initPermissions();
List<Device> _discoveredDevices = await _bluetoothClassicPlugin.getPairedDevices();
```

### Scanning for Devices

```dart
import 'package:bluetooth_classic/bluetooth_classic.dart';
import 'package:bluetooth_classic/models/device.dart';

final _bluetoothClassicPlugin = BluetoothClassic();
List<Device> _discoveredDevices = [];
await _bluetoothClassicPlugin.initPermissions();
_bluetoothClassicPlugin.onDeviceDiscovered().listen(
  (event) {
    _discoveredDevices = [..._discoveredDevices, event];
  },
);
await _bluetoothClassicPlugin.startScan();
// when you want to stop scanning
await _bluetoothClassicPlugin.stopScan();

```

### Connecting to a device, reading and sending data to it

```dart
import 'package:bluetooth_classic/bluetooth_classic.dart';

final _bluetoothClassicPlugin = BluetoothClassic();
Uint8List _data = Uint8List(0);

await _bluetoothClassicPlugin.initPermissions();
// connect to a device with its MAC address and the application uuid you want to use (in this example, serial)
await _bluetoothClassicPlugin.connect("AA:AA:AA:AA", "00001101-0000-1000-8000-00805f9b34fb");

// NB: I'm not doing error checking or seeing if the device is still connected in this code, check the [example folder](/example/lib/main.dart) for a more thorough demonstration
// send data to device
await _bluetoothClassicPlugin.write("ping");

//
_bluetoothClassicPlugin.onDeviceDataReceived().listen((event) {
  setState(() {
    _data = Uint8List.fromList([..._data, ...event]);
  });
});

// disconnect when done
await _bluetoothClassicPlugin.disconnect();

```

## Example Code

You can find the latest example code in the [example folder](/example/lib/main.dart)
