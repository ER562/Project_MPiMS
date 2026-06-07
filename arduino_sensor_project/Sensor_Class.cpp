#include "Sensor_Class.h"

Sensor::Sensor(){};
Sensor::~Sensor(){};

Sensor& Sensor::get(){
  static Sensor instance;
  return instance;
}

void Sensor::performMeasurement(){
  int chk = DHT11.read(DHT11PIN);
  if(chk != DHTLIB_OK){
    return;
  }
  temperature = (float)DHT11.temperature;
  humidity = (float)DHT11.humidity;
}

int Sensor::getTemperature(){
  return temperature;
}

int Sensor::getHumidity(){
  return humidity;
}