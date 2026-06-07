#include <dht11.h>
#define DHT11PIN 4    //przypisanie pinu 2 Arduino jako odczyt z sensora

class Sensor{
private:
  dht11 DHT11;
  float temperature;
  float humidity;

  Sensor();
  ~Sensor();
public:
  Sensor(const Sensor&) = delete;
  Sensor& operator=(const Sensor&) = delete;

  static Sensor& get();

  void performMeasurement();

  int getTemperature();

  int getHumidity();
};