#include <ArduinoBLE.h>
#include "Sensor_Class.h"

Sensor& sensor = Sensor::get();

BLEService envService("181A");
BLECharacteristic tempCharacteristic("2A6E", BLERead|BLENotify, 2);
BLECharacteristic humidCharacteristic("2A6F", BLERead|BLENotify, 2);
BLECharacteristic alertCharacteristic("2A46", BLEWrite|BLEWriteWithoutResponse, 20);

void setup() {

  if(!BLE.begin()){
    while(1);
  }

  BLE.setLocalName("Env sensor arduino");
  alertCharacteristic.setEventHandler(BLEWritten, [](BLEDevice central, BLECharacteristic characteristic){
    tempCharacteristic.writeValue((short)sensor.getTemperature());
    humidCharacteristic.writeValue((short)sensor.getHumidity());
  });
  envService.addCharacteristic(tempCharacteristic);
  envService.addCharacteristic(humidCharacteristic);
  envService.addCharacteristic(alertCharacteristic);
  sensor.performMeasurement();
  tempCharacteristic.writeValue((short)sensor.getTemperature());
  humidCharacteristic.writeValue((short)sensor.getHumidity());
  BLE.addService(envService);
  BLE.setAdvertisedService(envService);
  BLE.advertise();
}

unsigned long last_updated = millis();

void loop() {
  BLE.poll();

  if(BLE.connected()){
    if(tempCharacteristic.subscribed() && millis() - last_updated > 6000){
      sensor.performMeasurement();
      tempCharacteristic.writeValue((short)sensor.getTemperature());
      humidCharacteristic.writeValue((short)sensor.getHumidity());
      last_updated = millis();
    }
  }
}