#include <WiFi.h>
#include <FirebaseESP32.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include "DHT.h"

const char* WIFI_SSID_1 = "UTMNet"; 
const char* WIFI_PASSWORD_1 = "Hopy0506290558@"; 
const char* WIFI_SSID_2 = "58F2A3-Maxis Fibre Internet"; 
const char* WIFI_PASSWORD_2 = "18A3358392";

#define API_KEY "AIzaSyAuJjF7dME30MFvqMLxfgXZeG19rGnMFak"
#define DATABASE_URL "https://pbcms-c955a-default-rtdb.firebaseio.com/"
#define USER_EMAIL "device@iot.com"
#define USER_PASSWORD "device1234"

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// Pins
#define DS18B20_PIN 5     // DS18B20 connected to GPIO5
#define DHT_PIN 4         // DHT22 connected to GPIO4
#define DHT_TYPE DHT22
#define REED_PIN 14

OneWire oneWire(DS18B20_PIN);
DallasTemperature sensors(&oneWire);
DHT dht(DHT_PIN, DHT_TYPE);

void connectWiFi() {
  Serial.println("Connecting to Wi-Fi...");
  WiFi.disconnect(true);
  delay(100); 
  WiFi.begin(WIFI_SSID_1, WIFI_PASSWORD_1);
  unsigned long startAttemptTime = millis();

  // Try WiFi 1 for 10 seconds
  while (WiFi.status() != WL_CONNECTED && millis() - startAttemptTime < 10000) {
    Serial.print(".");
    delay(500);
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.printf("\nConnected to Wi-Fi: %s\n", WIFI_SSID_1);
    return;
  }

  // Disconnect before retrying
  WiFi.disconnect(true);
  delay(100);

  WiFi.begin(WIFI_SSID_2, WIFI_PASSWORD_2);
  startAttemptTime = millis();

  while (WiFi.status() != WL_CONNECTED && millis() - startAttemptTime < 10000) {
    Serial.print(".");
    delay(500);
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.printf("\nConnected to Wi-Fi: %s\n", WIFI_SSID_2);
  } else {
    Serial.println("\n❌ Failed to connect to any WiFi");
  }
}
void setup() {
  Serial.begin(115200);
  connectWiFi();

  // Firebase configuration
  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;
  auth.user.email = USER_EMAIL;
  auth.user.password = USER_PASSWORD;

  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  // Sensor setup
  sensors.begin();
  dht.begin();
  pinMode(REED_PIN, INPUT_PULLUP);
}

void loop() {
  // Reconnect Wi-Fi if disconnected 
  if (WiFi.status() != WL_CONNECTED) { 
    Serial.println("Wi-Fi lost. Attempting to reconnect..."); 
    connectWiFi(); 
  }

  // Read DS18B20 temperature
  sensors.requestTemperatures();
  float tempDS18B20 = sensors.getTempCByIndex(0);

  // Read DHT22 temperature & humidity
  float tempDHT22 = dht.readTemperature();
  float humidity = dht.readHumidity();

  // Door status from reed switch
  String doorStatus = (digitalRead(REED_PIN) == LOW) ? "CLOSED" : "OPEN";

  String wifiStatus = (WiFi.status() == WL_CONNECTED) ? "connected" : "disconnected";

  // Print to Serial Monitor
  Serial.printf("🌡 Inside Temp: %.2f °C | Outside Temp: %.2f °C | Humidity: %.2f %% | Door: %s\n",
                tempDS18B20, tempDHT22, humidity, doorStatus.c_str());

  // --------- Send to Firebase --------- 
  if (Firebase.ready()) { 
    if (!Firebase.setFloat(fbdo, "/sensors/temperature", tempDS18B20)) { 
      Serial.println("❌ Failed to send temperature:"); 
      Serial.println(fbdo.errorReason()); 
    } 
    if (!Firebase.setFloat(fbdo, "/sensors/humidity", humidity)) { 
      Serial.println("❌ Failed to send humidity:"); 
      Serial.println(fbdo.errorReason()); 
    } 
    if (!Firebase.setString(fbdo, "/sensors/door_status", doorStatus)) { 
      Serial.println("❌ Failed to send door status:"); 
      Serial.println(fbdo.errorReason()); 
    } 
    if (!Firebase.setString(fbdo, "/wifiStatus", wifiStatus)) {
      Serial.println("❌ Failed to send WiFi status:");
      Serial.println(fbdo.errorReason());
    }
  }

  delay(2000);
}