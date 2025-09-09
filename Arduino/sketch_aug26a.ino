#include <OneWire.h>
#include <DallasTemperature.h>
#include "DHT.h"

// Pins
#define DS18B20_PIN 5     // DS18B20 connected to GPIO5
#define DHT_PIN 4         // DHT22 connected to GPIO4
#define DHT_TYPE DHT22
#define REED_PIN 14       // Reed switch connected to GPIO14 (Normally Closed)

OneWire oneWire(DS18B20_PIN);
DallasTemperature sensors(&oneWire);
DHT dht(DHT_PIN, DHT_TYPE);

void setup() {
  Serial.begin(115200);
  delay(2000);

  // Initialize DS18B20 sensor
  sensors.begin();
  int deviceCount = sensors.getDeviceCount();
  Serial.print("Found DS18B20 devices: ");
  Serial.println(deviceCount);
  if (deviceCount == 0) {
    Serial.println("⚠️ DS18B20 not detected! Check wiring.");
  }

  // Initialize DHT22 sensor
  dht.begin();
  float initialTemp = dht.readTemperature();
  if (isnan(initialTemp)) {
    Serial.println("⚠️ Failed to initialize DHT22 sensor!");
  } else {
    Serial.println("DHT22 initialized successfully.");
  }

  // Initialize reed switch pin (Normally Closed logic)
  pinMode(REED_PIN, INPUT_PULLUP);
}

void loop() {
  // Read DS18B20 temperature
  sensors.requestTemperatures();
  float tempDS18B20 = sensors.getTempCByIndex(0);

  if (tempDS18B20 == DEVICE_DISCONNECTED_C) {
    Serial.println("⚠️ DS18B20 disconnected or not responding.");
  } else {
    Serial.print("DS18B20 Temperature (Inside): ");
    Serial.print(tempDS18B20, 1);
    Serial.println(" °C");
  }

  // Read DHT22 temperature and humidity
  float tempDHT22 = dht.readTemperature();
  float humidity = dht.readHumidity();

  if (isnan(tempDHT22) || isnan(humidity)) {
    Serial.println("⚠️ Failed to read from DHT22 sensor! Retrying...");
  } else {
    Serial.print("DHT22 Temperature (Outside): ");
    Serial.print(tempDHT22, 1);
    Serial.print(" °C, Humidity: ");
    Serial.print(humidity, 1);
    Serial.println(" %");
  }

  // Read reed switch for door status (Normally Closed logic)
  int reedState = digitalRead(REED_PIN);
  if (reedState ==LOW) {
  Serial.println("🚪 Door is CLOSED.");
} else {
  Serial.println("🚪 Door is OPEN.");
}

  delay(2000);
}

