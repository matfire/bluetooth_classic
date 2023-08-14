class Device {
  String address;
  String? name;

  static const int disconnected = 0;
  static const int connecting = 1;
  static const int connected = 2;

  Device({required this.address, this.name});

  factory Device.fromMap(Map<dynamic, dynamic> map) {
    return Device(address: map["address"], name: map["name"]);
  }
}
